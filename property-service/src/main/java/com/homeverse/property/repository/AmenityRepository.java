package com.homeverse.property.repository;

import com.homeverse.property.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {


    boolean existsByName(String name);
    @Query(value = "SELECT EXISTS(SELECT 1 FROM amenities WHERE LOWER(name) = LOWER(:name))", nativeQuery = true)
    boolean existsInDatabaseEvenIfDeleted(@Param("name") String name);
    boolean existsByNameIgnoreCase(String name);
    @Query("SELECT COUNT(a) FROM Amenity a WHERE a.name IN :names")
    long countByNames(@Param("names") List<String> names);
}