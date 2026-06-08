package com.homeverse.property.service.impl;

import com.homeverse.common.dto.NotificationEvent;
import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.property.config.RecommendClient;
import com.homeverse.property.dto.request.PropertyCreateDTO;
import com.homeverse.property.dto.request.TrackEventRequest;
import com.homeverse.property.dto.response.*;
import com.homeverse.property.entity.OwnerProfile;
import com.homeverse.property.entity.OwnerQuota;
import com.homeverse.property.entity.Project;
import com.homeverse.property.entity.PromotionQueue;
import com.homeverse.property.entity.UserPropertyInteraction;
import com.homeverse.property.repository.AmenityRepository;
import com.homeverse.property.repository.OwnerQuotaRepository;
import com.homeverse.property.repository.ProjectRepository;
import com.homeverse.property.repository.PromotionQueueRepository;
import com.homeverse.common.dto.PropertyQuotaSyncEvent;
import org.springframework.kafka.core.KafkaTemplate;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.homeverse.property.repository.PropertyCommentRepository;
import com.homeverse.property.repository.PropertyContactRepository;
import com.homeverse.property.entity.PropertyComment;
import com.homeverse.property.entity.PropertyContact;

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
    private final PromotionQueueRepository promotionQueueRepository;
    private final PropertyCommentRepository commentRepository;
    private final RecommendClient recommendClient;
    private final KafkaTemplate<String, Object> objectKafkaTemplate;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final PropertyContactRepository propertyContactRepository;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PropertyResponseDTO createProperty(Long ownerId, PropertyCreateDTO dto) {
        validatePropertyData(dto);

        OwnerQuota quota = ownerQuotaRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        OwnerProfile profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (quota.getFreePostsRemaining() <= 0) {
            throw new AppException(ErrorCode.POST_LIMIT_EXCEEDED);
        }

        if (quota.getRole() == null || !quota.getRole().contains("OWNER")) {
            throw new AppException(ErrorCode.KYC_NOT_VERIFIED);
        }
        quota.setFreePostsRemaining(quota.getFreePostsRemaining() - 1);
        ownerQuotaRepository.save(quota);
        sendQuotaSyncEvent(quota, "PROPERTY_CREATED_PENDING");

        String snapshotName = null;
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

            if (project.getStatus() == Project.Status.INACTIVE) {
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

        Point point = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        int days = (dto.getValidityDays() != null && dto.getValidityDays() > 0) ? dto.getValidityDays() : 30;
        LocalDateTime now = LocalDateTime.now();

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
                .ownerPhoneSnapshot(profile.getPhone())
                .ownerAvatarSnapshot(profile.getAvatar())
                .ownerSlugSnapshot(profile.getSlug())
                .createdAt(now)
                .expiresAt(now.plusDays(days))
                .legalDocumentType(legalType)
                .bedrooms(dto.getBedrooms() != null ? dto.getBedrooms() : 0)
                .bathrooms(dto.getBathrooms() != null ? dto.getBathrooms() : 0)
                .hasBalcony(dto.getHasBalcony() != null ? dto.getHasBalcony() : false)
                .furnishingStatus(
                        dto.getFurnishingStatus() != null ? Property.FurnishingStatus.valueOf(dto.getFurnishingStatus())
                                : null)
                .availabilityStatus(dto.getAvailabilityStatus() != null
                        ? Property.AvailabilityStatus.valueOf(dto.getAvailabilityStatus())
                        : null)
                .electricityPrice(
                        dto.getElectricityPrice() != null ? Property.UtilityPriceType.valueOf(dto.getElectricityPrice())
                                : null)
                .waterPrice(dto.getWaterPrice() != null ? Property.UtilityPriceType.valueOf(dto.getWaterPrice()) : null)
                .internetPrice(
                        dto.getInternetPrice() != null ? Property.UtilityPriceType.valueOf(dto.getInternetPrice())
                                : null)
                .promotionPackageId(null)
                .promotionPackageName(null)
                .isPromoted(false)
                .promotionExpiresAt(null)
                .isQuotaDeducted(true)
                .build();

        Property savedProperty = propertyRepository.save(property);

        log.info("Created Property ID: {}", savedProperty.getId());

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

        boolean requiresReview = false;

        if (!property.getAddress().equals(dto.getAddress().trim()) ||
                (property.getProvince() != null && !property.getProvince().equals(dto.getProvince())) ||
                Double.compare(property.getLocation().getY(), dto.getLatitude()) != 0 ||
                Double.compare(property.getLocation().getX(), dto.getLongitude()) != 0) {
            requiresReview = true;
        }

        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
            property.setProjectId(dto.getProjectId());
            property.setProjectNameSnapshot(project.getName());
        }

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
        property.setAmenities(dto.getAmenities());

        if (dto.getBedrooms() != null)
            property.setBedrooms(dto.getBedrooms());
        if (dto.getBathrooms() != null)
            property.setBathrooms(dto.getBathrooms());
        if (dto.getHasBalcony() != null)
            property.setHasBalcony(dto.getHasBalcony());

        if (requiresReview) {
            property.setStatus(Property.Status.PENDING);
            property.setIsPromoted(false);
            property.setPromotionPackageId(null);
            property.setPromotionPackageName(null);
            property.setPromotionExpiresAt(null);

            promotionQueueRepository
                    .findFirstByPropertyIdAndStatusOrderByPriorityLevelDesc(id, PromotionQueue.PromotionStatus.ACTIVE)
                    .ifPresent(active -> {
                        active.setStatus(PromotionQueue.PromotionStatus.WAITING);
                        promotionQueueRepository.save(active);
                    });
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
    public org.springframework.data.domain.Page<PropertyResponseDTO> getMyDeletedProperties(Long ownerId, int page,
            int size) {

        // Tạo bộ phân trang (Ví dụ: Trang 0, mỗi trang lấy 10 bài)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        // Gọi xuống DB móc rác lên
        org.springframework.data.domain.Page<Property> deletedProperties = propertyRepository
                .findDeletedByOwnerId(ownerId, pageable);

        // Convert cả một mảng Page<Property> sang Page<DTO> cực kỳ gọn gàng
        return deletedProperties.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getPublicProperties(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        return propertyRepository
                .findByStatusAndExpiresAtAfter(Property.Status.ACTIVE, LocalDateTime.now(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponseDTO getPublicPropertyDetail(Long id) {

        Property property = propertyRepository
                .findByIdAndStatusAndExpiresAtAfter(id, Property.Status.ACTIVE, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        return mapToResponse(property);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerPublicPropertiesResponse getOwnerPublicProperties(
            Long ownerId,
            int page,
            int size,
            String propertyType) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Property> propertyPage;

        if (propertyType != null && !propertyType.isBlank()) {
            Property.PropertyType type;

            try {
                type = Property.PropertyType.valueOf(propertyType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            propertyPage = propertyRepository
                    .findByOwnerIdAndStatusAndPropertyTypeOrderByCreatedAtDesc(
                            ownerId,
                            Property.Status.ACTIVE,
                            type,
                            pageable);
        } else {
            propertyPage = propertyRepository
                    .findByOwnerIdAndStatusOrderByCreatedAtDesc(
                            ownerId,
                            Property.Status.ACTIVE,
                            pageable);
        }

        Page<PropertyResponseDTO> properties = propertyPage.map(this::mapToResponse);

        List<PropertyTypeCountDTO> typeCounts = propertyRepository.countByOwnerIdAndStatusGroupByPropertyType(
                ownerId,
                Property.Status.ACTIVE);

        Long totalActivePosts = typeCounts.stream()
                .mapToLong(PropertyTypeCountDTO::getTotal)
                .sum();

        return OwnerPublicPropertiesResponse.builder()
                .ownerId(ownerId)
                .totalActivePosts(totalActivePosts)
                .typeCounts(typeCounts)
                .properties(properties)
                .build();
    }

    // hàm getReelsFeed
    @Override
    @Transactional(readOnly = true)
    public ReelsFeedResponse getReelsFeed(Long currentUserId, String guestId, String cursor, int size) {

        // 1. Decode cursor thành lastCreatedAt + lastId
        LocalDateTime lastCreatedAt = null;
        Long lastId = null;

        if (cursor != null && !cursor.trim().isEmpty()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
                String[] parts = decoded.split("\\|", 2); // tách bằng dấu |
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
        List<Property> properties;

        if (lastCreatedAt == null || lastId == null) {
            properties = propertyRepository.findFirstReelsFeed(
                    Property.Status.ACTIVE.name(),
                    size);
        } else {
            properties = propertyRepository.findNextReelsFeed(
                    Property.Status.ACTIVE.name(),
                    lastCreatedAt,
                    lastId,
                    size);
        }

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
            List<UserPropertyInteraction> userInteractions = interactionRepository.findInteractionsIn(currentUserId,
                    guestId, propertyIds);

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
    public PropertyReelResponseDTO getPropertyReelById(Long id) {

        PropertyResponseDTO property = getPublicPropertyDetail(id);

        PropertyReelResponseDTO dto = new PropertyReelResponseDTO();

        dto.setId(property.getId());
        dto.setTitle(property.getTitle());
        dto.setPrice(property.getPrice());
        dto.setAddress(property.getAddress());
        dto.setVideoUrl(property.getVideoUrl());
        dto.setCreatedAt(property.getCreatedAt());
        dto.setIsPromoted(property.getIsPromoted());

        dto.setLiked(false);
        dto.setSaved(false);
        dto.setLikeCount(0L);

        return dto;
    }

    @Override
    public List<PropertyResponseDTO> getPromotedProperties() {
        return propertyRepository
                .findPromotedProperties()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<PropertyResponseDTO> getTrendingProperties() {
        return propertyRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<PropertyResponseDTO> getRandomProperties() {
        List<PropertyResponseDTO> result = propertyRepository.findRandomProperties()
                .stream()
                .map(this::mapToResponse)
                .toList();

        System.out.println("RANDOM PROPERTY SIZE = " + result.size());

        return result;
    }

    @Override
    public List<PropertyReelResponseDTO> getPromotedReels() {
        return propertyRepository
                .findPromotedReels()
                .stream()
                .map(this::toReelDTO)
                .toList();
    }

    @Override
    public List<PropertyReelResponseDTO> getTrendingReels() {
        return propertyRepository
                .findTop10ByVideoUrlIsNotNullOrderByCreatedAtDesc()
                .stream()
                .map(this::toReelDTO)
                .toList();
    }

    @Override
    public List<PropertyReelResponseDTO> getRandomReels() {
        return propertyRepository
                .findRandomReels()
                .stream()
                .map(this::toReelDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getPropertiesByOwnerId(
            Long ownerId,
            int page,
            int size,
            String transactionType) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();

        if (transactionType != null && !transactionType.isBlank()) {
            Property.TransactionType type;

            try {
                type = Property.TransactionType.valueOf(transactionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            return propertyRepository
                    .findByOwnerIdAndStatusAndTransactionTypeAndExpiresAtAfterOrderByCreatedAtDesc(
                            ownerId,
                            Property.Status.ACTIVE,
                            type,
                            now,
                            pageable)
                    .map(this::mapToResponse);
        }

        return propertyRepository
                .findByOwnerIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        ownerId,
                        Property.Status.ACTIVE,
                        now,
                        pageable)
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

            if (dto.getFurnishingStatus() != null)
                Property.FurnishingStatus.valueOf(dto.getFurnishingStatus());
            if (dto.getAvailabilityStatus() != null)
                Property.AvailabilityStatus.valueOf(dto.getAvailabilityStatus());
            if (dto.getElectricityPrice() != null)
                Property.UtilityPriceType.valueOf(dto.getElectricityPrice());
            if (dto.getWaterPrice() != null)
                Property.UtilityPriceType.valueOf(dto.getWaterPrice());
            if (dto.getInternetPrice() != null)
                Property.UtilityPriceType.valueOf(dto.getInternetPrice());
            if (dto.getLegalDocumentType() != null)
                Property.LegalDocumentType.valueOf(dto.getLegalDocumentType());
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

                // Nếu số lượng tìm thấy dưới DB không bằng số lượng khách gửi lên -> Có kẻ tráo
                // data giả!
                if (countInDb != validAmenityNames.size()) {
                    log.error("Phát hiện có tiện ích giả mạo không tồn tại trong DB!");
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
            }
        }
    }

    @Override
    public Double getOwnerTrustScore(Long ownerId) {
        long activeCount = propertyRepository.countByOwnerIdAndStatus(
                ownerId,
                Property.Status.ACTIVE);

        long promotedCount = propertyRepository.countByOwnerIdAndIsPromotedTrueAndStatus(
                ownerId,
                Property.Status.ACTIVE);

        double activeScore = Math.min(activeCount / 20.0, 1.0);
        double promotedScore = Math.min(promotedCount / 5.0, 1.0);

        return activeScore * 0.7 + promotedScore * 0.3;
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
        dto.setOwnerNameSnapshot(property.getOwnerNameSnapshot());
        dto.setOwnerAvatarSnapshot(property.getOwnerAvatarSnapshot());
        dto.setOwnerSlugSnapshot(property.getOwnerSlugSnapshot());
        dto.setOwnerPhoneSnapshot(property.getOwnerPhoneSnapshot());
        dto.setCreatedAt(property.getCreatedAt());
        dto.setExpiresAt(property.getExpiresAt());
        dto.setBedrooms(property.getBedrooms());
        dto.setBathrooms(property.getBathrooms());
        dto.setHasBalcony(property.getHasBalcony());

        if (property.getFurnishingStatus() != null)
            dto.setFurnishingStatus(property.getFurnishingStatus().name());
        if (property.getAvailabilityStatus() != null)
            dto.setAvailabilityStatus(property.getAvailabilityStatus().name());
        if (property.getElectricityPrice() != null)
            dto.setElectricityPrice(property.getElectricityPrice().name());
        if (property.getWaterPrice() != null)
            dto.setWaterPrice(property.getWaterPrice().name());
        if (property.getInternetPrice() != null)
            dto.setInternetPrice(property.getInternetPrice().name());

        if (property.getInternetPrice() != null)
            dto.setInternetPrice(property.getInternetPrice().name());

        Long propertyId = property.getId();

        String likeStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":likes");

        String saveStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":saves");

        Long likeCount = likeStr != null
                ? Long.parseLong(likeStr)
                : interactionRepository.countByPropertyIdAndInteractionType(
                        propertyId,
                        UserPropertyInteraction.InteractionType.LIKE);

        Long saveCount = saveStr != null
                ? Long.parseLong(saveStr)
                : interactionRepository.countByPropertyIdAndInteractionType(
                        propertyId,
                        UserPropertyInteraction.InteractionType.SAVE);

        dto.setLikeCount(likeCount);
        dto.setSaveCount(saveCount);
        String viewStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":views");

        dto.setViewCount(viewStr != null ? Long.parseLong(viewStr) : 0L);
        String commentStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":comments");
        String contactStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":contacts");

        Long commentCount = commentStr != null
                ? Long.parseLong(commentStr)
                : commentRepository.countByPropertyIdAndStatus(
                        propertyId,
                        PropertyComment.Status.ACTIVE);

        dto.setCommentCount(commentCount);
        dto.setContactCount(
                contactStr != null
                        ? Long.parseLong(contactStr)
                        : 0L);

        dto.setIsLiked(false);
        dto.setIsSaved(false);

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
        Long propertyId = property.getId();

        String likeStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":likes");

        String saveStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":saves");

        String viewStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":views");
        String commentStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":comments");
        String contactStr = redisTemplate.opsForValue()
                .get("property:" + propertyId + ":contacts");
        Long likeCount = likeStr != null
                ? Long.parseLong(likeStr)
                : interactionRepository.countByPropertyIdAndInteractionType(
                        propertyId,
                        UserPropertyInteraction.InteractionType.LIKE);

        Long saveCount = saveStr != null
                ? Long.parseLong(saveStr)
                : interactionRepository.countByPropertyIdAndInteractionType(
                        propertyId,
                        UserPropertyInteraction.InteractionType.SAVE);

        dto.setLikeCount(likeCount);
        dto.setSaveCount(saveCount);
        dto.setViewCount(viewStr != null ? Long.parseLong(viewStr) : 0L);
        dto.setContactCount(
                contactStr != null
                        ? Long.parseLong(contactStr)
                        : 0L);
        Long commentCount = commentStr != null
                ? Long.parseLong(commentStr)
                : commentRepository.countByPropertyIdAndStatus(
                        propertyId,
                        PropertyComment.Status.ACTIVE);

        dto.setCommentCount(commentCount);
        dto.setLiked(false);
        dto.setSaved(false);
        return dto;
    }

    @Override
    public void contactProperty(
            Long userId,
            Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow();
        redisTemplate.opsForValue()
                .increment("property:" + propertyId + ":contacts");

        sendContactNotification(userId, property);
        if (!propertyContactRepository
                .existsByUserIdAndPropertyId(userId, propertyId)) {

            propertyContactRepository.save(
                    PropertyContact.builder()
                            .userId(userId)
                            .ownerId(property.getOwnerId())
                            .propertyId(propertyId)
                            .build());
        }
        recommendClient.track(
                TrackEventRequest.builder()
                        .userId(userId)
                        .itemId(propertyId)
                        .itemType(
                                property.getVideoUrl() != null
                                        ? "reel"
                                        : "property")
                        .action("CONTACT")
                        .watchTime(0.0)
                        .duration(1.0)
                        .price(
                                property.getPrice() != null
                                        ? property.getPrice().doubleValue()
                                        : 0.0)
                        .userBudget(
                                property.getPrice() != null
                                        ? property.getPrice().doubleValue()
                                        : 0.0)
                        .locationMatch(1)
                        .categoryMatch(1)
                        .district(property.getDistrict())
                        .build());
    }

    private void sendContactNotification(Long userId, Property property) {
        if (userId == null || property == null || property.getOwnerId() == null) {
            return;
        }

        if (property.getOwnerId().equals(userId)) {
            return;
        }

        try {
            NotificationEvent event = NotificationEvent.builder()
                    .receiverId(property.getOwnerId())
                    .title("Có người liên hệ")
                    .content("Có người vừa bấm liên hệ bài đăng của bạn")
                    .type("PROPERTY_CONTACT")
                    .referenceId(property.getId())
                    .build();
            System.out.println("SEND CONTACT NOTIFICATION = " + event);

            kafkaTemplate.send("notification-topic", event);

            System.out.println("SENT CONTACT NOTIFICATION OK");

            kafkaTemplate.send("notification-topic", event);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getMyProperties(
            Long ownerId,
            int page,
            int size,
            String status,
            String transactionType) {
        Pageable pageable = PageRequest.of(page, size);

        Property.Status statusEnum = null;
        Property.TransactionType transactionTypeEnum = null;

        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Property.Status.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        if (transactionType != null && !transactionType.isBlank()) {
            try {
                transactionTypeEnum = Property.TransactionType.valueOf(transactionType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        if (statusEnum != null && transactionTypeEnum != null) {
            return propertyRepository
                    .findByOwnerIdAndStatusAndTransactionTypeOrderByCreatedAtDesc(
                            ownerId,
                            statusEnum,
                            transactionTypeEnum,
                            pageable)
                    .map(this::mapToResponse);
        }

        if (statusEnum != null) {
            return propertyRepository
                    .findByOwnerIdAndStatusOrderByCreatedAtDesc(
                            ownerId,
                            statusEnum,
                            pageable)
                    .map(this::mapToResponse);
        }

        if (transactionTypeEnum != null) {
            return propertyRepository
                    .findByOwnerIdAndTransactionTypeOrderByCreatedAtDesc(
                            ownerId,
                            transactionTypeEnum,
                            pageable)
                    .map(this::mapToResponse);
        }

        return propertyRepository
                .findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponseDTO> getPublicPropertiesByProject(Long projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();

        return propertyRepository
                .findPublicByProjectId(
                        projectId,
                        Property.Status.ACTIVE,
                        now,
                        pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponseDTO> getAllActiveProperties() {
        return propertyRepository.findByStatus(Property.Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void sendQuotaSyncEvent(OwnerQuota quota, String reason) {
        try {
            PropertyQuotaSyncEvent event = PropertyQuotaSyncEvent.builder()
                    .userId(quota.getOwnerId())
                    .freePostsRemaining(quota.getFreePostsRemaining())
                    .role(quota.getRole())
                    .reason(reason)
                    .build();

            objectKafkaTemplate.send("property-quota-sync-topic", event);
            log.info("Đã gửi quota sync sang identity: {}", event);
        } catch (Exception e) {
            log.error("Không thể gửi quota sync sang identity: {}", e.getMessage(), e);
        }
    }
}