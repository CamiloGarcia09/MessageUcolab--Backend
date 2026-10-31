# Espacios personales de MessageUcoLab

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad funcional:** modelo de propiedad del backend y contrato OpenAPI

## Decisión vigente

MessageUcoLab administra organizaciones personales con un único propietario. Las aplicaciones de negocio pertenecen a una organización y tienen UUID propios, independientes de las aplicaciones registradas en Security Baseline.

Seguridad autentica al usuario y autoriza las rutas. MessageUcoLab decide si los datos solicitados pertenecen a ese usuario.

## Identidad local

`app_user` guarda una referencia local sin contraseña, token ni credencial. Su identidad estable se obtiene de:

```text
identity_source + tenant_id + subject
```

El correo y el nombre se actualizan desde la sesión BFF y son informativos. Nunca se usan como clave de autorización ni se aceptan desde un formulario para elegir propietario.

## Modelo de propiedad

```text
app_user
  └── organization.owner_user_id
        └── application.organization_id
              ├── environment.application_id
              ├── functionality.application_id
              └── message.application_id
```

Reglas vigentes:

- Una organización tiene un único propietario.
- El nombre de organización es único por propietario.
- El nombre de aplicación es único dentro de la organización.
- La aplicación hereda la propiedad de su organización.
- Las funcionalidades y mensajes se validan dentro de la aplicación autorizada.
- Un ambiente debe existir y pertenecer a la aplicación indicada.
- Las organizaciones históricas sin propietario son inaccesibles; no se asignan automáticamente.

No existen colaboradores, invitaciones, membresías ni RBAC local. Añadirlos requiere una decisión de producto y una migración explícita.

## Contrato HTTP

Todas las rutas siguientes están bajo `/messageucolab/v1` y requieren sesión BFF y permiso PEP:

| Método | Ruta | Resultado |
|---|---|---|
| `GET` | `/session/user` | Usuario autenticado |
| `GET` | `/organizations` | Organizaciones propias |
| `POST` | `/organizations` | Crear organización personal |
| `GET` | `/organizations/{organizationId}/applications` | Aplicaciones de una organización propia |
| `POST` | `/application` | Crear aplicación y sus ambientes |
| `GET` | `/session/applications` | Aplicaciones propias de la sesión |

El servidor toma el propietario de la sesión. No acepta propietario, correo o identidad enviados por el navegador.

El idioma mostrado para una aplicación proviene de `application.language_id`. No se toma del navegador ni se sustituye por el primer elemento del catálogo. Si la referencia no puede resolverse, el campo se omite.

## Frontend administrativo

El flujo vigente es:

```text
Mis organizaciones
  → Mis aplicaciones
    → Gestión de mensajes
```

Al cambiar de aplicación, el frontend debe limpiar catálogos dependientes, selección, paginación y suscripciones SSE. La aplicación seleccionada se envía mediante `X-Application-Id`, pero el backend siempre vuelve a validar su propiedad.

## Inicialización

Las bases nuevas reciben el modelo mediante `deployment/docker/scripts/surreal/surreal-init.surql`. Ese seed completo es solo para instalaciones nuevas y vacías.

No se debe ejecutar el seed completo como migración sobre datos que deban conservarse. Recrear contenedores tampoco garantiza eliminar almacenamiento persistente.

Los textos del modelo se cargan mediante `deployment/docker/scripts/redis/CatalogMessageInit.sh`, sin almacenar secretos en archivos.

## Errores de aislamiento

- UUID inválido: `422` según la validación de dominio.
- Sesión ausente o inválida: `401`.
- Falta de permiso PEP: `403`.
- Organización ajena o inexistente: `403` sin revelar su existencia.
- Dependencia de identidad o persistencia no disponible: `503` cuando el contrato lo contempla.

## Documentos relacionados

- [Integración de seguridad](integracion-seguridad.md)
- [Contrato de la API](contrato-api.md)
- [Arquitectura](arquitectura.md)
