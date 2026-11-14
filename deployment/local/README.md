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

El puerto de entrada externo está asociado al listener `external`; no hace falta configurar `listenerName` en
el cliente. No uses `host.docker.internal:6800` desde otro contenedor mientras se anuncie `localhost`: el broker
lo redirigiría al propio contenedor consumidor. Usa la red compartida o un DNS accesible desde ambos.

### Consumidores en otra máquina

Configura `PULSAR_ADVERTISED_HOST` con el DNS del host donde corre Pulsar, sin esquema ni puerto, y
`PULSAR_BIND_ADDRESS` con la interfaz del host accesible desde esa red privada. Recrea solo Pulsar con
`doppler run -- docker compose -p messageucolab up -d --no-deps --force-recreate --wait pulsar`.
El consumidor usará `PULSAR_URL=pulsar://<ese-dns>:6800` (o el `PULSAR_PORT` elegido).
El DNS debe resolver desde el consumidor y el firewall debe permitir ese puerto.

Por defecto solo se publica en `127.0.0.1`. **Este kit de desarrollo no configura TLS, autenticación ni
autorización por topic en Pulsar. No lo publiques en Internet ni en una red no confiable.** Para consumidores
externos en producción se necesita un broker o proxy protegido, certificados y permisos por identidad/topic;
cambiar el DNS no sustituye esos controles. La identidad de servicio HTTP de MessageUcoLab no autentica por
sí sola al cliente Pulsar. El puerto administrativo 8080 permanece limitado al host.

### Persistencia y actualización desde un kit anterior

BookKeeper utiliza el nombre fijo `pulsar` y el puerto 3181. Su cookie debe contener `bookieHost: "pulsar:3181"`,
no una IP efímera de Docker. Se conserva `/pulsar/data`; `/pulsar/conf` se genera de nuevo desde la imagen
y las variables en cada recreación. No cambies el hostname de una instalación con datos existentes.

Un volumen antiguo puede conservar una IP o identidad incompatible. Errores como
`Error while recovering ledger` o `Not enough non-faulty bookies available` requieren revisar logs y cookies;
no demuestran por sí solos corrupción ni autorizan borrar datos.

**Solo si los datos de Pulsar son desechables y se autoriza perder eventos, topics y suscripciones**, puede
reinicializarse el volumen. Los mensajes de negocio siguen en SurrealDB; no se reproducen automáticamente
los avisos eliminados. El prefijo `messageucolab` debe coincidir con el proyecto Compose real:

```bash
docker compose -p messageucolab rm -sf pulsar
```

```bash
docker volume rm messageucolab_pulsar-data
```

```bash
doppler run -- docker compose -p messageucolab up -d pulsar
```

Usa `up -d --no-deps --wait pulsar` para esperar disponibilidad sin intervenir otras dependencias.
El antiguo volumen `messageucolab_pulsar-conf` ya no se monta y puede retirarse, previa comprobación de que
ningún contenedor lo usa. Nunca uses `down -v` para esta operación.
Si un consumidor ya agotó sus intentos de suscripción durante el fallo, reinicia solo ese consumidor después
de que Pulsar esté disponible. Las conexiones establecidas usan la reconexión del cliente Pulsar.

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
