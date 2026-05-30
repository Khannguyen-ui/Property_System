
FROM eclipse-temurin:17-jre-alpine

# Thư mục làm việc trong container
WORKDIR /app

# Copy file jar từ thư mục target vào container
COPY target/*.jar app.jar

# Mở cổng 8083
EXPOSE 8084

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]