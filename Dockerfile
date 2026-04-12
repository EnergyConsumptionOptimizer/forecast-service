FROM gradle:9.4.1-jdk21-jammy AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/

RUN git init && \
    git config user.email "build@local" && \
    git config user.name "build" && \
    git commit --allow-empty -m "init" --no-gpg-sign

RUN --mount=type=cache,id=forecast-gradle-cache,target=/home/gradle/.gradle/caches \
    gradle dependencies --no-daemon --quiet

COPY src/ ./src/

RUN --mount=type=cache,id=forecast-gradle-cache,target=/home/gradle/.gradle/caches \
    gradle jar --no-daemon --parallel -x test -x check

FROM eclipse-temurin:26-jre-jammy AS runtime
WORKDIR /app

RUN addgroup --system javauser && adduser --system --ingroup javauser javauser
USER javauser

COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]