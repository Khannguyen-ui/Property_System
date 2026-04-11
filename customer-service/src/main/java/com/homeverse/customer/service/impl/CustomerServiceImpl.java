package com.homeverse.customer.service.impl;

import com.homeverse.customer.client.FptAiClient;
import com.homeverse.customer.client.MediaClient;
import com.homeverse.customer.dto.external.FptOcrResponse;
import com.homeverse.customer.dto.request.*;
import com.homeverse.customer.dto.response.CustomerPublicResponseDTO;
import com.homeverse.customer.dto.response.CustomerResponseDTO;
import com.homeverse.customer.dto.response.KycOcrResponseDTO;
import com.homeverse.customer.entity.Customer;
import com.homeverse.customer.repository.CustomerRepository;
import com.homeverse.customer.service.CustomerService;
import org.redisson.api.RBloomFilter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;
    private final MediaClient mediaClient;
    private final FptAiClient fptAiClient;
    private final StringRedisTemplate redisTemplate;
    @Value("${fpt.ai.api-key}")
    private String fptApiKey;
    private final RBloomFilter<String> citizenIdBloomFilter;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public void initCustomerProfile(CustomerInitDTO dto) {
        String generatedSlug = generateSlug(dto.getFullName());
        Customer customer = Customer.builder()
                .id(dto.getId())
                .publicId(generatedSlug)
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .kycStatus("UNVERIFIED")
                .build();
        customerRepository.save(customer);
    }

    private Customer getCurrentCustomer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Hồ sơ khách hàng không tồn tại!"));
    }

    @Override
    @Transactional
    public void updateEmailCustomer(Long id, String newEmail) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đồng bộ thất bại: Không tìm thấy ID " + id));

        String oldEmail = customer.getEmail();
        customer.setEmail(newEmail);
        customerRepository.save(customer);

    }

    @Override
    @Transactional
    public CustomerResponseDTO uploadAvatar(MultipartFile file) {
        try {
            Customer customer = getCurrentCustomer();
            String oldAvatarUrl = customer.getAvatarUrl();
            String newAvatarUrl = mediaClient.uploadImage(file, "avatars").getResult();

            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        mediaClient.deleteImage(oldAvatarUrl);
                    } catch (Exception e) {
                        System.err.println("Lỗi khi xóa ảnh rác (Chạy ngầm): " + e.getMessage());
                    }
                });
            }


            customer.setAvatarUrl(newAvatarUrl);
            Customer saved = customerRepository.save(customer);

            return modelMapper.map(saved, CustomerResponseDTO.class);

        } catch (Exception e) {

            System.err.println("Lỗi upload");
            e.printStackTrace();

            throw new RuntimeException("Không thể upload avatar: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CustomerResponseDTO uploadBanner(MultipartFile file) {
        try {
            Customer customer = getCurrentCustomer();
            String oldBannerUrl = customer.getBannerUrl();
            String newBannerUrl = mediaClient.uploadImage(file, "banners").getResult();

            if (oldBannerUrl != null && !oldBannerUrl.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        mediaClient.deleteImage(oldBannerUrl);
                    } catch (Exception e) {
                        System.err.println("Lỗi xóa banner cũ: " + e.getMessage());
                    }
                });
            }

            customer.setBannerUrl(newBannerUrl);
            Customer saved = customerRepository.save(customer);
            return modelMapper.map(saved, CustomerResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload banner: " + e.getMessage());
        }
    }


    @Override
    public CustomerResponseDTO getMyProfile() {
        return modelMapper.map(getCurrentCustomer(), CustomerResponseDTO.class);
    }

    @Override
    public CustomerPublicResponseDTO getPublicProfile(String slug) {
        Customer customer = customerRepository.findByPublicId(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin chủ bài đăng!"));

        return modelMapper.map(customer, CustomerPublicResponseDTO.class);
    }

    @Override
    @Transactional
    public CustomerResponseDTO updateProfile(CustomerProfileDTO dto) {
        Customer customer = getCurrentCustomer();

        if (dto.getFullName() != null) customer.setFullName(dto.getFullName());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getAvatarUrl() != null) customer.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getBannerUrl() != null) customer.setBannerUrl(dto.getBannerUrl());
        if (dto.getLifestyleProfile() != null) customer.setLifestyleProfile(dto.getLifestyleProfile());

        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerResponseDTO.class);
    }

    @Override
    public KycOcrResponseDTO scanCitizenId(MultipartFile image) {
        Customer customer = getCurrentCustomer();
        if ("VERIFIED".equals(customer.getKycStatus()) || "PENDING".equals(customer.getKycStatus())) {
            throw new IllegalArgumentException("Tài khoản của bạn đã xác thực hoặc đang chờ duyệt!");
        }
        try {

            FptOcrResponse response = fptAiClient.detectIdCard(fptApiKey, image);

            if (response.getErrorCode() != 0 || response.getData() == null || response.getData().isEmpty()) {
                throw new IllegalArgumentException("Ảnh CCCD không hợp lệ hoặc không thể đọc được! (Lỗi từ FPT: " + response.getErrorMessage() + ")");
            }

            FptOcrResponse.DataDetail details = response.getData().get(0);
            String ocrCitizenId = details.getId();
            if (citizenIdBloomFilter.contains(ocrCitizenId)) {

                if (customerRepository.existsByCitizenIdAndIdNot(ocrCitizenId, customer.getId())) {
                    throw new IllegalArgumentException("Số CCCD này đã được sử dụng bởi một tài khoản khác!");
                }
            }

            double idProb = (details.getIdProb() != null) ? Double.parseDouble(details.getIdProb()) : 0.0;
            double nameProb = (details.getNameProb() != null) ? Double.parseDouble(details.getNameProb()) : 0.0;
            double addressProb = (details.getAddressProb() != null) ? Double.parseDouble(details.getAddressProb()) : 0.0;

            double realAiScore = (idProb + nameProb + addressProb) / 3.0;


            String kycToken = UUID.randomUUID().toString();
            String redisValue = details.getId() + "|" + realAiScore;

            redisTemplate.opsForValue().set(
                    "KYC_SESSION:" + kycToken,
                    redisValue,
                    15, TimeUnit.MINUTES
            );

            return KycOcrResponseDTO.builder()
                    .kycToken(kycToken)
                    .citizenId(details.getId())
                    .fullName(details.getName())
                    .address(details.getAddress())
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể bóc tách thông tin CCCD: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void submitKyc(String kycToken, String citizenId, String fullName, String address, MultipartFile frontImage, MultipartFile backImage) {
        Customer customer = getCurrentCustomer();

        if ("VERIFIED".equals(customer.getKycStatus()) || "PENDING".equals(customer.getKycStatus())) {
            throw new IllegalArgumentException("Tài khoản của bạn đã được xác thực hoặc đang chờ duyệt!");
        }

        String redisKey = "KYC_SESSION:" + kycToken;
        String redisData = redisTemplate.opsForValue().get(redisKey);

        if (redisData == null) {
            throw new IllegalArgumentException("Phiên xác minh đã hết hạn hoặc không hợp lệ. Vui lòng quét lại ảnh!");
        }


        String[] parts = redisData.split("\\|");
        String savedCitizenId = parts[0];
        double aiScore = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.85;

        if (!savedCitizenId.equals(citizenId)) {
            throw new IllegalArgumentException("Dữ liệu không khớp với ảnh gốc đã quét!");
        }

        if (citizenIdBloomFilter.contains(citizenId)) {
            if (customerRepository.existsByCitizenIdAndIdNot(citizenId, customer.getId())) {
                throw new IllegalArgumentException("Số CCCD này đã được sử dụng bởi một tài khoản khác!");
            }
        }

        if (aiScore < 0.80) {
            throw new IllegalArgumentException("Ảnh CCCD của bạn quá mờ, bị chói sáng hoặc có dấu hiệu chỉnh sửa. Vui lòng ra nơi đủ sáng và chụp lại!");
        }
        List<String> oldKycImages = customer.getCitizenImages() != null ?
                new java.util.ArrayList<>(customer.getCitizenImages()) : null;

        try {
            CompletableFuture<String> frontFuture = CompletableFuture.supplyAsync(() ->
                    mediaClient.uploadImage(frontImage, "kyc").getResult()
            );
            CompletableFuture<String> backFuture = CompletableFuture.supplyAsync(() ->
                    mediaClient.uploadImage(backImage, "kyc").getResult()
            );

            CompletableFuture.allOf(frontFuture, backFuture).join();

            customer.setCitizenId(citizenId);
            customer.setFullName(fullName);
            customer.setAddress(address);
            customer.setCitizenImages(List.of(frontFuture.join(), backFuture.join()));

            if (aiScore >= 0.95) {
                customer.setKycStatus("VERIFIED");


                CompletableFuture.runAsync(() -> {

                    kafkaTemplate.send("kyc-approved-topic", customer.getEmail());
                    System.out.println("Đã bắn event KYC thành công cho: " + customer.getEmail());
                });

            }else {
                customer.setKycStatus("PENDING");
            }
            redisTemplate.delete(redisKey);
            customerRepository.save(customer);

            citizenIdBloomFilter.add(citizenId);

            if (oldKycImages != null && !oldKycImages.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    oldKycImages.forEach(url -> {
                        try {
                            mediaClient.deleteImage(url);
                        } catch (Exception ignored) {
                        }
                    });
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý hồ sơ KYC: " + e.getMessage());
        }
    }

    private String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UUID.randomUUID().toString().substring(0, 10);
        }

        String slug = name.replace("đ", "d").replace("Đ", "D");

        String normalized = Normalizer.normalize(slug, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        slug = pattern.matcher(normalized).replaceAll("");

        slug = slug.toLowerCase();
        slug = slug.replaceAll("[^a-z0-9]+", "-");

        slug = slug.replaceAll("^-|-$", "");
        String randomTail = UUID.randomUUID().toString().substring(0, 5);

        return slug + "-" + randomTail;
    }
}