package com.homeverse.property.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.property.dto.request.PropertyCreateDTO;
import com.homeverse.property.dto.response.PropertyReelResponseDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.dto.response.ReelsFeedResponse;
import com.homeverse.property.entity.OwnerProfile;
import com.homeverse.property.entity.OwnerQuota;
import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.repository.AmenityRepository;
import com.homeverse.property.repository.OwnerQuotaRepository;
import com.homeverse.property.repository.ProjectRepository;
import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.repository.OwnerProfileRepository;
import com.homeverse.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final ProjectRepository projectRepository;
    private final AmenityRepository amenityRepository;
    private final OwnerQuotaRepository ownerQuotaRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final com.homeverse.property.repository.InteractionRepository interactionRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PropertyResponseDTO createProperty(Long ownerId, PropertyCreateDTO dto) {


        validatePropertyData(dto);

        OwnerQuota quota = ownerQuotaRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        OwnerProfile profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (quota.getFreePostsRemaining() <= 0) {
            log.warn("Chủ nhà {} cố tình tạo bài đăng nhưng đã hết lượt!", ownerId);
            throw new AppException(ErrorCode.POST_LIMIT_EXCEEDED);
        }
        if (quota.getRole() == null || !quota.getRole().contains("OWNER")) {
            log.warn("User {} chưa KYC hoặc bị tước quyền cố tình đăng bài!", ownerId);
            throw new AppException(ErrorCode.KYC_NOT_VERIFIED);
        }

        if (dto.getProjectId() != null) {
            if (!projectRepository.existsById(dto.getProjectId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }
        String snapshotName = null;
        if (dto.getProjectId() != null) {
            com.homeverse.property.entity.Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

            // Nếu dự án bị khóa, không cho thêm bài mới vào
            if (project.getStatus() == com.homeverse.property.entity.Project.Status.INACTIVE) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            snapshotName = project.getName();
        }
        Property.LegalDocumentType legalType = Property.LegalDocumentType.NONE;
        if (dto.getLegalDocumentType() != null) {
            try {
                legalType = Property.LegalDocumentType.valueOf(dto.getLegalDocumentType());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }
        // 2. Chỉ tạo Point khi tọa độ đã được xác nhận hợp lệ
        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));


        int days = (dto.getValidityDays() != null && dto.getValidityDays() > 0) ? dto.getValidityDays() : 30;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDate = now.plusDays(days);

        // 4. Khởi tạo Entity (Lưu mặc định là PENDING, không tốn Quota)
        Property property = Property.builder()
                .projectId(dto.getProjectId())
                .projectNameSnapshot(snapshotName)
                .title(dto.getTitle().trim())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .area(dto.getArea())
                .address(dto.getAddress().trim())
                .province(dto.getProvince())
                .street(dto.getStreet().trim())
                .ward(dto.getWard().trim())
                .district(dto.getDistrict().trim())
                .location(point)
                .propertyType(Property.PropertyType.valueOf(dto.getPropertyType()))
                .transactionType(Property.TransactionType.valueOf(dto.getTransactionType()))
                .capacity(dto.getCapacity())
                .images(dto.getImages())
                .videoUrl(dto.getVideoUrl())
                .amenities(dto.getAmenities())
                .status(Property.Status.PENDING)
                .ownerId(ownerId)
                .ownerNameSnapshot(profile.getFullName())
                .ownerAvatarSnapshot(profile.getAvatar())
                .ownerSlugSnapshot(profile.getSlug())
                .createdAt(now)
                .expiresAt(expirationDate)
                .legalDocumentType(legalType)
                .bedrooms(dto.getBedrooms() != null ? dto.getBedrooms() : 0)
                .bathrooms(dto.getBathrooms() != null ? dto.getBathrooms() : 0)
                .hasBalcony(dto.getHasBalcony() != null ? dto.getHasBalcony() : false)
                .furnishingStatus(dto.getFurnishingStatus() != null ? Property.FurnishingStatus.valueOf(dto.getFurnishingStatus()) : null)
                .availabilityStatus(dto.getAvailabilityStatus() != null ? Property.AvailabilityStatus.valueOf(dto.getAvailabilityStatus()) : null)
                .electricityPrice(dto.getElectricityPrice() != null ? Property.UtilityPriceType.valueOf(dto.getElectricityPrice()) : null)
                .waterPrice(dto.getWaterPrice() != null ? Property.UtilityPriceType.valueOf(dto.getWaterPrice()) : null)
                .internetPrice(dto.getInternetPrice() != null ? Property.UtilityPriceType.valueOf(dto.getInternetPrice()) : null)
                .promotionPackageId(dto.getPromotionPackageId())
                .promotionPackageName(dto.getPromotionPackageName())
                .isPromoted(dto.getPromotionPackageId() != null) // Nếu có ID gói thì là true
                .promotionExpiresAt(dto.getPromotionPackageId() != null && dto.getValidityDays() != null 
                        ? LocalDateTime.now().plusDays(dto.getValidityDays()) 
                        : null)
                // -----------------------
                .build();

        // 1. Ép lưu Property và đẩy xuống DB ngay
        Property savedProperty = propertyRepository.saveAndFlush(property); 

        // 2. Trừ quota và cũng ép lưu ngay
        quota.setFreePostsRemaining(quota.getFreePostsRemaining() - 1);
        ownerQuotaRepository.saveAndFlush(quota); 

        log.info("✅ Đã tạo Property thành công. Promotion: {}, Hết hạn: {}", 
                savedProperty.getIsPromoted(), savedProperty.getPromotionExpiresAt());

        log.info("✅ Đã trừ 1 lượt đăng của User {}. Lượt còn lại: {}", 
                ownerId, quota.getFreePostsRemaining());

        // 3. Trả về kết quả
        return mapToResponse(savedProperty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PropertyResponseDTO updateProperty(Long ownerId, Long id, PropertyCreateDTO dto) {
        validatePropertyData(dto);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
        }
        OwnerProfile profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean requiresReview = false;

        // 1. Quét rủi ro: Thay đổi vị trí (Địa chỉ, Tọa độ)
        if (!property.getAddress().equals(dto.getAddress().trim()) ||
                (property.getProvince()!=null && !property.getProvince().equals(dto.getProvince())) ||
                (property.getStreet() != null && !property.getStreet().equals(dto.getStreet().trim())) ||
                (property.getWard() != null && !property.getWard().equals(dto.getWard().trim())) ||
                (property.getDistrict() != null && !property.getDistrict().equals(dto.getDistrict().trim())) ||
                Double.compare(property.getLocation().getY(), dto.getLatitude()) != 0 ||
                Double.compare(property.getLocation().getX(), dto.getLongitude()) != 0) {
            requiresReview = true;
            log.info("Phát hiện đổi Địa chỉ/Tọa độ ở bài đăng {}", id);
        }

        // 2. Quét rủi ro: Thay đổi bản chất BĐS (Loại nhà, Bán/Cho thuê)
        if (!property.getPropertyType().name().equals(dto.getPropertyType()) ||
                !property.getTransactionType().name().equals(dto.getTransactionType())) {
            requiresReview = true;
            log.info("Phát hiện đổi Loại hình BĐS ở bài đăng {}", id);
        }

        // 3. Quét rủi ro: Tráo đổi Dự án (Project)
        Long currentProjectId = property.getProjectId();
        Long newProjectId = dto.getProjectId();
        boolean projectChanged = (currentProjectId == null && newProjectId != null) ||
                (currentProjectId != null && !currentProjectId.equals(newProjectId));
        if (projectChanged) {
            requiresReview = true;
            log.info("Phát hiện đổi Dự án ở bài đăng {}", id);
        }

        // =========================================================
        // ---> CẬP NHẬT DỮ LIỆU NHƯ BÌNH THƯỜNG <---
        // =========================================================

        // Cập nhật Project
        if (newProjectId != null) {
            com.homeverse.property.entity.Project project = projectRepository.findById(newProjectId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
            if (project.getStatus() == com.homeverse.property.entity.Project.Status.INACTIVE) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            property.setProjectId(newProjectId);
            property.setProjectNameSnapshot(project.getName());
        } else {
            property.setProjectId(null);
            property.setProjectNameSnapshot(null);
        }

        // Cập nhật Tọa độ và các trường Cơ bản
        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        property.setLocation(point);
        property.setTitle(dto.getTitle().trim());
        property.setDescription(dto.getDescription());
        property.setPrice(dto.getPrice());
        property.setArea(dto.getArea());
        property.setAddress(dto.getAddress().trim());
        property.setProvince(dto.getProvince().trim());
        property.setStreet(dto.getStreet().trim());
        property.setWard(dto.getWard().trim());
        property.setDistrict(dto.getDistrict().trim());
        property.setPropertyType(Property.PropertyType.valueOf(dto.getPropertyType()));
        property.setTransactionType(Property.TransactionType.valueOf(dto.getTransactionType()));
        property.setCapacity(dto.getCapacity());
        property.setImages(dto.getImages());
        property.setVideoUrl(dto.getVideoUrl());
        property.setAmenities(dto.getAmenities());


        if (dto.getBedrooms() != null) property.setBedrooms(dto.getBedrooms());
        if (dto.getBathrooms() != null) property.setBathrooms(dto.getBathrooms());
        if (dto.getHasBalcony() != null) property.setHasBalcony(dto.getHasBalcony());

        if (dto.getLegalDocumentType() == null || dto.getLegalDocumentType().trim().isEmpty() || dto.getLegalDocumentType().equals("NONE")) {
            property.setLegalDocumentType(Property.LegalDocumentType.NONE);
        } else {
            property.setLegalDocumentType(Property.LegalDocumentType.valueOf(dto.getLegalDocumentType()));
        }
        if (dto.getFurnishingStatus() != null)
            property.setFurnishingStatus(Property.FurnishingStatus.valueOf(dto.getFurnishingStatus()));
        if (dto.getAvailabilityStatus() != null)
            property.setAvailabilityStatus(Property.AvailabilityStatus.valueOf(dto.getAvailabilityStatus()));
        if (dto.getElectricityPrice() != null)
            property.setElectricityPrice(Property.UtilityPriceType.valueOf(dto.getElectricityPrice()));
        if (dto.getWaterPrice() != null) property.setWaterPrice(Property.UtilityPriceType.valueOf(dto.getWaterPrice()));
        if (dto.getInternetPrice() != null)
            property.setInternetPrice(Property.UtilityPriceType.valueOf(dto.getInternetPrice()));

        // =========================================================
        // ---> CẬP NHẬT PROMOTION (NẾU CÓ) <---
        // =========================================================
        // Lưu ý: Chỉ cập nhật nếu DTO có truyền thông tin gói mới
        if (dto.getPromotionPackageId() != null) {
            property.setPromotionPackageId(dto.getPromotionPackageId());
            property.setPromotionPackageName(dto.getPromotionPackageName());
            
            // Nếu bài đăng chưa được promoted hoặc gói cũ đã hết hạn, thì mới set hạn mới
            // Còn nếu đang còn hạn, việc cộng dồn thường được xử lý qua Kafka thanh toán
            if (property.getIsPromoted() == null || !property.getIsPromoted() || 
                property.getPromotionExpiresAt() == null || property.getPromotionExpiresAt().isBefore(LocalDateTime.now())) {
                
                property.setIsPromoted(true);
                if (dto.getValidityDays() != null) {
                    property.setPromotionExpiresAt(LocalDateTime.now().plusDays(dto.getValidityDays()));
                }
            } else {
                // Nếu đang còn hạn mà update bài đăng, ta chỉ cập nhật tên gói (nếu có thay đổi) 
                // và giữ nguyên IsPromoted là true.
                property.setIsPromoted(true); 
            }
        }

        // =========================================================
        // ---> PHÁN QUYẾT CUỐI CÙNG (CHỐT STATUS) <---
        // =========================================================
        if (requiresReview) {
            property.setStatus(Property.Status.PENDING);
        }
        
        Property updatedProperty = propertyRepository.save(property);
        return mapToResponse(updatedProperty);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProperty(Long ownerId, Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
        }

        propertyRepository.delete(property);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hardDeleteProperty(Long ownerId, Long id) {
        // 1. Dùng hàm Native Query để móc bài đăng từ trong thùng rác ra
        Property property = propertyRepository.findDeletedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        // 2. Kiểm tra xem người đang bấm xóa có đúng là chủ bài đăng không
        if (!property.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
        }

        // 3. Ra đòn kết liễu - Xóa sạch khỏi Database
        propertyRepository.hardDeleteById(id);
        log.info("Chủ nhà {} đã XÓA VĨNH VIỄN bài đăng ID: {}", ownerId, id);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProperty(Long ownerId, Long id) {
        // 1. Tìm bài trong thùng rác
        Property property = propertyRepository.findDeletedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        // 2. Kiểm tra quyền
        if (!property.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
        }

        // 3. Khôi phục về trạng thái PENDING (Chờ Admin duyệt lại)
        propertyRepository.restoreById(id);
        log.info("Chủ nhà {} đã KHÔI PHỤC bài đăng ID: {}", ownerId, id);
    }

    // ==========================================
    // ---> TÍNH NĂNG THÙNG RÁC: XEM RÁC CỦA TÔI <---
    // ==========================================
    @Override
    @Transactional(readOnly = true) // Tối ưu tốc độ cho hàm chỉ đọc (GET)
    public org.springframework.data.domain.Page<PropertyResponseDTO> getMyDeletedProperties(Long ownerId, int page, int size) {

        // Tạo bộ phân trang (Ví dụ: Trang 0, mỗi trang lấy 10 bài)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Gọi xuống DB móc rác lên
        org.springframework.data.domain.Page<Property> deletedProperties = propertyRepository.findDeletedByOwnerId(ownerId, pageable);

        // Convert cả một mảng Page<Property> sang Page<DTO> cực kỳ gọn gàng
        return deletedProperties.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getPublicProperties(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Chỉ móc những bài đăng có trạng thái ACTIVE (Đã duyệt)
        return propertyRepository.findByStatus(Property.Status.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponseDTO getPublicPropertyDetail(Long id) {
        // Chỉ cho phép xem nếu bài đăng tồn tại và đang ACTIVE
        Property property = propertyRepository.findByIdAndStatus(id, Property.Status.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        return mapToResponse(property);
    }

    //  hàm getReelsFeed
    @Override
    @Transactional(readOnly = true)
    public ReelsFeedResponse getReelsFeed(Long currentUserId,String guestId, String cursor, int size) {

        // 1. Decode cursor thành lastCreatedAt + lastId
        LocalDateTime lastCreatedAt = null;
        Long lastId = null;

        if (cursor != null && !cursor.trim().isEmpty()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
                String[] parts = decoded.split("\\|", 2);   // tách bằng dấu |
                if (parts.length == 2) {
                    lastCreatedAt = java.time.LocalDateTime.parse(parts[0]);
                    lastId = Long.parseLong(parts[1]);
                }
            } catch (Exception e) {
                // Nếu cursor hỏng → coi như first page (an toàn)
                lastCreatedAt = null;
                lastId = null;
            }
        }

        // 2. Query data từ Database
        List<Property> properties = propertyRepository.findReelsFeed(
                Property.Status.ACTIVE,
                lastCreatedAt,
                lastId,
                size);

        // Thoát sớm nếu không có dữ liệu để tiết kiệm tài nguyên
        if (properties.isEmpty()) {
            return ReelsFeedResponse.builder()
                    .items(List.of())
                    .nextCursor(null)
                    .build();
        }

        // ====================================================================
        // --- BẮT ĐẦU PHẦN TỐI ƯU MỚI (LIKE, SAVE & REDIS O(1)) ---
        // ====================================================================

        List<Long> propertyIds = properties.stream().map(Property::getId).toList();

        // A. Xử lý Like/Save của User bằng HashSet (Tra cứu O(1))
        Set<Long> likedPropertyIds = new HashSet<>();
        Set<Long> savedPropertyIds = new HashSet<>();

        // NẾU CÓ ĐĂNG NHẬP (currentUserId) HOẶC CÓ MÃ KHÁCH (guestId) THÌ MỚI QUERY DB
        if (currentUserId != null || (guestId != null && !guestId.trim().isEmpty())) {

            // Gọi hàm mới truyền cả 2 tham số
            List<UserPropertyInteraction> userInteractions =
                    interactionRepository.findInteractionsIn(currentUserId, guestId, propertyIds);

            for (UserPropertyInteraction interaction : userInteractions) {
                if (interaction.getInteractionType() == UserPropertyInteraction.InteractionType.LIKE) {
                    likedPropertyIds.add(interaction.getPropertyId());
                } else if (interaction.getInteractionType() == UserPropertyInteraction.InteractionType.SAVE) {
                    savedPropertyIds.add(interaction.getPropertyId());
                }
            }
        }

        // B. Lấy tổng Like từ Redis (Dùng MGET)
        List<String> redisKeys = propertyIds.stream().map(id -> "property:" + id + ":likes").toList();
        List<String> redisLikeCounts = redisTemplate.opsForValue().multiGet(redisKeys);

        // C. Lắp ráp dữ liệu vào DTO
        List<PropertyReelResponseDTO> dtoList = new ArrayList<>(properties.size());

        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            Long pId = property.getId();

            PropertyReelResponseDTO dto = toReelDTO(property);

            dto.setLiked(likedPropertyIds.contains(pId));
            dto.setSaved(savedPropertyIds.contains(pId));

            String likeStr = redisLikeCounts != null ? redisLikeCounts.get(i) : null;
            dto.setLikeCount(likeStr != null ? Long.parseLong(likeStr) : 0L);

            dtoList.add(dto);
        }

        // 3. Encode cursor cho lần sau
        Property last = properties.get(properties.size() - 1);
        String raw = last.getCreatedAt() + "|" + last.getId();
        String nextCursor = java.util.Base64.getEncoder().encodeToString(raw.getBytes());

        return ReelsFeedResponse.builder()
                .items(dtoList)
                .nextCursor(nextCursor)
                .build();
    }




    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getPropertiesByOwnerId(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return propertyRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, Property.Status.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    private void validatePropertyData(PropertyCreateDTO dto) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (dto.getArea() == null || dto.getArea() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (dto.getLatitude() == null || dto.getLatitude() < -90 || dto.getLatitude() > 90 ||
                dto.getLongitude() == null || dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            throw new AppException(ErrorCode.LOCATION_REQUIRED);
        }


        if (dto.getPropertyType() == null || dto.getTransactionType() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        try {
            Property.PropertyType.valueOf(dto.getPropertyType());
            Property.TransactionType.valueOf(dto.getTransactionType());

            if (dto.getFurnishingStatus() != null) Property.FurnishingStatus.valueOf(dto.getFurnishingStatus());
            if (dto.getAvailabilityStatus() != null) Property.AvailabilityStatus.valueOf(dto.getAvailabilityStatus());
            if (dto.getElectricityPrice() != null) Property.UtilityPriceType.valueOf(dto.getElectricityPrice());
            if (dto.getWaterPrice() != null) Property.UtilityPriceType.valueOf(dto.getWaterPrice());
            if (dto.getInternetPrice() != null) Property.UtilityPriceType.valueOf(dto.getInternetPrice());
            if (dto.getLegalDocumentType() != null) Property.LegalDocumentType.valueOf(dto.getLegalDocumentType());
        } catch (IllegalArgumentException e) {

            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (dto.getAmenities() != null && !dto.getAmenities().isEmpty()) {
            // Lọc ra các tên không bị rỗng
            List<String> validAmenityNames = dto.getAmenities().stream()
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();

            if (!validAmenityNames.isEmpty()) {
                // Chỉ gọi ĐÚNG 1 CÂU QUERY đếm số lượng dưới DB
                long countInDb = amenityRepository.countByNames(validAmenityNames);

                // Nếu số lượng tìm thấy dưới DB không bằng số lượng khách gửi lên -> Có kẻ tráo data giả!
                if (countInDb != validAmenityNames.size()) {
                    log.error("Phát hiện có tiện ích giả mạo không tồn tại trong DB!");
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
            }
        }
    }

    private PropertyResponseDTO mapToResponse(Property property) {
    PropertyResponseDTO dto = new PropertyResponseDTO();

    dto.setId(property.getId());
    dto.setProjectId(property.getProjectId());
    dto.setTitle(property.getTitle());
    dto.setDescription(property.getDescription());
    dto.setPrice(property.getPrice());
    dto.setPricePerSqm(property.getPricePerSqm());
    dto.setArea(property.getArea());
    dto.setAddress(property.getAddress());
    dto.setProvince(property.getProvince());
    dto.setStreet(property.getStreet());
    dto.setWard(property.getWard());
    dto.setDistrict(property.getDistrict());

    if (property.getLocation() != null) {
        dto.setLongitude(property.getLocation().getX());
        dto.setLatitude(property.getLocation().getY());
    }
    if (property.getPropertyType() != null) {
        dto.setPropertyType(property.getPropertyType().name());
    }
    if (property.getTransactionType() != null) {
        dto.setTransactionType(property.getTransactionType().name());
    }
    if (property.getStatus() != null) {
        dto.setStatus(property.getStatus().name());
    }
    if (property.getLegalDocumentType() != null) {
        dto.setLegalDocumentType(property.getLegalDocumentType().name());
    }
    
    dto.setCapacity(property.getCapacity());
    dto.setImages(property.getImages());
    dto.setAmenities(property.getAmenities());
    dto.setVideoUrl(property.getVideoUrl());
    dto.setProjectNameSnapshot(property.getProjectNameSnapshot());
    dto.setQuotaDeducted(property.isQuotaDeducted());

    // --- BỔ SUNG PROMOTION TẠI ĐÂY ---
    dto.setIsPromoted(property.getIsPromoted() != null && property.getIsPromoted());
    dto.setPromotionExpiresAt(property.getPromotionExpiresAt());
    dto.setPromotionPackageId(property.getPromotionPackageId());
    dto.setPromotionPackageName(property.getPromotionPackageName());
    // --------------------------------

    dto.setOwnerId(property.getOwnerId());
    dto.setCreatedAt(property.getCreatedAt());
    dto.setExpiresAt(property.getExpiresAt());
    dto.setBedrooms(property.getBedrooms());
    dto.setBathrooms(property.getBathrooms());
    dto.setHasBalcony(property.getHasBalcony());

    if (property.getFurnishingStatus() != null) dto.setFurnishingStatus(property.getFurnishingStatus().name());
    if (property.getAvailabilityStatus() != null)
        dto.setAvailabilityStatus(property.getAvailabilityStatus().name());
    if (property.getElectricityPrice() != null) dto.setElectricityPrice(property.getElectricityPrice().name());
    if (property.getWaterPrice() != null) dto.setWaterPrice(property.getWaterPrice().name());
    if (property.getInternetPrice() != null) dto.setInternetPrice(property.getInternetPrice().name());

    return dto;
}
    private PropertyReelResponseDTO toReelDTO(Property property) {
    PropertyReelResponseDTO dto = new PropertyReelResponseDTO();
    dto.setId(property.getId());
    dto.setTitle(property.getTitle());
    dto.setPrice(property.getPrice());
    dto.setAddress(property.getAddress());
    dto.setVideoUrl(property.getVideoUrl());
    dto.setOwnerSlug(property.getOwnerSlugSnapshot());
    dto.setOwnerNameSnapshot(property.getOwnerNameSnapshot());
    dto.setOwnerAvatarSnapshot(property.getOwnerAvatarSnapshot());
    dto.setCreatedAt(property.getCreatedAt());

    // --- BỔ SUNG PROMOTION CHO REEL ---
    // Chỉ cần trả về IsPromoted để FE hiển thị badge "Vip/Hot"
    dto.setIsPromoted(property.getIsPromoted() != null && property.getIsPromoted());
    // ----------------------------------

    return dto;
}
}