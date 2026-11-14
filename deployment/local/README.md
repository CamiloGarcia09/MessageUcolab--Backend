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

## Habilitar la traducción desde el componente de Parámetros (opcional)

La traducción con IA viene apagada (`TRANSLATION_AI_ENABLED=false`). Si tu equipo usa el Catálogo de Parámetros, la
puede encender o apagar desde allí, sin reiniciar MessageUcoLab:

1. En el Catálogo, crea en la aplicación de MessageUcoLab el parámetro **`TRADUCCION_HABILITADA`** con valor `true`
   o `false`.
2. En **Tokens → Nuevo**, crea un token para esa aplicación y ese ambiente, y copia su ID y su secreto.
3. En el `.env` de este kit pon la dirección del Catálogo y el ID del token; el secreto va en Doppler:

| Variable | Dónde | Valor |
|---|---|---|
| `CATALOGO_PARAMETROS_BASE_URL` | `.env` | `http://host.docker.internal:8081/catalogo-parametros/api/v1` |
| `CATALOGO_PARAMETROS_TOKEN_ID` | `.env` | El ID del token |
| `CATALOGO_PARAMETROS_SECRETO` | Doppler | El secreto del token |

4. Recrea MessageUcoLab: `doppler run -- docker compose -p messageucolab up -d messageucolab`.

MessageUcoLab consulta el parámetro cada 60 segundos. Si el Catálogo no responde, conserva el último valor
conocido; si nunca lo pudo leer, usa `TRANSLATION_AI_ENABLED`. Para traducir de verdad, además debe estar Ollama
(`COMPOSE_PROFILES=ollama`) o un proveedor configurado.

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
