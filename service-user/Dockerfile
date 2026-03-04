# ---- Build Stage ----
FROM gradle:8.14-jdk17 AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle dependencies --no-daemon || true

COPY src ./src

RUN gradle bootJar --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
