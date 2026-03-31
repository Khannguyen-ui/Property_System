package com.homeverse.customer.controller;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.customer.dto.request.*;
import com.homeverse.customer.dto.response.CustomerPublicResponseDTO;
import com.homeverse.customer.dto.response.CustomerResponseDTO;
import com.homeverse.customer.dto.response.KycOcrResponseDTO;
import com.homeverse.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // API nội bộ: Identity-Service sẽ gọi API này
    @PostMapping("/init")
    public ApiResponse<String> initProfile(@RequestBody CustomerInitDTO dto) {
        customerService.initCustomerProfile(dto);
        return ApiResponse.<String>builder().result("Đã khởi tạo hồ sơ rỗng").build();
    }
    @PutMapping("/{id}/email")
    public ApiResponse<String> updateEmail(@PathVariable("id") Long id, @RequestParam("newEmail") String newEmail) {
        customerService.updateEmailCustomer(id, newEmail);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đồng bộ email thành công")
                .result("OK")
                .build();
    }

    @GetMapping("/profile")
    public ApiResponse<CustomerResponseDTO> getMyProfile() {
        return ApiResponse.<CustomerResponseDTO>builder()
                .result(customerService.getMyProfile())
                .build();
    }

    @GetMapping("/{slug}/public-profile")
    public ApiResponse<CustomerPublicResponseDTO> getPublicProfile(@PathVariable("slug") String slug) {
        return ApiResponse.<CustomerPublicResponseDTO>builder()
                .result(customerService.getPublicProfile(slug))
                .build();
    }

    @PutMapping("/profile")
    public ApiResponse<CustomerResponseDTO> updateProfile(@RequestBody CustomerProfileDTO dto) {
        return ApiResponse.<CustomerResponseDTO>builder()
                .result(customerService.updateProfile(dto))
                .build();
    }


    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerResponseDTO> uploadAvatar(@RequestPart("file") MultipartFile file) {
        CustomerResponseDTO response = customerService.uploadAvatar(file);
        return ApiResponse.<CustomerResponseDTO>builder()
                .result(response)
                .build();
    }

    @PostMapping(value = "/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerResponseDTO> uploadBanner(@RequestPart("file") MultipartFile file) {
        return ApiResponse.<CustomerResponseDTO>builder()
                .result(customerService.uploadBanner(file))
                .build();
    }
    @PostMapping(value = "/kyc/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KycOcrResponseDTO> scanCitizenId(@RequestPart("image") MultipartFile image) {
        return ApiResponse.<KycOcrResponseDTO>builder()
                .result(customerService.scanCitizenId(image))
                .build();
    }

    @PostMapping(value = "/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> submitKyc(
            @RequestParam("kycToken") String kycToken,
            @RequestParam("citizenId") String citizenId,
            @RequestParam("fullName") String fullName,
            @RequestParam("address") String address,
            @RequestPart("frontImage") MultipartFile frontImage,
            @RequestPart("backImage") MultipartFile backImage) {

        customerService.submitKyc(kycToken, citizenId, fullName, address, frontImage, backImage);

        return ApiResponse.<String>builder()
                .result("Hồ sơ KYC đã được gửi thành công! Vui lòng chờ quản trị viên phê duyệt.")
                .build();
    }
}