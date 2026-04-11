package com.homeverse.property.service.impl;

import com.homeverse.common.dto.ApiResponse;
import com.homeverse.common.exception.AppException; // 🟢 Import từ common
import com.homeverse.common.exception.ErrorCode;    // 🟢 Import từ common
import com.homeverse.property.dto.request.PropertyRequestDTO;
import com.homeverse.property.dto.response.PropertyResponseDTO;
import com.homeverse.property.dto.response.UserResponseDTO;
import com.homeverse.property.entity.Property;
import com.homeverse.property.repository.PropertyRepository;
import com.homeverse.property.service.PropertyService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final ModelMapper modelMapper;
    private final RestClient identityRestClient;

    @PersistenceContext
    private EntityManager entityManager;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    // 🟢 HÀM HỖ TRỢ: Gọi chéo sang identity-service
    private UserResponseDTO getCurrentUserFromIdentity(String token) {
        try {
            ApiResponse<UserResponseDTO> response = identityRestClient.get()
                    .uri("/users/profile")
                    .header("Authorization", token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<UserResponseDTO>>() {});

            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("Lỗi liên kết identity-service: {}", e.getMessage());
            // Token hỏng hoặc service sập đều trả về 401
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    public PropertyResponseDTO createProperty(PropertyRequestDTO request, String token) {
        UserResponseDTO currentUser = getCurrentUserFromIdentity(token);
        // Đảm bảo có user mới cho tạo bài
        if (currentUser == null) throw new AppException(ErrorCode.UNAUTHENTICATED);

        Property property = modelMapper.map(request, Property.class);
        property.setLandlordId(currentUser.getId());
        property.setStatus(Property.Status.PENDING);

        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
            location.setSRID(4326);
            property.setLocation(location);
        }

        Property savedProperty = propertyRepository.save(property);
        return mapToResponse(savedProperty, currentUser);
    }

    @Override
    public PropertyResponseDTO updateProperty(Long id, PropertyRequestDTO request, String token) {
        UserResponseDTO currentUser = getCurrentUserFromIdentity(token);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND)); // 404: Không tìm thấy bài đăng

        boolean isOwner = property.getLandlordId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        // Kiểm tra quyền: Phải là chủ hoặc admin (403 Forbidden)
        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        modelMapper.map(request, property);

        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
            location.setSRID(4326);
            property.setLocation(location);
        }

        property.setUpdatedAt(LocalDateTime.now());
        Property updatedProperty = propertyRepository.save(property);

        return mapToResponse(updatedProperty, currentUser);
    }

    @Override
    public void deleteProperty(Long id, String token) {
        UserResponseDTO currentUser = getCurrentUserFromIdentity(token);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        boolean isOwner = property.getLandlordId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        propertyRepository.delete(property);
    }

    @Override
    public void updatePropertyStatus(Long id, String statusStr, String token) {
        UserResponseDTO currentUser = getCurrentUserFromIdentity(token);
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        boolean isOwner = property.getLandlordId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.NOT_PROPERTY_OWNER);
        }

        try {
            Property.Status newStatus = Property.Status.valueOf(statusStr.toUpperCase());

            // Logic chặn: Chỉ Admin mới được ACTIVE/REJECTED bài đang PENDING
            if ((newStatus == Property.Status.ACTIVE || newStatus == Property.Status.REJECTED)
                    && !isAdmin && property.getStatus() == Property.Status.PENDING) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            property.setStatus(newStatus);
            propertyRepository.save(property);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // 400: Trạng thái truyền vào sai
        }
    }

    @Override
    public Page<PropertyResponseDTO> getPendingProperties(int page, int size, String token) {
        UserResponseDTO currentUser = getCurrentUserFromIdentity(token);

        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return propertyRepository.findByStatus(Property.Status.PENDING, pageable)
                .map(prop -> mapToResponse(prop, null));
    }

    @Override
    public PropertyResponseDTO getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        return mapToResponse(property, null);
    }

    @Override
    public Page<PropertyResponseDTO> getAllProperties(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return propertyRepository.findAll(pageable)
                .map(prop -> mapToResponse(prop, null));
    }

    @Override
    public Page<PropertyResponseDTO> getPropertiesByLandlord(Long landlordId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return propertyRepository.findByLandlordId(landlordId, pageable)
                .map(prop -> mapToResponse(prop, null));
    }

    @Override
    public Page<PropertyResponseDTO> searchNearbyAdvanced(
            Double lat, Double lng, Double radius,
            String keyword, String type,
            BigDecimal minPrice, BigDecimal maxPrice,
            Double minArea, Double maxArea,
            List<Integer> bedroomList, List<Integer> bathroomList,
            List<String> directionList, String furniture,
            Pageable pageable) {

        try {
            // 1. Xây dựng SQL (Giữ nguyên logic cực xịn)
            StringBuilder sql = new StringBuilder("SELECT p.* FROM properties p " +
                    "WHERE p.status = 'ACTIVE' AND p.expiration_date >= NOW() ");

            if (lat != null && lng != null && radius != null && radius > 0) {
                sql.append(" AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :radius / 111319) ");
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND p.search_vector @@ plainto_tsquery('simple', :keyword) ");
            }

            if (type != null && !type.equals("ALL") && !type.isEmpty()) sql.append(" AND p.rental_type = :type ");
            if (minPrice != null) sql.append(" AND p.price >= :minPrice ");
            if (maxPrice != null) sql.append(" AND p.price <= :maxPrice ");
            if (minArea != null) sql.append(" AND p.area >= :minArea ");
            if (maxArea != null) sql.append(" AND p.area <= :maxArea ");
            if (bedroomList != null && !bedroomList.isEmpty()) sql.append(" AND p.num_bedrooms IN (:bedroomList) ");
            if (bathroomList != null && !bathroomList.isEmpty()) sql.append(" AND p.num_bathrooms IN (:bathroomList) ");
            if (directionList != null && !directionList.isEmpty()) sql.append(" AND p.direction IN (:directionList) ");
            if (furniture != null && !furniture.trim().isEmpty()) sql.append(" AND p.furniture_status = :furniture ");

            // 2. Query Count
            Query countQuery = entityManager.createNativeQuery("SELECT count(*) FROM (" + sql.toString() + ") as tmp");
            setParameters(countQuery, lat, lng, radius, keyword, type, minPrice, maxPrice, minArea, maxArea, bedroomList, bathroomList, directionList, furniture);
            int totalRows = ((Number) countQuery.getSingleResult()).intValue();

            // 3. Query Data & Sort
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" ORDER BY ts_rank(p.search_vector, plainto_tsquery('simple', :keyword)) DESC, ");
            } else if (lat != null && lng != null) {
                sql.append(" ORDER BY ST_Distance(p.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) ASC, ");
            } else {
                sql.append(" ORDER BY ");
            }

            sql.append(" COALESCE(p.priority_level, 0) DESC, p.last_pushed_at DESC NULLS LAST, p.created_at DESC ");

            Query query = entityManager.createNativeQuery(sql.toString(), Property.class);
            setParameters(query, lat, lng, radius, keyword, type, minPrice, maxPrice, minArea, maxArea, bedroomList, bathroomList, directionList, furniture);

            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());

            List<Property> properties = query.getResultList();

            return new PageImpl<>(
                    properties.stream().map(prop -> mapToResponse(prop, null)).collect(Collectors.toList()),
                    pageable,
                    totalRows);

        } catch (Exception e) {
            log.error("Lỗi search nâng cao: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // 500: Lỗi DB/GIS
        }
    }

    private void setParameters(Query query, Double lat, Double lng, Double radius, String keyword, String type,
                               BigDecimal minPrice, BigDecimal maxPrice, Double minArea, Double maxArea,
                               List<Integer> bedroomList, List<Integer> bathroomList, List<String> directionList, String furniture) {
        if (lat != null && lng != null && radius != null && radius > 0) {
            query.setParameter("lat", lat);
            query.setParameter("lng", lng);
            query.setParameter("radius", radius);
        }
        if (keyword != null && !keyword.trim().isEmpty()) query.setParameter("keyword", keyword.trim());
        if (type != null && !type.equals("ALL") && !type.isEmpty()) query.setParameter("type", type);
        if (minPrice != null) query.setParameter("minPrice", minPrice);
        if (maxPrice != null) query.setParameter("maxPrice", maxPrice);
        if (minArea != null) query.setParameter("minArea", minArea);
        if (maxArea != null) query.setParameter("maxArea", maxArea);
        if (bedroomList != null && !bedroomList.isEmpty()) query.setParameter("bedroomList", bedroomList);
        if (bathroomList != null && !bathroomList.isEmpty()) query.setParameter("bathroomList", bathroomList);
        if (directionList != null && !directionList.isEmpty()) query.setParameter("directionList", directionList);
        if (furniture != null && !furniture.trim().isEmpty()) query.setParameter("furniture", furniture);
    }

    private PropertyResponseDTO mapToResponse(Property property, UserResponseDTO landlordInfo) {
        PropertyResponseDTO response = modelMapper.map(property, PropertyResponseDTO.class);

        if (property.getLocation() != null) {
            response.setLongitude(property.getLocation().getX());
            response.setLatitude(property.getLocation().getY());
        }

        if (landlordInfo != null) {
            response.setLandlordInfo(landlordInfo);
        }
        return response;
    }
}