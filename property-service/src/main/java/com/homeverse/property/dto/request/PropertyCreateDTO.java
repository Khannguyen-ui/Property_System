    package com.homeverse.property.dto.request;

    import jakarta.validation.constraints.Min;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import lombok.Data;
    import java.math.BigDecimal;
    import java.util.List;

    @Data
    public class PropertyCreateDTO {
        private Long projectId;

        @NotBlank(message = "Loại giao dịch không được trống")
        private String transactionType;

        @NotBlank(message = "Tiêu đề không được trống")
        private String title;

        private String description;

        @NotNull(message = "Giá không được trống")
        private BigDecimal price;

        @NotNull(message = "Diện tích không được trống")
        private Double area;

        @NotBlank(message = "Địa chỉ không được trống")
        private String address;

        @NotBlank(message = "Tỉnh/Thành phố không được để trống")
        private String province;
        @NotBlank(message = "Đường/Phố không được trống")
        private String street;

        @NotBlank(message = "Phường/Xã không được trống")
        private String ward;

        @NotBlank(message = "Quận/Huyện không được trống")
        private String district;

        @NotNull(message = "Vĩ độ (Lat) không được trống")
        private Double latitude;

        @NotNull(message = "Kinh độ (Long) không được trống")
        private Double longitude;

        @NotNull(message = "Phải chọn loại hình bất động sản")
        private String propertyType;


        private String ownerNameSnapshot;
        private String ownerAvatarSnapshot;
        private String ownerSlugSnapshot;

        private Integer capacity;

        private Integer validityDays;

        private List<String> images;
        private String videoUrl;
        private List<String> amenities;
        @Min(value = 0, message = "Số phòng ngủ không được âm")
        private Integer bedrooms;

        @Min(value = 0, message = "Số phòng vệ sinh không được âm")
        private Integer bathrooms;

        private Boolean hasBalcony;


        private String furnishingStatus;
        private String availabilityStatus;
        private String electricityPrice;
        private String waterPrice;
        private String internetPrice;
        private String legalDocumentType;
    }