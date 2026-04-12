# Bước 1: Dùng máy ảo Java 21 (sửa lại số 17 thành 21 để khớp với comment)
FROM eclipse-temurin:21-jre-alpine

# Bước 2: Tạo thư mục làm việc bên trong container
WORKDIR /app

# Bước 3: Copy file JAR đã build.
# Dùng dấu * để Docker tự lấy file JAR bất kỳ trong target,
COPY target/*.jar app.jar

EXPOSE 8087

# Bước 4: Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]