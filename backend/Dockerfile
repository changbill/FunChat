# 1단계: 빌드 전용 환경 (Docker Hub에서 공식 Gradle 이미지를 가져옴)
FROM gradle:jdk21-corretto-al2023 AS build
WORKDIR /app

# 소스 코드 복사
COPY . .

# 빌드 실행 (여기서 Jar 파일이 생성됨)
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

# 2단계: 실행 전용 환경 (가벼운 Alpine 이미지)
FROM eclipse-temurin:21.0.9_10-jdk-ubi9-minimal

WORKDIR /app

# 1단계에서 빌드된 Jar 파일만 쏙 가져옴
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]