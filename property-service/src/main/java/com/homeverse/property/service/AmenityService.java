package com.homeverse.property.service;

import com.homeverse.property.entity.Amenity;

import java.util.List;

public interface AmenityService {

    List<Amenity> getAll();

    Amenity create(Amenity amenity);

    Amenity update(Integer id, Amenity dto);

    void delete(Integer id);

}