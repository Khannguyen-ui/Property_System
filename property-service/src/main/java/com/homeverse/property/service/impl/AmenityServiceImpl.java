package com.homeverse.property.service.impl;

import com.homeverse.common.exception.AppException;
import com.homeverse.common.exception.ErrorCode;
import com.homeverse.property.entity.Amenity;
import com.homeverse.property.repository.AmenityRepository;
import com.homeverse.property.service.AmenityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "amenities")
    public List<Amenity> getAll() {
        return amenityRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "amenities", allEntries = true)
    public Amenity create(Amenity amenity) {
        String cleanName = validateAndCleanName(amenity);

        Optional<Amenity> existingOpt =
                amenityRepository.findByNameIgnoreCaseIncludingDeleted(cleanName);

        if (existingOpt.isPresent()) {
            Amenity existing = existingOpt.get();

            if (Boolean.FALSE.equals(existing.getIsDeleted())) {
                log.warn("Tiện ích đã tồn tại: {}", cleanName);
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            existing.setName(cleanName);
            existing.setIcon(amenity.getIcon());
            existing.setIsDeleted(false);

            log.info("Khôi phục tiện ích đã xóa mềm: {}", cleanName);

            return amenityRepository.save(existing);
        }

        Amenity newAmenity = Amenity.builder()
                .name(cleanName)
                .icon(amenity.getIcon())
                .isDeleted(false)
                .build();

        return amenityRepository.save(newAmenity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "amenities", allEntries = true)
    public Amenity update(Integer id, Amenity dto) {
        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        String cleanName = validateAndCleanName(dto);

        Optional<Amenity> sameNameOpt =
                amenityRepository.findByNameIgnoreCaseIncludingDeleted(cleanName);

        if (sameNameOpt.isPresent()) {
            Amenity sameNameAmenity = sameNameOpt.get();

            boolean isDifferentAmenity = !sameNameAmenity.getId().equals(existingAmenity.getId());
            boolean isActive = Boolean.FALSE.equals(sameNameAmenity.getIsDeleted());

            if (isDifferentAmenity && isActive) {
                log.warn("Tên tiện ích đã tồn tại: {}", cleanName);
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        existingAmenity.setName(cleanName);
        existingAmenity.setIcon(dto.getIcon());

        return amenityRepository.save(existingAmenity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "amenities", allEntries = true)
    public void delete(Integer id) {
        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        amenityRepository.delete(existingAmenity);

        log.info("Đã xóa mềm tiện ích: {}", existingAmenity.getName());
    }

    private String validateAndCleanName(Amenity amenity) {
        if (amenity == null || amenity.getName() == null || amenity.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return amenity.getName().trim();
    }
}