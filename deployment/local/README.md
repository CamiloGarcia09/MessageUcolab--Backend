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

## Conectarse a Pulsar desde tu aplicación

Pulsar anuncia dos direcciones y cada cliente recibe la del puerto por el que entró. No hay que configurar nada
más que la dirección:

| Dónde corre tu aplicación | `PULSAR_URL` | Además |
|---|---|---|
| En tu PC (IntelliJ o terminal) | `pulsar://localhost:6800` | Nada |
| En un contenedor de Docker | `pulsar://pulsar:6650` | Conectar el contenedor a la red `messageucolab_pulsar-network` |

Si actualizas desde un kit anterior, recrea Pulsar una vez para que tome esta configuración. Pulsar solo guarda
los avisos de paso (los mensajes están en SurrealDB); si al arrancar el log dice
`Not enough non-faulty bookies available`, su almacenamiento quedó con el nombre del contenedor anterior y hay que
empezarlo de cero:

```bash
docker compose -p messageucolab rm -sf pulsar
```

```bash
docker volume rm messageucolab_pulsar-data
```

```bash
doppler run -- docker compose -p messageucolab up -d pulsar
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
