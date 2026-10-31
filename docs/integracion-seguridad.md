# Integración con Security Baseline

> **Última actualización:** 31 de octubre de 2026
> **Estado:** Documento oficial vigente
> **Fuente de verdad de seguridad:** contrato externo de Security Baseline y configuración del backend

## Límites de responsabilidad

Security Baseline es un componente externo y de solo lectura para este repositorio. MessageUcoLab no modifica su código, frontends, despliegue, contenedores, permisos ni datos.

| Responsabilidad | Componente |
|---|---|
| Autenticar al usuario | Security Baseline / proveedor de identidad |
| Mantener la sesión BFF | PDP/BFF de seguridad |
| Autorizar método y ruta | PEP de seguridad |
| Validar propiedad de datos | MessageUcoLab |
| Administrar organizaciones y aplicaciones de negocio | MessageUcoLab |

## Flujo vigente

1. El navegador inicia sesión con Security Baseline.
2. La cookie `SECURITY_BASELINE_SESSION` identifica la sesión BFF.
3. MessageUcoLab consulta `GET /api/v1/session` del PDP.
4. El PEP valida el permiso correspondiente al método y la ruta.
5. MessageUcoLab resuelve la referencia local del usuario.
6. Los casos de uso comprueban propiedad, aplicación y ambiente.

No existe un perfil alternativo, identidad simulada, destino fijo de desarrollo ni fallback sin autenticación.

## Identidad

La sesión proporciona `subject`, `tenantId`, `email` y `name`. La identidad estable local usa `identity_source + tenantId + subject`; correo y nombre son informativos.

Una sesión ausente produce `401`. Una respuesta inválida del BFF o una dependencia inaccesible no permite acceso por defecto.

## Tokens y credenciales

MessageUcoLab:

- No emite tokens propios.
- No genera ni administra claves RSA.
- No acepta el header `Token`.
- No almacena contraseñas de usuarios.
- No usa `DOPPLERTOKEN` desde la aplicación.
- No comparte `PEP_REGISTRATION_TOKEN` con consumidores.

Doppler CLI continúa inyectando la configuración del entorno. Los secretos no deben imprimirse, copiarse al chat ni guardarse en archivos versionados.

## Configuración local

| Servicio | URL local |
|---|---|
| PDP/BFF | `http://localhost:18080` |
| PEP | `http://localhost:18081` |
| Keycloak | `http://keycloak.localhost:19090` |
| Backend MessageUcoLab | `http://localhost:8085` |
| Kong MessageUcoLab | `http://localhost:8000` |

Java ejecutado en el host usa `localhost`. El backend dentro de Docker usa `host.docker.internal` para llegar al PDP y al PEP publicados por el host.

Propiedades principales:

```properties
components.security.session.pdp-url=${SECURITY_PDP_URL:http://localhost:18080}
security.pep.enforcement.pep-url=${SECURITY_PEP_ENFORCEMENT_PEP_URL:http://localhost:18081}
security.pep.enforcement.application-name=MessageUcoLab
security.pep.enforcement.environment=${SECURITY_PEP_ENFORCEMENT_ENVIRONMENT:dev}
```

Los valores `dev`, `lab` y `prod` son los ambientes admitidos por el contrato del PEP. El HTTP inseguro se limita al laboratorio local.

## Maven y starter PEP

`infrastructure` consume:

```text
co.edu.uco:security-pep-integration-spring-boot-starter:0.1.3
```

El paquete se descarga desde GitHub Packages. El build necesita `GITHUB_PACKAGES_USER` y `GITHUB_PACKAGES_TOKEN` con `read:packages`. `.mvn/settings.xml` contiene referencias a variables, no credenciales.

Spring Boot 4.1.1 y Spring Framework 7 son obligatorios para esta versión del starter. No se debe intentar compatibilidad mezclando Spring 6, Boot 3 o excluyendo WebClient.

## Rutas públicas

`/actuator/health` y `/messageucolab/v1/public/**` están excluidas del PEP en la configuración actual. La segunda exclusión es temporal mientras no exista autorización aplicación-a-aplicación.

Esto no convierte las rutas administrativas en públicas. La API pública valida aplicación, ambiente y disponibilidad del mensaje, pero no autentica al consumidor. El puerto 8085 no debe exponerse públicamente para evadir los controles de Kong.

## Manejo de respuestas

- `401`: sesión ausente, inválida o vencida.
- `403`: permiso PEP denegado o propiedad de negocio no válida.
- `503`: dependencia de identidad no disponible en operaciones que lo documentan.

El frontend puede solicitar reconexión manual ante `401`. No repite mutaciones automáticamente. Un `403` no inicia reautenticación y una caída de red no se presenta como sesión vencida.

## Restricciones operativas

- No iniciar, detener ni reconstruir Security Baseline desde este repositorio.
- No modificar roles, perfiles, permisos o asignaciones externas.
- No desactivar el PEP para resolver un error de acceso.
- No usar la sesión de un propietario como credencial máquina-a-máquina.
- No inferir vigencia de una asignación solo por etiquetas visuales del frontend de seguridad.

## Estado conocido

El usuario confirmó que el listado administrativo funciona después de registrar sus rutas y permisos. Ese antecedente no sustituye una verificación automatizada o un smoke test del despliegue actual.

La actualización documental del 31 de octubre de 2026 no ejecutó pruebas, no consultó sesiones reales y no modificó Security Baseline.

## Documentos relacionados

- [Espacios personales](espacios-personales.md)
- [Consumo de mensajes](consumo-mensajes.md)
- [Despliegue](despliegue.md)
