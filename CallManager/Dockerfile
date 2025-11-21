# 1) Build Stage ----------------------------------------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY . .

RUN gradle clean build -x test

# 2) Runtime Stage --------------------------------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# JAR 파일 이름 자동 탐색 (build/libs/*.jar)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
