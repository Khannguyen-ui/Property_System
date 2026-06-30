package com.homeverse.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VNPayService {

    String createPaymentUrl(long amount, String orderInfo, HttpServletRequest request);

    String createPaymentUrl(long amount, String orderInfo, String txnRef, HttpServletRequest request);

    void storeRedirectTarget(String txnRef, String successUrl, String failedUrl);

    String consumeRedirectUrl(String txnRef, boolean success);

    int orderReturn(HttpServletRequest request);

    boolean verifySignature(Map<String, String> fields);
}