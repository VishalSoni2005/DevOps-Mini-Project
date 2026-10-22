# ============================================
# Multi-Stage Dockerfile
# Supply Chain Management Application
# ============================================

# ---------- Stage 1: Build with Maven ----------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine

# Add labels for container identification
LABEL maintainer="supplychain-team"
LABEL application="supply-chain-management"
LABEL version="1.0.0"

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create log directory
RUN mkdir -p /var/log/supply-chain && \
    chown -R appuser:appgroup /var/log/supply-chain

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set default log directory in container
ENV LOG_DIR=/var/log/supply-chain

# JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
