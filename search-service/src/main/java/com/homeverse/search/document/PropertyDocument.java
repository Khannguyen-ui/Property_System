package com.homeverse.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "#{@environment.getProperty('elasticsearch.index.properties', 'properties_v1')}", createIndex = false)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long projectId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;


    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String address;
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword),}
    )
    private String province;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)} // Để Filter và Vẽ biểu đồ
    )
    private String street;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String ward;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String district;

    @Field(type = FieldType.Double)
    private BigDecimal price;
    @Field(type = FieldType.Double)
    private BigDecimal pricePerSqm;

    @Field(type = FieldType.Double)
    private Double area;

    @Field(type = FieldType.Keyword)
    private String propertyType;

    @Field(type = FieldType.Keyword)
    private String transactionType;

    @Field(type = FieldType.Keyword)
    private String legalDocumentType;

    @Field(type = FieldType.Integer)
    private Integer bedrooms;

    @Field(type = FieldType.Integer)
    private Integer bathrooms;

    @Field(type = FieldType.Boolean)
    private Boolean hasBalcony;

    @Field(type = FieldType.Keyword)
    private List<String> amenities;
    @Field(type = FieldType.Keyword)
    private String furnishingStatus;

    @Field(type = FieldType.Keyword)
    private String availabilityStatus;

    @Field(type = FieldType.Keyword)
    private String electricityPrice;

    @Field(type = FieldType.Keyword)
    private String waterPrice;

    @Field(type = FieldType.Keyword)
    private String internetPrice;


    @Field(type = FieldType.Keyword)
    private List<String> images;


    @Field(type = FieldType.Integer)
    private Integer capacity;


    @Field(type = FieldType.Keyword)
    private String status;


    @Field(type = FieldType.Long)
    private Long ownerId;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}