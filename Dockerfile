# ---- build stage ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# gradle wrapper & build scripts
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# sources
COPY src src

# build jar (skip tests for faster build on server)
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]