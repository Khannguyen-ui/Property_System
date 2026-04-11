package com.homeverse.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // === NHÓM LỖI HỆ THỐNG CHUNG (9xxx) ===
    UNCATEGORIZED_EXCEPTION(9999, "Hệ thống đang bận hoặc gặp sự cố, vui lòng thử lại sau!", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1006, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    // === NHÓM LỖI IDENTITY SERVICE (1xxx) ===
    USER_EXISTED(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1004, "Chưa xác thực, vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "Bạn không có quyền truy cập tài nguyên này", HttpStatus.FORBIDDEN),

    PASSWORD_INCORRECT(1101, "Mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1102, "Tài khoản hiện đang bị khóa", HttpStatus.FORBIDDEN),
    KYC_NOT_VERIFIED(1103, "Tài khoản chưa được xác minh danh tính (KYC)", HttpStatus.FORBIDDEN),
    KYC_ALREADY_SUBMITTED(1104, "Hồ sơ KYC đã được gửi và đang chờ duyệt", HttpStatus.BAD_REQUEST),
    TOKEN_INVALID(1011, "Mã xác thực không hợp lệ", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(1012, "Mã xác thực đã hết hạn", HttpStatus.BAD_REQUEST),

    // === NHÓM LỖI PROPERTY SERVICE (2xxx) ===
    PROPERTY_NOT_FOUND(2001, "Không tìm thấy bài đăng bất động sản", HttpStatus.NOT_FOUND),
    NOT_PROPERTY_OWNER(2002, "Bạn không phải là chủ sở hữu của bài đăng này", HttpStatus.FORBIDDEN),
    INVALID_PROPERTY_STATUS(2003, "Trạng thái bất động sản không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    PROPERTY_EXPIRED(2004, "Bài đăng này đã hết hạn hiển thị", HttpStatus.GONE),
    LOCATION_REQUIRED(2005, "Tọa độ địa lý (Lat/Lng) là bắt buộc cho bài đăng", HttpStatus.BAD_REQUEST),
    SEARCH_QUERY_ERROR(2006, "Lỗi trong quá trình tìm kiếm nâng cao", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}