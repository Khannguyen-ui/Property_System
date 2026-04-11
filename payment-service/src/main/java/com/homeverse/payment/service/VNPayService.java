package com.homeverse.payment.service;

import jakarta.servlet.http.HttpServletRequest;

public interface VNPayService {
    // Tạo URL thanh toán để chuyển hướng user sang trang nhập thẻ
    String createPaymentUrl(long amount, String orderInfo, HttpServletRequest request);

    // Kiểm tra kết quả trả về từ VNPay (Check chữ ký, trạng thái)
    int orderReturn(HttpServletRequest request);
}