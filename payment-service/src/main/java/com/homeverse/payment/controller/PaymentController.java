package com.homeverse.payment.controller;

import com.homeverse.payment.kafka.PaymentProducer;
import com.homeverse.common.dto.PaymentEvent;
import com.homeverse.payment.entity.Transaction;
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
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(
            @RequestParam long amount,
            @RequestParam Long userId,
            HttpServletRequest request) {

        String orderInfo = "NAP_TIEN_USER_" + userId;
        String paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, request);
        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }

    @GetMapping("/vnpay-return")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("🚀🚀🚀 ĐÃ VÀO ĐƯỢC HÀM RETURN! Query: {}", request.getQueryString());
        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String transactionId = request.getParameter("vnp_TransactionNo");
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

                response.sendRedirect(frontendUrl + "/payment-success?status=success&amount=" 
                        + amount.toPlainString() + "&txnRef=" + transactionId);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        response.sendRedirect(frontendUrl + "/payment-failed");
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
}