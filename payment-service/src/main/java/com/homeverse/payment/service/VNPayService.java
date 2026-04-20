package com.homeverse.payment.service;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

public interface VNPayService {
    String createPaymentUrl(long amount, String orderInfo, HttpServletRequest request);
    int orderReturn(HttpServletRequest request);
    boolean verifySignature(Map<String, String> fields);
}