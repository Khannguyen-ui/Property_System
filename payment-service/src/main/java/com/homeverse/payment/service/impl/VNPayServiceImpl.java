package com.homeverse.payment.service.impl;

import com.homeverse.payment.config.VNPayConfig;
import com.homeverse.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private static final String REDIRECT_PREFIX = "vnpay:redirect:";
    private static final Duration REDIRECT_TTL = Duration.ofMinutes(30);

    private final VNPayConfig vnPayConfig;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String createPaymentUrl(long amount, String orderInfo, HttpServletRequest request) {
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        return createPaymentUrl(amount, orderInfo, vnp_TxnRef, request);
    }

    @Override
    public String createPaymentUrl(long amount, String orderInfo, String txnRef, HttpServletRequest request) {
        String vnp_TxnRef = (txnRef == null || txnRef.isBlank())
                ? VNPayConfig.getRandomNumber(8)
                : txnRef;

        String vnp_IpAddr = VNPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8);
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8);

                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnp_SecureHash = hmacSHA512(vnPayConfig.vnp_HashSecret, hashData.toString());
        return vnPayConfig.vnp_PayUrl + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    @Override
    public void storeRedirectTarget(String txnRef, String successUrl, String failedUrl) {
        if (txnRef == null || txnRef.isBlank()) {
            return;
        }

        if (successUrl != null && !successUrl.isBlank()) {
            redisTemplate.opsForValue().set(
                    REDIRECT_PREFIX + txnRef + ":success",
                    successUrl,
                    REDIRECT_TTL
            );
        }

        if (failedUrl != null && !failedUrl.isBlank()) {
            redisTemplate.opsForValue().set(
                    REDIRECT_PREFIX + txnRef + ":failed",
                    failedUrl,
                    REDIRECT_TTL
            );
        }
    }

    @Override
    public String consumeRedirectUrl(String txnRef, boolean success) {
        if (txnRef == null || txnRef.isBlank()) {
            return null;
        }

        String key = REDIRECT_PREFIX + txnRef + ":" + (success ? "success" : "failed");
        String url = redisTemplate.opsForValue().get(key);
        if (url != null) {
            redisTemplate.delete(key);
        }
        return url;
    }

    @Override
    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        if (verifySignature(fields, vnp_SecureHash)) {
            return "00".equals(request.getParameter("vnp_ResponseCode")) ? 1 : 0;
        }
        return -1;
    }

    @Override
    public boolean verifySignature(Map<String, String> fields) {
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        Map<String, String> cleanFields = new HashMap<>(fields);
        cleanFields.remove("vnp_SecureHashType");
        cleanFields.remove("vnp_SecureHash");
        return verifySignature(cleanFields, vnp_SecureHash);
    }

    private boolean verifySignature(Map<String, String> fields, String vnp_SecureHash) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();

        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) sb.append("&");
            }
        }

        return hmacSHA512(vnPayConfig.vnp_HashSecret, sb.toString()).equals(vnp_SecureHash);
    }

    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            hmac512.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}