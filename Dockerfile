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
COPY clients/java/pom.xml clients/java/
COPY clients/java/messageucolab-client-api/pom.xml clients/java/messageucolab-client-api/
COPY clients/java/messageucolab-spring-boot-starter/pom.xml clients/java/messageucolab-spring-boot-starter/

ARG GITHUB_PACKAGES_USER
RUN --mount=type=secret,id=maven_settings,required=true \
    --mount=type=secret,id=github_packages_token,env=GITHUB_PACKAGES_TOKEN,required=true \
    ./mvnw -s /run/secrets/maven_settings dependency:go-offline -B -ntp

COPY infrastructure/src infrastructure/src
COPY core/src core/src
COPY utils/src utils/src
COPY clients/java/messageucolab-client-api/src clients/java/messageucolab-client-api/src
COPY clients/java/messageucolab-spring-boot-starter/src clients/java/messageucolab-spring-boot-starter/src
COPY deployment/docker/scripts/surreal/surreal-init.surql deployment/docker/scripts/surreal/surreal-init.surql

RUN --mount=type=secret,id=maven_settings,required=true \
    --mount=type=secret,id=github_packages_token,env=GITHUB_PACKAGES_TOKEN,required=true \
    ./mvnw -s /run/secrets/maven_settings clean verify -B -ntp

# El jar se extrae por capas: las dependencias cambian poco y quedan en capas que Docker reutiliza.
# El driver de SurrealDB trae su motor nativo para 12 plataformas (~300 MB); solo se conserva el de
# la arquitectura de la imagen (TARGETARCH lo define Docker: amd64 o arm64).
ARG TARGETARCH
RUN apt-get update \
    && apt-get install --no-install-recommends -y zip unzip \
    && rm -rf /var/lib/apt/lists/* \
    && cp infrastructure/target/infrastructure-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination extracted \
    && mkdir -p extracted/dependencies extracted/spring-boot-loader \
        extracted/snapshot-dependencies extracted/application \
    && if [ "$TARGETARCH" = "arm64" ]; then native=linux_arm64; else native=linux_64; fi \
    && surrealdb=$(find extracted -name 'surrealdb-*.jar') \
    && zip -q "$surrealdb" -d $(unzip -Z1 "$surrealdb" 'natives/*' | grep -v "^natives/$native/" | grep -v '/$') \
    && unzip -Z1 "$surrealdb" 'natives/*' | grep -q "^natives/$native/"

FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

RUN mkdir -p /app/logs && chown -R app:app /app

COPY --from=build --chown=app:app /app/extracted/dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/application/ ./

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV TZ=UTC

EXPOSE 8085

USER app

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=UTC -jar app.jar"]
