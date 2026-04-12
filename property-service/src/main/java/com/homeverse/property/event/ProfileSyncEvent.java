package com.homeverse.property.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSyncEvent {
    private Long userId;     // Trùng với id bên OwnerProfile
    private String slug;     // Chuỗi chống IDOR
    private String fullName; // Tên hiển thị
    private String avatar;
    private String action;
}