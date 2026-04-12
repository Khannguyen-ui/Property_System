# Bước 1: Dùng một cái máy ảo nhẹ có sẵn Java 21
FROM eclipse-temurin:17-jre-alpine

# Bước 2: Tạo thư mục làm việc bên trong container
WORKDIR /app

# Bước 3: Copy file JAR đã build từ máy Ân vào trong container
# (Lưu ý: Tên file JAR phải khớp với tên trong mục target sau khi Ân chạy mvn clean install)
COPY target/payment-service-1.0.0.jar app.jar
EXPOSE 8086
# Bước 4: Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]