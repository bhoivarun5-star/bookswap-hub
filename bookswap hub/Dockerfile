# ── Stage 1: Build (vulnerabilities here don't affect final image) ───────────
FROM maven:3.9.6-eclipse-temurin-17-jammy AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress \
    && rm -rf ~/.m2

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

# Patch all OS packages to eliminate known CVEs
RUN apt-get update && apt-get upgrade -y && apt-get clean && rm -rf /var/lib/apt/lists/*

# Create uploads directory and non-root user
RUN mkdir -p /app/uploads \
    && groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

# Memory-optimised JVM flags for Render free tier (512MB RAM)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseSerialGC", \
  "-Xss512k", \
  "-jar", "app.jar"]
