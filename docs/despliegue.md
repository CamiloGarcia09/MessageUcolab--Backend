# Despliegue de MessageUcoLab

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad:** Dockerfiles, archivos Compose y pipelines del repositorio

## Requisitos

- Docker Engine y Docker Compose.
- Doppler CLI con proyecto `messageucolab` y configuración `dev` para local.
- `GITHUB_PACKAGES_USER` y `GITHUB_PACKAGES_TOKEN` con acceso al starter PEP.
- Security Baseline disponible y administrado por su equipo responsable.

No se necesita un `.env` con secretos. `deployment/local/.env` contiene configuración no sensible de la distribución con imagen publicada; las credenciales continúan en Doppler.

JDK 25 y Maven Wrapper 3.9.9 solo se requieren para compilar o verificar el proyecto directamente en el host. El arranque oficial local no ejecuta Maven fuera de Docker.

## Desarrollo local

Verifique herramientas:

```powershell
docker version
docker compose version
doppler --version
```

Construya y levante el stack completo desde `deployment/docker`:

```powershell
cd deployment/docker
doppler run --project messageucolab --config dev -- docker compose up -d --build
cd ../..
```

El comando levanta MessageUcoLab y todas sus dependencias declaradas en el Compose. No ejecute después `spring-boot:run` ni otro comando Maven en el host. El flujo de sesión BFF está habilitado por defecto; desde el contenedor, PDP y PEP se alcanzan mediante `host.docker.internal` según la configuración de Doppler.

## Reconstrucción aislada del backend

Cuando el stack completo ya está disponible, puede reconstruir únicamente el backend:

```powershell
cd deployment/docker
doppler run --project messageucolab --config dev -- docker compose up -d --build --no-deps messageucolab
```

`--no-deps` evita reinicializar dependencias y por eso esta variante no sustituye el primer arranque del stack completo. El Dockerfile ejecuta `clean verify`; no debe usarse para evadir la autorización obligatoria de pruebas del proyecto.

Dentro del contenedor, PDP y PEP se alcanzan mediante `host.docker.internal`. No use `localhost` para servicios publicados por el host.

## Imagen publicada

`deployment/local` permite ejecutar MessageUcoLab sin compilar el código. Su Compose obtiene:

```text
${MESSAGEUCOLAB_DOCKER_ORG}/messageucolab:${MESSAGEUCOLAB_VERSION}
```

Consulte `deployment/local/README.md` para preparar el paquete local. La imagen debe usar una etiqueta explícita; no se recomienda `latest` para una entrega reproducible.

## Servicios

| Servicio | Puerto local habitual |
|---|---:|
| Backend | 8085 |
| Kong | 8000 |
| SurrealDB | 8005 |
| Redis | 6379 |
| Ollama | 11434 |
| Loki | 3100 |
| Grafana | 3000 |
| Prometheus | 9090 |

Los valores reales están parametrizados. No copie credenciales desde Doppler para completar archivos locales.

## Inicializadores y persistencia

El Compose incluye:

- `surrealdb-init`: importa el seed completo para una instalación nueva.
- `redis-init`: carga el catálogo de textos.
- `ollama-init`: descarga el modelo configurado.
- `surrealdb-import-schema`: mantenimiento aditivo del recibo de importación.

El seed completo no es una migración. No lo ejecute sobre una base con datos que deban conservarse. Eliminar o recrear un contenedor no necesariamente elimina su volumen.

Los archivos bajo `deployment/docker/surreal-data` no representan una estrategia de despliegue y su presencia versionada es deuda pendiente del repositorio.

## Dockerfile

La imagen usa dos etapas:

1. `eclipse-temurin:25-jdk-noble` compila con Maven Wrapper y secretos BuildKit.
2. `eclipse-temurin:25-jre-noble` ejecuta la aplicación con un usuario sin privilegios.

El build conserva únicamente el binario nativo de SurrealDB correspondiente a la arquitectura objetivo. El runtime expone 8085 y comprueba `/actuator/health`.

Los secretos Maven se montan con BuildKit y no se copian a la imagen final.

## CI

`azure-pipelines.yml` se activa en `develop` y en pull requests dirigidos a `develop`.

La validación incluye:

1. Herramientas del agente.
2. `clean verify` con Temurin 25.
3. Resultados Surefire y cobertura JaCoCo.
4. Docker Compose de Azure.
5. Plantillas Bicep.
6. Construcción de la imagen.

El pipeline requiere credenciales de lectura de GitHub Packages. No debe recibir el grupo completo de secretos de despliegue para validar un pull request.

### Publicación del cliente Java

Los tres artefactos del cliente Java se publican automáticamente en GitHub Packages cuando un cambio bajo `clients/java` llega a `main`. El workflow `.github/workflows/publish-java-client.yml` toma como base el último tag `messageucolab-client-vX.Y.Z`, incrementa el patch y usa esa versión para el reactor Maven.

La publicación crea el tag y el GitHub Release correspondientes. Si una ejecución se reintenta sobre el mismo commit, reutiliza la versión reservada; si una publicación anterior quedó incompleta, bloquea versiones posteriores hasta resolverla. Las pruebas no se repiten durante el despliegue porque el cambio ya debe haber superado `clean verify` en el pipeline de integración.

La versión `0.1.0` fue la publicación inicial. La primera publicación generada por este workflow será `0.1.1`.

## Despliegue en Azure

`azure-deploy-pipeline.yml` es manual y solo acepta `refs/heads/develop`. Admite:

- `DEPLOY`: construye, publica en ACR, copia archivos a la VM, despliega y ejecuta un smoke test.
- `STOP`: elimina las reglas públicas pertinentes y desasigna la VM.

El despliegue usa una etiqueta de imagen basada en el SHA del commit, acceso SSH temporal y una regla pública para Kong. La configuración vive en:

- `infrastructure/azure/main.bicep`
- `infrastructure/azure/key-vault-access.bicep`
- `deployment/azure/docker-compose.yml`
- `deployment/azure/deploy.sh`

No se debe desplegar directamente desde `main`: CI y CD están diseñados para `develop`.

## Verificación operativa

Después de desplegar, compruebe como mínimo:

- Health directo y por Kong.
- OpenAPI y Swagger UI.
- Conectividad con SurrealDB y Redis.
- Registro y decisión del PEP.
- Sesión BFF válida y rechazo sin sesión.
- Operaciones administrativas permitidas y denegadas.
- Consulta pública y rate limiting.
- Traducción y conservación de marcadores.
- SSE y aislamiento por aplicación.
- Métricas Prometheus y logs Loki.

Una compilación exitosa no sustituye estas comprobaciones.

## Reversión

Conserve la etiqueta anterior de la imagen. Java 25 produce bytecode incompatible con runtimes anteriores; una reversión restaura la imagen completa, no solo el JRE.

No revierta datos mediante el seed completo. Las migraciones y cambios de catálogo requieren un procedimiento específico y respaldo.

## Estado de esta actualización

La consolidación documental del 31 de octubre de 2026 no ejecutó `clean verify`, no construyó imágenes, no inició contenedores y no desplegó recursos. Por tanto, no certifica el estado actual de CI, Azure o servicios locales.

## Documentos relacionados

- [Arquitectura](arquitectura.md)
- [Integración de seguridad](integracion-seguridad.md)
- [Importación de mensajes](importacion-mensajes.md)
