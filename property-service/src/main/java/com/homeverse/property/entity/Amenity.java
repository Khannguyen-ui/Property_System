package com.homeverse.property.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.io.Serializable;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@SQLDelete(sql = "UPDATE amenities SET is_deleted = true WHERE id = ?")

@SQLRestriction("is_deleted = false")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(nullable = false, unique = true)
    private String name;

    private String icon;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;


}
