# Guía de integración: consumir MessageUcoLab

Actualizada el 20 de octubre de 2026 para el retiro del mecanismo propietario.
Las verificaciones históricas con ese mecanismo no certifican una integración
nueva con el proveedor de seguridad.

## 1. Responsabilidades

MessageUcoLab administra mensajes por Organización → Aplicación → Ambiente.
Las aplicaciones consumidoras consultan textos por código y deciden cómo
mostrarlos. El minifront administrativo no controla sus pantallas ni su idioma.

- El código es único por aplicación, con prefijo `MSG_`. Al crearlo se informa
  solo el sufijo: máximo 10 caracteres, letras, números y espacios.
- Título: entre 10 y 50 caracteres. Contenido: entre 10 y 100 caracteres.
- La aplicación aprovisiona Develop, Testing y Production automáticamente.
- Un mensaje solo se entrega si está activo y habilitado para el ambiente
  autorizado. No enviar aplicación o ambiente arbitrarios por query.

## 2. Autenticación y contexto

No se generan credenciales de integración en MessageUcoLab. El header `Token`
y el endpoint de emisión propietarios fueron retirados.

Antes de integrar un consumidor, acordar con el responsable de seguridad:

1. La identidad o sesión admitida por el proveedor y el PEP para ese consumidor.
2. Los recursos y permisos de lectura o traducción necesarios.
3. El contexto de organización, aplicación y ambiente autorizado en el backend.

El frontend administrativo conserva el login del PDP/BFF y envía sus cookies con
`credentials: 'include'`. No sustituirlo por credenciales almacenadas en el navegador.
Para servicios no humanos, no asumir que la sesión de un administrador es una
credencial de servicio ni compartir su contexto activo entre aplicaciones.

De forma predeterminada, la aplicación se valida contra la sesión BFF mediante
`X-Application-Id`. La resolución recibe `X-Environment-Id` como dato explícito y
comprueba que el ambiente pertenece a esa aplicación. No hay un destino fijo de
desarrollo ni un fallback que deduzca la aplicación desde el ambiente.
No se requieren perfiles de Spring ni banderas de frontend para activar este
flujo. No hay un interceptor alternativo de identidad simulada ni fallback propietario.

## 3. Consultar un mensaje

URL local por Kong: `http://localhost:8000`; backend directo: `http://localhost:8085`.
Esta petición debe llevar la autenticación acordada con seguridad:

```http
GET /messageucolab/v1/messages/resolve?code=MSG_PRODSAVED
Accept: application/json
X-Application-Id: <UUID de la aplicación asignada a la sesión>
X-Environment-Id: <UUID de un ambiente de esa aplicación>
```

Respuesta exitosa del contrato de consulta simplificada:

```json
{
  "code": "MSG_PRODSAVED",
  "title": "Producto guardado",
  "content": "El producto {nombre} se guardó correctamente."
}
```

Consultar [el contrato de consumo](message-consumer-api.md) y
`infrastructure/src/main/resources/static/openapi.yaml`. La consulta simplificada
no usa el envelope `data`/`errors` de las consultas administrativas.

El consumidor debe reemplazar los marcadores `{nombre}`; el backend entrega el
contenido original sin sustitución. La selección de idioma pertenece al consumidor.

## 4. Traducción

Requiere autorización para traducir en el contexto activo:

```http
POST /messageucolab/v1/messages/MSG_PRODSAVED/translations
Content-Type: application/json
Accept: application/json

{ "sourceLanguage": "auto", "targetLanguage": "en" }
```

La respuesta administrativa utiliza `data`/`errors` e incluye los campos
traducidos y metadatos del proveedor. La traducción con IA puede ser lenta:
preparar y cachear las traducciones en lugar de solicitarlas en cada renderizado.
Verificar la conservación de marcadores antes de usar el resultado.

## 5. Errores y resiliencia

| Estado | Significado | Acción del consumidor |
|---|---|---|
| 200 | Mensaje disponible | Usar el objeto del contrato correspondiente |
| 400 / 422 | Entrada o código inválido | Corregir la petición |
| 401 | Falta autenticación válida o identidad resuelta | Revisar la integración con seguridad |
| 403 | Falta permiso o el contexto está fuera del alcance | Solicitar revisión de recursos y permisos |
| 404 | Mensaje inexistente, inactivo o ajeno al contexto | Revisar código y ambiente |
| 500 / timeout | Fallo técnico o indisponibilidad | Usar el respaldo del consumidor |

Comprobar siempre el estado antes de deserializar. No asumir que los rechazos del
PEP o de los filtros tienen el mismo cuerpo que los errores de dominio.

Definir un timeout corto, texto de respaldo y caché acotada. La clave de caché
debe separar aplicación, ambiente e idioma; un cambio de contexto no debe
reutilizar textos de otro alcance. Los cambios se ven cuando vence o se invalida
esa caché. No se ofrece promoción de mensajes entre ambientes en este contrato.

## 6. Ejecutar el componente en desarrollo

Requiere Docker Compose y Doppler CLI vinculado al proyecto `messageucolab`,
configuración `dev`. No hace falta un `.env` ni acceso de escritura a secretos
desde la aplicación. Doppler CLI inyecta SurrealDB, Redis, Pulsar, IA y las
credenciales de registro del PEP. No imprimir ni copiar esos valores.

Consultar [integración de seguridad](security-integration.md) antes de levantar servicios. Seguridad y
sus frontends los administra su responsable: no iniciarlos, modificarlos ni
reiniciarlos desde MessageUcoLab.

Después de autorizar las pruebas que ejecuta el Dockerfile, para reconstruir
solo MessageUcoLab sobre la infraestructura ya disponible:

```powershell
cd deployment/docker
doppler run --project messageucolab --config dev -- docker compose up -d --no-deps --build messageucolab
```

No reimportar seeds ni borrar volúmenes para retirar el mecanismo propietario.
El nuevo seed deja de crear sus tablas; los datos y secretos preexistentes no
se eliminan automáticamente.

## Lista de integración

- [ ] Aplicación, funcionalidades y mensajes registrados en el ambiente correcto.
- [ ] Identidad o sesión y permisos acordados con seguridad.
- [ ] Contexto autorizado resuelto por el backend, sin IDs arbitrarios.
- [ ] Cliente sin el header propietario ni llamadas al endpoint retirado.
- [ ] Timeout, caché aislada, respaldo y sustitución de marcadores definidos.
- [ ] Verificados acceso permitido, rechazo sin autenticación, falta de permiso,
  mensaje inexistente e indisponibilidad, sin registrar credenciales.
