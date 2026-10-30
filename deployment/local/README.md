# MessageUcoLab en local, sin el código

Carpeta para que los equipos del semillero levanten MessageUcoLab con la **imagen publicada en Docker
Hub**, igual que el componente de seguridad: no hace falta clonar el backend ni compilar.

| Archivo | Para qué |
|---|---|
| `compose.yaml` | Los servicios de MessageUcoLab. La aplicación usa la imagen `${MESSAGEUCOLAB_DOCKER_ORG}/messageucolab:${MESSAGEUCOLAB_VERSION}` |
| `.env` | Puertos, direcciones y la versión de la imagen. **No lleva contraseñas** |
| `empaquetar.ps1` | Arma `messageucolab-local.zip` con estos archivos y la configuración de `../docker` |

Las contraseñas (`SURREALDBUSER`, `SURREALDBPASSWORD`, `REDISPASSWORD`, `GF_SECURITY_ADMIN_PASSWORD` y
`PEP_REGISTRATION_TOKEN`) van en Doppler, como explica la guía de integración.

## Levantar (equipos)

```bash
doppler run -- docker compose -p messageucolab up -d
```

## Publicar una versión (equipo MessageUcoLab)

1. Desde la raíz del backend, en `develop`, construye y publica la imagen (pide `docker login` antes):
   ```bash
   doppler run -- docker build --secret id=maven_settings,src=.mvn/settings.xml --secret id=github_packages_token,env=GITHUB_PACKAGES_TOKEN --build-arg GITHUB_PACKAGES_USER --tag <usuario>/messageucolab:<versión> .
   docker push <usuario>/messageucolab:<versión>
   ```
2. Cambia `MESSAGEUCOLAB_VERSION` en `.env` y arma el paquete para los equipos:
   ```powershell
   .\empaquetar.ps1
   ```
