# syntax=docker/dockerfile:1.10
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY core/mvnw core/mvnw
RUN sed -i 's/\r$//' mvnw core/mvnw && chmod +x mvnw

COPY pom.xml .
COPY infrastructure/pom.xml infrastructure/
COPY core/pom.xml core/
COPY utils/pom.xml utils/

ARG GITHUB_PACKAGES_USER
RUN --mount=type=secret,id=maven_settings,required=true \
    --mount=type=secret,id=github_packages_token,env=GITHUB_PACKAGES_TOKEN,required=true \
    ./mvnw -s /run/secrets/maven_settings dependency:go-offline -B -ntp

COPY infrastructure/src infrastructure/src
COPY core/src core/src
COPY utils/src utils/src
COPY deployment/docker/scripts/surreal/surreal-init.surql deployment/docker/scripts/surreal/surreal-init.surql

RUN --mount=type=secret,id=maven_settings,required=true \
    --mount=type=secret,id=github_packages_token,env=GITHUB_PACKAGES_TOKEN,required=true \
    ./mvnw -s /run/secrets/maven_settings clean verify -B -ntp

FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

RUN mkdir -p /app/logs && chown -R app:app /app

COPY --from=build --chown=app:app /app/infrastructure/target/infrastructure-*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV TZ=UTC

EXPOSE 8085

USER app

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=UTC -jar app.jar"]
