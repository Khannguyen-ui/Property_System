# Sử dụng base image Java 17 (hoặc 21 tùy phiên bản bạn dùng) siêu nhẹ
FROM eclipse-temurin:17-jre-alpine

# Set thư mục làm việc trong container
WORKDIR /app

# Copy file .jar đã được build từ thư mục target vào container
# Ký tự * giúp tự động nhận diện tên file jar được sinh ra
COPY target/*.jar app.jar


EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]