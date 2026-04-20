package com.homeverse.payment.service;

public interface ServicePackageService {
    void buyMembership(Long userId, Long packageId);
    // Sau này có thể thêm: void buyPushTicket(Long userId, Long packageId);
    void buyPromotion(Long userId, Long packageId, Long propertyId);
}