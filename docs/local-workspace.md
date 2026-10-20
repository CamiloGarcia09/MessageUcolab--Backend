# Espacio de trabajo local temporal

Este modo permite continuar el desarrollo mientras se retiran los contextos y los
tokens antiguos. No sustituye la integración definitiva con `security-baseline`.

## Contrato

- Se activa únicamente con el perfil Spring `local-workspace`.
- No exige los encabezados antiguos `Token` ni `Authorization: Bearer` simulado.
- El login del frontend con el PDP y sus cookies se conserva. Este login, por sí
  solo, **no protege el backend**: la integración y el enforcement del PEP deben
  verificarse por separado. No se desactiva su configuración existente.
- `GET /messageucolab/v1/workspace/local` devuelve JSON con `data: [workspace]`.
  El workspace contiene `organizationId`, `organizationName`, `applicationId`,
  `applicationName`, `environmentId` y `environmentName`.
- Los valores por defecto corresponden al seed: UCO / Arquisoft / Develop.
- El backend fija el ambiente para todas las operaciones de mensajes, incluido
  SSE. La aplicación se sigue obteniendo del ambiente persistido al crear un
  mensaje. El body de `POST /messageucolab/v1/messages` no cambia.
- Los endpoints antiguos de contexto y emisión de tokens no se habilitan en este
  modo. Fuera de este perfil, la autenticación antigua conserva su comportamiento.
- El frontend no consulta ni guarda contextos antiguos en este modo, ni permite
  cambiar el destino fijo. Los IDs se obtienen del backend, no de localStorage.

## Activación local

El frontend incluye `VITE_LOCAL_WORKSPACE=true` en `.env.development`. Solo tiene
efecto en el servidor de desarrollo de Vite, nunca en un build de producción.
Conservar la configuración existente del PDP, del API y del componente de seguridad.

Para el backend en Docker, desde `deployment/docker`:

```powershell
doppler run --project messageucolab --config dev -- docker compose -f docker-compose.yml -f docker-compose.local-workspace.yml up -d --no-deps --build messageucolab
```

El override limita la publicación directa del backend a `127.0.0.1` y no reinicia
los servicios de soporte ni sus inicializadores. Esto no aísla un proxy ya
publicado: el Compose base también incluye Kong en el puerto 8000, que podría
reenviar solicitudes al backend. No usar este modo en una red compartida ni
exponer el gateway mientras esté activo; su acceso es independiente del login
del frontend. No ejecutar un `up` general ni volver a importar el seed: su
inicializador puede reemplazar datos de desarrollo.

**Importante:** el Dockerfile ejecuta `clean verify`, incluidas las pruebas. Pedir
autorización para `unit-test-agent` antes de ejecutar este build, según `AGENTS.md`.

Para ejecutar Java directamente, activar `--spring.profiles.active=local-workspace`
al arrancar la aplicación mediante Doppler. El perfil escucha en `127.0.0.1` por
defecto; el override Docker cambia el bind interno, no la publicación local.

Se pueden configurar `LOCAL_WORKSPACE_ORGANIZATION_ID`,
`LOCAL_WORKSPACE_ORGANIZATION_NAME`, `LOCAL_WORKSPACE_APPLICATION_ID`,
`LOCAL_WORKSPACE_APPLICATION_NAME`, `LOCAL_WORKSPACE_ENVIRONMENT_ID` y
`LOCAL_WORKSPACE_ENVIRONMENT_NAME` con Doppler. Deben corresponder a registros
existentes y relacionados en SurrealDB. No son credenciales.

## Desactivación

1. En el frontend, establecer `VITE_LOCAL_WORKSPACE=false` y reiniciar Vite.
2. Recrear solo `messageucolab` usando el Compose base, sin el override local.
3. No activar el perfil `local-workspace` en Azure ni en entornos compartidos.

## Validación pendiente

Con autorización, verificar ausencia de tokens antiguos, acceso directo al panel,
creación/listado/traducción/SSE en el mismo ambiente, errores de la sesión nueva y
que fuera del perfil se sigan rechazando solicitudes sin autenticación antigua.
Comprobar también el enforcement real del PEP antes de exponer el backend.
