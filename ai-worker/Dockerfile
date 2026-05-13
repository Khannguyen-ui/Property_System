
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app


COPY . .

# build đúng module ai-worker
RUN mvn -pl ai-worker -am clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/ai-worker/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]