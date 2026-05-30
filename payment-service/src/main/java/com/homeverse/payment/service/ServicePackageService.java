package com.homeverse.payment.service;

import com.homeverse.payment.entity.ServicePackage;
import java.util.List;

public interface ServicePackageService {
    
    // ==========================================
    // 🛒 NHÓM CHO USER (MUA BÁN)
    // ==========================================
    void buyMembership(Long userId, Long packageId);
    void buyPromotion(Long userId, Long packageId, Long propertyId);

    // ==========================================
    // 🛠 NHÓM CHO ADMIN (QUẢN LÝ DANH MỤC)
    // ==========================================
    List<ServicePackage> getAllPackagesForAdmin(); // Xem cả gói ẩn/hiện
    List<ServicePackage> getAllActivePackages();   // Chỉ xem gói đang bán (cho User)
    ServicePackage createPackage(ServicePackage pkg);
    ServicePackage updatePackage(Long id, ServicePackage pkg);
    void deletePackage(Long id); // Nên dùng Soft Delete (active = false)
}