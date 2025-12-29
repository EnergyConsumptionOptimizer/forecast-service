# Stage 1: Build with Gradle
FROM gradle:9.2.1-jdk21 AS builder
WORKDIR /usr/src/app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/
COPY . .

RUN gradle shadowJar --no-daemon

# Stage 2: Runtime with slim JDK
FROM eclipse-temurin:25
WORKDIR /app

COPY --from=builder /usr/src/app/build/libs/*-all.jar app.jar
EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]