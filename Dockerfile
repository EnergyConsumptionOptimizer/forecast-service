# Stage 1: Build with Gradle
FROM gradle:9.3.1-jdk21-jammy AS build
WORKDIR /usr/src/app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/
COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    gradle jar --parallel

# Stage 2: Runtime with slim JDK
FROM eclipse-temurin:22-jre-jammy
WORKDIR /app

COPY --from=build /usr/src/app/build/libs/app.jar app.jar
EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]