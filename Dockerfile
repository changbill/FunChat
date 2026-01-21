# 1. 실행 환경 설정 (Java 21)
# 프리티어 EC2의 부담을 줄이기 위해 가벼운 Alpine 리눅스 기반 이미지를 사용합니다.
FROM eclipse-temurin:21-jre-alpine

# 2. 작업 디렉토리 생성
WORKDIR /app

# 3. 빌드된 Jar 파일을 컨테이너 내부로 복사
# Jenkins가 ./gradlew bootJar를 통해 빌드한 결과물을 복사합니다.
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 4. 시간대 설정 (선택 사항)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 5. 실행 명령
# 프리티어 메모리를 아끼기 위해 JVM 옵션을 추가하는 것이 좋습니다.
ENTRYPOINT ["java", "-Xmx512M", "-Xms256M", "-jar", "app.jar"]