# syntax=docker/dockerfile:1
# =============================================================================
# Stage 1 - build: full Maven + JDK image, compiles and packages the JAR
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Layer caching: copy ONLY pom.xml first and download dependencies.
# This layer is reused on every build until pom.xml changes, so a code-only
# change does not re-download the whole Maven repository.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source code and build (tests already ran in the Jenkins Test stage)
COPY src ./src
RUN mvn -B -q package -DskipTests

# =============================================================================
# Stage 2 - runtime: small JRE-only Alpine image, contains just the JAR
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL org.opencontainers.image.title="calculator-api" \
      org.opencontainers.image.description="Calculator REST API (Spring Boot) - CSE 816 SPE Mini Project" \
      org.opencontainers.image.source="https://github.com/Aishrma/Calculator_APP"

# Security: create and use an unprivileged user instead of root
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Copy only the executable JAR from the build stage - no source, no Maven, no JDK
COPY --from=build --chown=spring:spring /workspace/target/calculator-api.jar app.jar

USER spring

# Let the JVM size its heap from the container memory limit
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080

EXPOSE 8080

# Docker-level health check (busybox wget is available in Alpine)
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
