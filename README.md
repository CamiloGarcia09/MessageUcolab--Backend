# MessageUcoLab

A Spring Boot application for message handling and processing.

The current persistence schema is supported only for new SurrealDB installations. Existing database
volumes are not migrated; recreate the SurrealDB volume before starting a deployment with this schema.
The domain no longer persists `parameter` or `represent_parameter`; technical configuration parameters
continue to be read through `CatalogParameterPort` from `parameter.properties`.

## Azure CI/CD

Microsoft-hosted CI and protected Azure deployment run as separate Azure DevOps pipelines. The Azure for Students deployment, Doppler setup, infrastructure provisioning, and operations are documented in [`docs/azure-deployment.md`](docs/azure-deployment.md).

## Prerequisites

- JDK 25 (configure `JAVA_HOME` and your IDE project SDK)
- Maven 3.9.9 via the committed Maven Wrapper (`./mvnw`, or `.\mvnw.cmd` on Windows)
- Required infrastructure:
  - MongoDB (version 4.4 or higher)
  - Redis (version 6.0 or higher)
  - PostgreSQL (version 12 or higher)
  - Apache Pulsar (version 3.2.2)
  - Apache Kafka (version 7.0.0 or higher)
  - Zookeeper (version 7.0.0 or higher)
  - KSQLDB (version 0.20.0 or higher)
  - Debezium Connect (version 1.9 or higher)
  - Kong API Gateway (version 3.5 or higher)
  - Observability (optional but recommended):
    - Grafana (for metrics and logs visualization)
    - Loki (for log storage and querying)
    - OpenTelemetry Collector (for telemetry collection)
    - Prometheus (for metrics storage and querying)

## Java 25 toolchain

Local environment variables and secrets are managed in **Doppler**, not a required `.env`
file. Use `doppler run --project messageucolab --config dev -- docker compose up -d`
from `deployment/docker` and `doppler run --project messageucolab --config dev -- ./mvnw spring-boot:run -pl infrastructure`
from the repository root. The local configuration is `dev`, not the deployment's `dev_azure`.
Do not print secrets or export them into repository files. If the CLI has no local
binding, verify the Doppler project/configuration rather than asking for secret values.

All modules inherit Java 25 (`--release 25`) from the root `pom.xml`. Spring Boot 3.5.16
manages the compiler, Lombok, Mockito/Byte Buddy and Jackson versions; JaCoCo 0.8.15
supports the resulting bytecode. Preview features and Byte Buddy's experimental mode
are not required.

Before building, check `java -version` and `./mvnw -version` (Windows: `.\mvnw.cmd -version`).
On Linux/macOS, run `chmod +x mvnw` once if the checkout did not preserve executable permissions.
Both must use JDK 25. Then run `./mvnw clean verify` from the repository root.
Docker and Azure CI use Temurin 25 and the same Maven Wrapper configuration.
The Docker build runs `clean verify`, including coverage checks, and copies the SurrealDB
seed into the build stage so its static contract tests are not skipped. The seed is not
executed by the image build and is not included in the runtime image.

### Windows and IntelliJ IDEA

Install JDK 25 outside temporary directories and set the user `JAVA_HOME` to that installation.
Restart terminals and the IDE after changing environment variables. If Java 17 is still first
in the system PATH, changing only the user PATH does not override it; reordering the system
PATH requires administrator permissions. For the current PowerShell session, use:

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
```

Use an IntelliJ IDEA version with Java 25 support (the 2024.1 release predates it).
Register the installed JDK in **Project Structure > SDKs**, select it as the **Project SDK**,
and set the Maven importer and runner JDK to the project JDK. Reload the Maven project;
the language level and bytecode target must be 25, without preview features.

Validate the container in a non-production environment with Redis, SurrealDB and Pulsar
before promoting it: check health, message/token operations, translation, SSE, OpenAPI
and metrics. A successful unit-test build does not replace these integration checks.
Keep the previous image tag for rollback: Java 25 bytecode cannot run on Java 17, so
rollback means restoring the previous complete image, not only switching the JVM.

## Project Structure

The project is organized into multiple modules:

- `Core`: Main business logic and domain models
- `Infrastructure`: Infrastructure layer components (contains the main application class)
- `Utils`: Utility classes and helper functions

## Infrastructure Configuration

### MongoDB

1. Install MongoDB (version 4.4 or higher)
2. Create a database named `messageuco`
3. Configure credentials in environment variables or in `application.properties`

### Redis

1. Install Redis (version 6.0 or higher)
2. Configure credentials in environment variables or in `application.properties`

### PostgreSQL

1. Install PostgreSQL (version 12 or higher)
2. Create a database named `ucolab`
3. Create a user `crosswords` with password `crosswords` (or configure according to your needs)
4. Assign permissions to the user for the database
5. If you're configuring PostgreSQL manually (without Docker), run the SQL script located at `deployment/docker/scripts/init.sql` to create the necessary tables
   > Note: If you use the provided docker-compose, this script will be executed automatically when the PostgreSQL container starts

### Apache Kafka

1. Install Apache Kafka (version 7.0.0 or higher)
2. Install Zookeeper (version 7.0.0 or higher)
3. Configure the following topics:
   - `connect-configs`
   - `connect-offsets`
   - `connect-status`

### KSQLDB

1. Install KSQLDB Server (version 0.20.0 or higher)
2. Configure to connect to Kafka cluster
3. KSQLDB Server listens on port 8088

### Debezium Connect

1. Install Debezium Connect (version 1.9 or higher)
2. Configure to connect to:
   - Kafka cluster
   - PostgreSQL database
   - MongoDB database
3. Debezium Connect listens on port 8083

### Kong API Gateway

1. Install Kong Gateway (version 3.5 or higher)
2. Configure in DB-less mode
3. Use the configuration in `deployment/docker/kong.yaml`
4. Kong Gateway exposes:
   - API Gateway on port 8000
   - Admin API on port 8001

### Apache Pulsar

1. Install Apache Pulsar (version 3.2.2)
2. Configure the service according to the official documentation
3. Create the topic `list-messageModel-topic`

### Observability Infrastructure

#### Grafana

1. Install Grafana (or use Docker)
2. Configure to connect to Prometheus and Loki
3. Import predefined dashboards for Spring Boot application monitoring

#### Loki

1. Install Loki (or use Docker)
2. Configure to receive logs from the application
3. The application is configured to send logs to Loki at `http://localhost:3100/loki/api/v1/push`

#### OpenTelemetry Collector

1. Install OpenTelemetry Collector (or use Docker)
2. Use the configuration in `deployment/docker/otel-collector-config.yaml`
3. The collector listens on ports 4317 (gRPC) and 4318 (HTTP)

#### Prometheus

1. Install Prometheus (or use Docker)
2. Use the configuration in `deployment/docker/prometheus.yml`
3. Prometheus listens on port 9090
4. The application exposes metrics at the `/actuator/prometheus` endpoint

## Environment Variables

Configure the following environment variables before running the application:

```text
# MongoDB
MONGODBHOST=localhost
MONGODBPORT=27017
MONGODBUSER=your_mongodb_user
MONGOPASSWORD=your_mongodb_password
MONGODBDATABASE=messageuco

# Redis
REDISHOST=localhost
REDISPORT=6379
REDISPASSWORD=your_redis_password
REDISDATABASE=0

# PostgreSQL
POSTGRESHOST=localhost
POSTGRESPORT=5435
POSTGRESDATABASE=crs-crossword-db
POSTGRESUSERNAME=your_postgres_user
POSTGRESPASSWORD=your_postgres_password

# Azure Key Vault
AZURE_KEYVAULT_UCOLAB_ENDPOINT=your_azure_keyvault_endpoint

# Doppler
DOPPLERTOKEN=your_doppler_token

# Pulsar
PULSARURL=pulsar://pulsar-standalone:6650
PULSARTOPICNAME=crossword-topic
```

## Building the Project

### Local Build

1. Clone the repository:

```bash
git clone https://github.com/devBorisG/MessageUcoLab.git
cd MessageUcoLab
```

2. Build the project using Maven:

```bash
./mvnw clean install
```

## Running the Application

To run the application, make sure all infrastructure dependencies are running (MongoDB, Redis, PostgreSQL, Kafka), then use Maven:

```bash
./mvnw spring-boot:run -pl infrastructure
```

The application will start on port 8085 by default.

## API Documentation

API documentation is available at:

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

## API Endpoints

### Example Endpoints

1. Health check:

```bash
# Direct access
curl http://localhost:8085/actuator/health

# Through API Gateway
curl http://localhost:8000/actuator/health
```

2. Message endpoints:

```bash
# Get messages for the authenticated environment
# Direct access
curl http://localhost:8085/messageucolab/v1/messages

# Through API Gateway
curl http://localhost:8000/messageucolab/v1/messages

# Get a specific message by code
# Direct access
curl "http://localhost:8085/messageucolab/v1/messages?code={messageCode}"

# Through API Gateway
curl "http://localhost:8000/messageucolab/v1/messages?code={messageCode}"

# Translate a specific message
# Direct access
curl -X POST "http://localhost:8085/messageucolab/v1/messages/{messageCode}/translations" -H "Content-Type: application/json" -d '{"sourceLanguage":"auto","targetLanguage":"en"}'

# Through API Gateway
curl -X POST "http://localhost:8000/messageucolab/v1/messages/{messageCode}/translations" -H "Content-Type: application/json" -d '{"sourceLanguage":"auto","targetLanguage":"en"}'

# Get token for an application
# Direct access
curl http://localhost:8085/messageucolab/v1/application/{id}/token

# Through API Gateway
curl http://localhost:8000/messageucolab/v1/application/{id}/token
```

## Monitoring and Logging

- Application logs are pushed to Loki and persisted in the `loki-data` Docker volume (independent of the application lifecycle)
- The application exposes Prometheus metrics at `http://localhost:8085/actuator/prometheus`
- Access to monitoring interfaces:
  - Grafana: `http://localhost:3000` (user: admin, password: admin)
  - Prometheus: `http://localhost:9090`
  - Loki: `http://localhost:3100`

## Troubleshooting

Common problems and solutions:

1. Port already in use:
   - Change the port using the `SERVER_PORT` environment variable
   - Or stop the process that is using the conflicting port

2. Connection problems with MongoDB/Redis/PostgreSQL:
   - Verify that the services are running
   - Check credentials and configurations in `application.properties`
   - Verify network connectivity

3. Compilation errors:
   - Make sure `JAVA_HOME`, `java -version` and `./mvnw -version` use JDK 25
   - Clean and rebuild the project: `./mvnw clean install`

## Using Docker for Infrastructure (Optional)

If you prefer not to install all infrastructure components locally, you can use Docker to run them. In the `deployment/docker` folder, you'll find a `docker-compose.yml` file that you can use to start all the necessary services:

```bash
cd deployment/docker
docker-compose up -d
```

This will start:

- MongoDB (port 27017)
- Redis (port 6379)
- PostgreSQL (port 5435)
- Apache Pulsar (port 6650, 8080)
- Apache Kafka (port 9094)
- Zookeeper (port 2181)
- KSQLDB Server (port 8088)
- Debezium Connect (port 8083)
- Kong API Gateway (ports 8000, 8001)
- Observability services:
  - Grafana (port 3000)
  - Loki (port 3100)
  - OpenTelemetry Collector (ports 4317, 4318)
  - Prometheus (port 9090)

Then you can run the application connecting to these services.

### Kong Gateway Configuration

After starting the services with Docker Compose, you can access Kong Gateway at:
- API Gateway: `http://localhost:8000`
- Admin API: `http://localhost:8001`

The gateway is configured in DB-less mode using the configuration file at `deployment/docker/kong.yaml`.

### Kafka Configuration

The Kafka cluster is configured with:
- External access on port 9094
- Internal communication on port 9092
- Automatic topic creation enabled
- Single broker setup for development

### Debezium Connect Configuration

Debezium Connect is configured to:
- Connect to Kafka cluster
- Monitor PostgreSQL database changes
- Monitor MongoDB database changes
- Store configurations in Kafka topics

# Change Data Capture (CDC) Configuration

## Create Required Kafka Topics

Before configuring Debezium and KSQLDB, you must create the necessary Kafka themes. You can do this by running the provided script inside the kafka container:

1. **Create each topic:**
   
   a. First, connect to the Kafka container:
   ```bash
   docker exec -it kafka-crosswords bash
   ```

   b. Once inside the container, create each topic using the following commands:
   ```bash
   # Create topics for reference data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.language_base_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.application_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.application_state_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.environment_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.environment_state_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.environment_type_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.functionality_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.functionality_state_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_category_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_environment_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_environment_state_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_state_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.message_type_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.token_data
   kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --topic postgres.public.token_state_data
   ```

   c. Verify that all topics were created successfully:
   ```bash
   kafka-topics --list --bootstrap-server localhost:9092
   ```

   d. Exit the Kafka container:
   ```bash
   exit
   ```

This step is crucial as it prepares the Kafka infrastructure for the CDC pipeline. The topics will store the change events from PostgreSQL that will later be processed by KSQLDB and synchronized to MongoDB.

## Debezium PostgreSQL Configuration

To configure CDC with Debezium, follow these steps:

1. **Configure PostgreSQL Connector**
   
   Make a POST request to the Kafka Connect endpoint:
   ```bash
   curl -X POST http://localhost:8083/connectors \
   -H "Content-Type: application/json" \
   -d '{
     "name": "postgres-source-connector",    
     "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "transforms.unwrap.delete.handling.mode": "rewrite",
        "slot.name": "debezium_slot",
        "publication.name": "debezium_publication",
        "transforms": "unwrap,extractId",
        "transforms.unwrap.rewrite.drop.tombstones": "true",
        "topic.prefix": "postgres",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "key.converter": "org.apache.kafka.connect.storage.StringConverter",
        "database.user": "your_postgres_user",
        "database.dbname": "crs-crossword-db",
        "database.server.name": "postgres",
        "database.port": "5432",
        "plugin.name": "pgoutput",
        "key.converter.schemas.enable": "false",
        "transforms.unwrap.unwrap.keys": "true",
        "database.hostname": "debezium-postgres",
        "transforms.extractId.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
        "database.password": "your_postgres_password",
        "name": "postgres-source-connector",
        "value.converter.schemas.enable": "false",
        "transforms.unwrap.add.fields": "op,source.ts_ms",
        "transforms.extractId.field": "id",
        "table.include.list": "public.language_base_data,public.application_state_data,public.application_data,public.environment_type_data,public.environment_state_data,public.environment_data,public.functionality_state_data,public.functionality_data,public.message_category_data,public.message_type_data,public.message_state_data,public.message_data,public.message_environment_state_data,public.message_environment_data,public.token_state_data,public.token_data,public.message_data_table"
      }
   }'
   ```

2. **Configure KSQLDB**

   a. Access the KSQLDB server:
   ```bash
   ksql http://localhost:8088
   ```

   b. Execute the SQL commands contained in the `deployment/docker/scripts/ksql.sql` file:
   - Creation of reference data tables
   - Creation of transactional data streams
   - Creation of enriched data streams with joins

3. **Configure MongoDB Connectors**

   Make POST requests for each MongoDB connector:
   ```bash
   # Token State Connector
   curl -X POST http://localhost:8083/connectors \
   -H "Content-Type: application/json" \
   -d '{
     "name": "mongodb-token-state-sink",
     "config": {
       "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
       "topics": "postgres.public.token_state_data",
       "collection": "token_state",
       "database": "messageuco",
       "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.PartialValueStrategy",
       "document.id.strategy.partial.value.projection.list": "id",
       "connection.uri": "mongodb://your_mongodb_user:your_mongodb_password@localhost:27017/messageuco",
       "value.converter.schemas.enable": "false",
       "value.converter": "org.apache.kafka.connect.json.JsonConverter",
       "document.id.strategy.partial.value.projection.type": "allowlist",
       "key.converter": "org.apache.kafka.connect.storage.StringConverter",
       "deletes.enabled": "true",
       "transforms": "unwrap",
       "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
       "transforms.unwrap.delete.handling.mode": "rewrite",
       "transforms.unwrap.drop.tombstones": "true",
       "errors.log.enable": "true"
     }
   }'

   # Token Connector
   curl -X POST http://localhost:8083/connectors \
   -H "Content-Type: application/json" \
   -d '{
     "name": "mongodb-token-sink",
     "config": {
       "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
       "topics": "postgres.public.token_data",
       "collection": "token",
       "database": "messageuco",
       "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.PartialValueStrategy",
       "document.id.strategy.partial.value.projection.list": "id",
       "connection.uri": "mongodb://your_mongodb_user:your_mongodb_password@localhost:27017/messageuco",
       "value.converter.schemas.enable": "false",
       "value.converter": "org.apache.kafka.connect.json.JsonConverter",
       "document.id.strategy.partial.value.projection.type": "allowlist",
       "key.converter": "org.apache.kafka.connect.storage.StringConverter",
       "deletes.enabled": "true",
       "transforms": "unwrap",
       "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
       "transforms.unwrap.delete.handling.mode": "rewrite",
       "transforms.unwrap.drop.tombstones": "true",
       "errors.log.enable": "true"
     }
   }'

   # Message Environment Connector
   curl -X POST http://localhost:8083/connectors \
   -H "Content-Type: application/json" \
   -d '{
     "name": "mongodb-message-environment-sink",
     "config": {
       "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
       "topics": "message_environment_data_stream",
       "collection": "message_environment",
       "database": "messageuco",
       "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.PartialValueStrategy",
       "document.id.strategy.partial.value.projection.list": "message_environment_id",
       "connection.uri": "mongodb://your_mongodb_user:your_mongodb_password@localhost:27017/messageuco",
       "value.converter.schemas.enable": "false",
       "value.converter": "org.apache.kafka.connect.json.JsonConverter",
       "document.id.strategy.partial.value.projection.type": "allowlist",
       "key.converter": "org.apache.kafka.connect.storage.StringConverter",
       "deletes.enabled": "true",
       "transforms": "unwrap",
       "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
       "transforms.unwrap.delete.handling.mode": "rewrite",
       "transforms.unwrap.drop.tombstones": "true",
       "errors.log.enable": "true"
     }
   }'
   
   # Environment Connector
   curl -X POST http://localhost:8083/connectors \
   -H "Content-Type: application/json" \
   -d '{
     "name": "mongodb-environment-sink",
     "config": {
        "connector.class": "com.mongodb.kafka.connect.MongoSinkConnector",
        "topics": "postgres.public.environment_data",
        "collection": "environment",
        "database": "messageuco",
        "document.id.strategy": "com.mongodb.kafka.connect.sink.processor.id.strategy.PartialValueStrategy",
        "document.id.strategy.partial.value.projection.list": "id",
        "connection.uri": "mongodb://your_mongodb_user:your_mongodb_password@localhost:27017/messageuco",
        "value.converter.schemas.enable": "false",
        "name": "mongodb-environment-sink",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "document.id.strategy.partial.value.projection.type": "allowlist",
        "key.converter": "org.apache.kafka.connect.storage.StringConverter",
        "deletes.enabled": "true",
        "transforms": "unwrap",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.unwrap.delete.handling.mode": "rewrite",
        "transforms.unwrap.drop.tombstones": "true",
        "errors.log.enable": "true"
     }
   }'
   ```

## MongoDB Data Structure

After configuration, data will be synchronized in the following MongoDB collections:

- `token_state`: Token states
- `token`: Token information
- `message_environment`: Message-environment relationship
- `environment`: Environment information

## Verification

To verify that everything is working correctly:

1. Check connector status:
   ```bash
   curl http://localhost:8083/connectors
   ```

2. Check container logs:
   ```bash
   docker-compose logs -f connect
   docker-compose logs -f kafka
   ```

3. Query data in MongoDB:
   ```bash
   mongosh "mongodb://your_mongodb_user:your_mongodb_password@localhost:27017/messageuco"
   ```
