# Bước 1: Sử dụng JRE 21 bản Alpine để chạy ứng dụng (nhẹ và bảo mật)
FROM eclipse-temurin:17-jre-alpine

# Bước 2: Thiết lập thư mục làm việc bên trong container
WORKDIR /app

# Bước 3: Copy file JAR đã build từ máy sếp vào container
# Lưu ý: Sếp cần chạy 'mvn clean package' trước khi build docker
COPY target/*.jar app.jar

# Bước 4: Mở cổng 8085 (Cổng anh em mình đã quy hoạch cho Notification)
EXPOSE 8085

# Bước 5: Lệnh để khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]