# 1단계: 빌드 환경 (Java 21 JDK)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사
COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 소스 코드 복사 및 애플리케이션 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행 환경 (경량 Java 21 JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/desserting-backend-*.jar app.jar

ENV TZ=Asia/Seoul
EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
