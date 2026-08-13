# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system clickkart && adduser --system --ingroup clickkart clickkart
WORKDIR /app
COPY --from=build /workspace/target/clickkart-notification-service.jar app.jar
# logback-spring.xml writes to ./logs - this directory must exist and be writable by the
# non-root user before the JVM starts.
RUN mkdir -p /app/logs && chown -R clickkart:clickkart /app
USER clickkart

ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8082

# Probes /actuator/health/readiness, not /actuator/health - deliberately the same endpoint the
# Kubernetes readiness probe uses, so "healthy" means the same thing locally and in a cluster.
#
# It matters here specifically: spring-boot-starter-mail auto-configures a MailHealthIndicator
# that opens an SMTP connection, and it is part of the composite /actuator/health. Probing that
# made an unreachable mail server mark the whole container unhealthy, even though the service was
# fully able to accept requests and record them as FAILED. In a cluster that would be worse than
# useless - a single SMTP outage would fail every replica's readiness at once and pull the entire
# service out of its Service pool, turning a degraded feature into a total outage.
#
# The readiness group (readinessState,db - see clickkart-config-repository) already excludes mail
# for exactly that reason; this aligns the container healthcheck with it. Mail status is still
# reported under /actuator/health for diagnostics.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT:-8082}/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
