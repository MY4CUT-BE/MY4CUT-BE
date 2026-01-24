FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Gradle로 빌드된 jar를 넣어서 실행만 함
COPY build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
