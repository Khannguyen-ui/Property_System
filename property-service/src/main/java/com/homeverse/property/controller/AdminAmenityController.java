package com.homeverse.property.controller;

import com.homeverse.property.entity.Amenity;
import com.homeverse.property.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/amenities")
@RequiredArgsConstructor
public class AdminAmenityController {

    private final AmenityService amenityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Amenity> createAmenity(@RequestBody Amenity amenity) {
        return ResponseEntity.ok(amenityService.create(amenity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Amenity> updateAmenity(
            @PathVariable Integer id,
            @RequestBody Amenity amenity) {
        return ResponseEntity.ok(amenityService.update(id, amenity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAmenity(@PathVariable Integer id) {
        amenityService.delete(id);
        return ResponseEntity.ok("Đã xóa tiện ích thành công.");
    }
}