# Stage 1: Build Application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper files and build configs
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Cache dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code and build jar
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Run Application
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
