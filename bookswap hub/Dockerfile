# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Patch all Alpine packages to eliminate known CVEs
RUN apk update && apk upgrade --no-cache && rm -rf /var/cache/apk/*

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress \
    && rm -rf ~/.m2

# ── Stage 2: Runtime (Alpine ~130MB vs Ubuntu ~270MB) ────────────────────────
FROM eclipse-temurin:17.0.13_11-jre-alpine
WORKDIR /app

# Patch all Alpine packages to eliminate known CVEs
RUN apk update && apk upgrade --no-cache && rm -rf /var/cache/apk/*

# Create uploads directory
RUN mkdir -p /app/uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Memory-optimised JVM flags for Render free tier (512MB RAM)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseSerialGC", \
  "-Xss512k", \
  "-jar", "app.jar"]
