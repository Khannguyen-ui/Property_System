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
        String cleanName = amenity.getName().trim();

        if (amenityRepository.existsByNameIgnoreCase(cleanName)) {
            log.warn("Tiện ích đã tồn tại: {}", cleanName);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        amenity.setName(cleanName);
        amenity.setIsDeleted(false);
        return amenityRepository.save(amenity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "amenities", allEntries = true)
    public Amenity update(Integer id, Amenity dto) {
        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        String cleanName = dto.getName().trim();

        if (!existingAmenity.getName().equalsIgnoreCase(cleanName)
                && amenityRepository.existsByNameIgnoreCase(cleanName)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
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
}