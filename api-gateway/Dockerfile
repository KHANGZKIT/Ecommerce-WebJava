# ── Build stage ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 1. Copy ALL pom.xml files first — changes rarely, so this layer is cached
COPY pom.xml .
COPY api-gateway/pom.xml          api-gateway/
COPY user-service/pom.xml         user-service/
COPY product-service/pom.xml      product-service/
COPY order-service/pom.xml        order-service/
COPY payment-service/pom.xml      payment-service/

# 2. Resolve ALL dependencies offline and cache them in a BuildKit mount.
#    This layer is only invalidated when a pom.xml changes — NOT on src/ changes.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline -pl api-gateway -am -q

# 3. Copy only this service's source code
COPY api-gateway/src api-gateway/src

# 4. Build using the cached local Maven repo
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean package -pl api-gateway -am -DskipTests

# ── Run stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
