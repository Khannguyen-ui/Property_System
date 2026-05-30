package com.homeverse.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  // Quan trọng để Jackson deserialization không bị lỗi
@AllArgsConstructor // Cần thiết để @Builder hoạt động
public class UserSummaryDTO { // Thêm public để dùng được ở các package khác
    private String fullName;
    private String avatarUrl;
}