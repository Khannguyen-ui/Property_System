package com.homeverse.customer.service;

import com.homeverse.customer.dto.request.CustomerInitDTO;
import com.homeverse.customer.dto.request.CustomerProfileDTO;
import com.homeverse.customer.dto.request.KycRequestDTO;
import com.homeverse.customer.dto.response.CustomerPublicResponseDTO;
import com.homeverse.customer.dto.response.CustomerResponseDTO;
import com.homeverse.customer.dto.response.KycOcrResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {

    void initCustomerProfile(CustomerInitDTO dto);

    void updateEmailCustomer(Long id, String newEmail);

    CustomerResponseDTO getMyProfile();

    CustomerResponseDTO updateProfile(CustomerProfileDTO dto);

    CustomerResponseDTO uploadAvatar(MultipartFile file);

    CustomerPublicResponseDTO getPublicProfile(String slug);

    KycOcrResponseDTO scanCitizenId(MultipartFile image);

    void submitKyc(String kycToken, String citizenId, String fullName, String address, MultipartFile frontImage, MultipartFile backImage);

    CustomerResponseDTO uploadBanner(MultipartFile file);
}