FROM gradle:8.11.1-jdk17 AS build

WORKDIR /workspace

# 루트 기준 전체 복사 (모듈만 복사하면 settings.gradle 등이 누락됨)
COPY ../ ./

RUN chmod +x ./gradlew

# 모듈만 타겟으로 빌드
RUN ./gradlew :module-service-user:bootJar --no-daemon -x test

# === 런타임 ===
FROM openjdk:17-alpine

WORKDIR /app

COPY --from=build /workspace/module-service-user/build/libs/*.jar ./app.jar

EXPOSE 5002
ENTRYPOINT ["java", "-jar", "./app.jar"]