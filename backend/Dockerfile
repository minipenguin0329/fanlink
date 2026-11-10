# ---- build stage ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy build files first so Docker layer caching skips the dependency
# download step unless build.gradle/settings.gradle actually changed.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --shell /usr/sbin/nologin spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

COPY --from=build /app/build/libs/*.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
