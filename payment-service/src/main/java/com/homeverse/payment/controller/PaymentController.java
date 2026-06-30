package com.homeverse.payment.controller;

import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.payment.config.VNPayConfig;
import com.homeverse.payment.entity.Transaction;
import com.homeverse.payment.kafka.PaymentProducer;
import com.homeverse.payment.repository.TransactionRepository;
import com.homeverse.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final String MOBILE_SUCCESS_DEFAULT = "homeswipe://wallet/success";
    private static final String MOBILE_FAILED_DEFAULT = "homeswipe://wallet/failed";

    private final VNPayService vnPayService;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(
            @RequestParam long amount,
            @RequestParam Long userId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request) {

        String orderInfo = "NAP_TIEN_USER_" + userId;
        String txnRef = VNPayConfig.getRandomNumber(8);

        boolean mobileFlow = isMobileFlow(platform, returnUrl);

        if (mobileFlow) {
            String successUrl = resolveSuccessRedirectUrl(platform, returnUrl);
            String failedUrl = resolveFailedRedirectUrl(successUrl);
            vnPayService.storeRedirectTarget(txnRef, successUrl, failedUrl);
        }

        String paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, txnRef, request);
        return ResponseEntity.ok(Map.of(
                "url", paymentUrl,
                "txnRef", txnRef
        ));
    }

    @GetMapping("/vnpay-return")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("VNPay return query: {}", request.getQueryString());

        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String txnRef = request.getParameter("vnp_TxnRef");
        String totalPrice = request.getParameter("vnp_Amount");

        if (paymentStatus == 1) {
            try {
                String[] parts = orderInfo.split("_");
                Long userId = Long.parseLong(parts[parts.length - 1]);
                BigDecimal amount = new BigDecimal(totalPrice).divide(new BigDecimal(100));

                if (!transactionRepository.existsByVnpayCode(transactionId)) {
                    transactionRepository.save(Transaction.builder()
                            .userId(userId)
                            .amount(amount)
                            .type("DEPOSIT")
                            .status("SUCCESS")
                            .vnpayCode(transactionId)
                            .createdAt(LocalDateTime.now())
                            .build());

                    paymentProducer.sendPaymentSuccess(PaymentEvent.builder()
                            .userId(userId)
                            .amount(amount)
                            .transactionId(transactionId)
                            .status("SUCCESS")
                            .type("DEPOSIT")
                            .build());
                }

                String target = vnPayService.consumeRedirectUrl(txnRef, true);
                if (target == null || target.isBlank()) {
                    target = frontendUrl + "/payment-success";
                }

                response.sendRedirect(appendQueryParams(target, Map.of(
                        "status", "success",
                        "amount", amount.toPlainString(),
                        "txnRef", txnRef != null ? txnRef : transactionId,
                        "transactionId", transactionId
                )));
                return;
            } catch (Exception e) {
                log.error("VNPay return success flow error", e);
            }
        }

        String target = vnPayService.consumeRedirectUrl(txnRef, false);
        if (target == null || target.isBlank()) {
            target = frontendUrl + "/payment-failed";
        }

        response.sendRedirect(appendQueryParams(target, Map.of(
                "status", "failed",
                "txnRef", txnRef != null ? txnRef : "",
                "message", "payment_failed"
        )));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> processVNPayIPN(@RequestParam Map<String, String> params) {
        try {
            boolean isValid = vnPayService.verifySignature(params);
            if (!isValid) {
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
            }

            String transactionId = params.get("vnp_TransactionNo");
            String responseCode = params.get("vnp_ResponseCode");
            String orderInfo = params.get("vnp_OrderInfo");
            BigDecimal amount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));

            String[] parts = orderInfo.split("_");
            Long userId = Long.parseLong(parts[parts.length - 1]);

            if ("00".equals(responseCode)) {
                if (!transactionRepository.existsByVnpayCode(transactionId)) {
                    transactionRepository.save(Transaction.builder()
                            .userId(userId)
                            .amount(amount)
                            .type("DEPOSIT")
                            .status("SUCCESS")
                            .vnpayCode(transactionId)
                            .createdAt(LocalDateTime.now())
                            .build());

                    paymentProducer.sendPaymentSuccess(PaymentEvent.builder()
                            .userId(userId)
                            .amount(amount)
                            .transactionId(transactionId)
                            .status("SUCCESS")
                            .type("DEPOSIT")
                            .build());
                }
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
            } else {
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success (Payment Failed)"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }

    private boolean isMobileFlow(String platform, String returnUrl) {
        if ("mobile".equalsIgnoreCase(platform)) {
            return true;
        }
        return returnUrl != null && returnUrl.startsWith("homeswipe://");
    }

    private String resolveSuccessRedirectUrl(String platform, String returnUrl) {
        if (returnUrl != null && !returnUrl.isBlank()) {
            validateRedirectUrl(returnUrl);
            return returnUrl;
        }

        if ("mobile".equalsIgnoreCase(platform)) {
            return MOBILE_SUCCESS_DEFAULT;
        }

        return frontendUrl + "/payment-success";
    }

    private String resolveFailedRedirectUrl(String successUrl) {
        if (successUrl == null || successUrl.isBlank()) {
            return MOBILE_FAILED_DEFAULT;
        }

        if (successUrl.startsWith("homeswipe://")) {
            if (successUrl.contains("/success")) {
                return successUrl.replace("/success", "/failed");
            }
            return MOBILE_FAILED_DEFAULT;
        }

        if (successUrl.contains("/payment-success")) {
            return successUrl.replace("/payment-success", "/payment-failed");
        }

        return successUrl.contains("?")
                ? successUrl + "&status=failed"
                : successUrl + "/failed";
    }

    private void validateRedirectUrl(String url) {
        boolean allowed =
                url.startsWith("homeswipe://") ||
                        url.startsWith(frontendUrl) ||
                        url.startsWith("http://localhost:3000") ||
                        url.startsWith("http://localhost:5173");

        if (!allowed) {
            throw new IllegalArgumentException("Return URL không nằm trong whitelist");
        }
    }

    private String appendQueryParams(String baseUrl, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}