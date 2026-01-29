# 빌드 환경
FROM gradle:jdk21-ubi AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle clean bootJar -x test

# 실행 환경
FROM eclipse-temurin:21.0.9_10-jre-alpine-3.23
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]