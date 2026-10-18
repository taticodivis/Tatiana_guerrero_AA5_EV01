# Codivis — Servicio web de registro y autenticación

**Aprendiz:** Tatiana Guerrero. **Programa:** Análisis y Desarrollo de Software, SENA.

**Evidencia:** GA7-220501096-AA5-EV01, Diseño y desarrollo de servicios web - caso.

## Enunciado de la guía 7

El apartado 3.1.5 solicita diseñar y codificar un servicio web para registro e inicio de sesión. Debe recibir usuario y contraseña, comunicar autenticación satisfactoria cuando sean correctos y devolver un error en caso contrario. El código debe contener comentarios y utilizar versionamiento. La entrega es un ZIP o RAR con los archivos del proyecto y el enlace del repositorio.

## Solución realizada

API HTTP en Java 17 o superior con Spring Boot y Spring MVC, con servidor integrado. Las solicitudes de registro y acceso usan formularios `application/x-www-form-urlencoded`; las respuestas son JSON. El diseño se explica en [DISENO.md](DISENO.md) y las rutas en [ENDPOINT.txt](ENDPOINT.txt).

- Registro: valida usuario, contraseña y confirmación; rechaza duplicados.
- Acceso: verifica las credenciales y devuelve un mensaje de éxito o error.
- Persistencia: cuentas en `data/usuario.properties`, con hashes PBKDF2-HMAC-SHA256, sal aleatoria y 600000 iteraciones.
- No hay cuentas predeterminadas. No devuelve contraseñas ni hashes al consumidor.
- El caso verifica credenciales; no emite tokens ni crea sesiones. Incluye una página de registro y acceso que consume la API y muestra sus resultados con SweetAlert2. También se puede consumir desde Postman.

## Ejecutar

En VS Code, abre esta carpeta y ejecuta `src/main/java/Run.java` con **Run Java**. Detener esa ejecución detiene el servidor.

En Windows también puedes abrir `Iniciar.cmd`. Necesitas un JDK 17 o superior y Maven. El iniciador reutiliza Maven y su repositorio de dependencias de la carpeta hermana `herramientas`, igual que AA3; si no existe, usa `mvn.cmd` en PATH. La primera descarga de dependencias requiere Internet. Spring Boot incorpora su servidor y no necesita instalar Tomcat por separado.

Página de acceso: http://localhost:8086/crm/

Estado: http://localhost:8086/crm/api/estado

El puerto 8086 permite mantener independiente este proyecto de AA5-EV02, que usa 8085. Los datos también son propios de esta carpeta. La API está limitada a la máquina local.

## Uso

En Postman importa `postman/Codivis-AA5-EV01.postman_collection.json`. Ejecuta primero Estado y Registrar usuario, y después Inicio de sesión. La colección incluye también casos inválidos. El registro recibe `usuario`, `clave` y `confirmacion`; el login recibe `usuario` y `clave`.

Ejemplo de autenticación correcta:

```json
{"ok":true,"codigo":"AUTENTICACION_CORRECTA","mensaje":"Autenticacion satisfactoria."}
```

Ejemplo de autenticación incorrecta (HTTP 401):

```json
{"ok":false,"codigo":"CREDENCIALES_INVALIDAS","mensaje":"Usuario o clave incorrectos."}
```

## Comprobación local

Con el servidor iniciado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Verificar-Api.ps1
```

Ejecuta 17 casos HTTP contra la API real y guarda el resultado en `evidencias/resultado-http.json`. Cada ejecución genera nombres de usuario nuevos. Esta comprobación no equivale a ejecutar Postman; la evidencia AA5-EV02 pide el video y las capturas de las pruebas en esa herramienta.

## Estructura y versionamiento

`ApiController.java` procesa HTTP y genera respuestas; `ErroresApi.java` mantiene los errores JSON; `UsuarioRepository.java` valida y persiste cuentas; `Run.java` permite iniciar desde VS Code. La interfaz está en `src/main/resources/static/`. Las clases contienen comentarios y se organizan en paquetes por responsabilidad. Se utilizan nombres de clases en PascalCase, métodos en camelCase y constantes en MAYUSCULAS.

El proyecto se versiona con Git. `enlace-repositorio.txt` debe contener la URL real del repositorio publicado antes de entregar. No se incluyen cuentas reales, `.git/`, `target/` ni herramientas externas en el ZIP.

Entrega: `Tatiana_Guerrero_AA5_EV01.zip`. Para esta evidencia la guía no exige video ni documento con capturas; corresponden a AA5-EV02.

## Alertas visuales

La interfaz usa SweetAlert2 11.26.25 desde jsDelivr (https://sweetalert2.github.io/). Necesita Internet para cargar esa biblioteca; si no está disponible, el mensaje sigue visible en la página. Los éxitos y errores provienen de la API real. No se crea sesión ni se redirige a un área privada tras autenticar.
