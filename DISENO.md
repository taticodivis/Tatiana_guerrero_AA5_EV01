# Diseño del servicio web — AA5-EV01

Aprendiz: Tatiana Guerrero.

## Objetivo y alcance

Permitir registrar usuarios y verificar sus credenciales mediante un servicio web. El consumidor envía una solicitud HTTP y recibe un resultado JSON con `ok` (booleano), `codigo` (identificador del resultado) y `mensaje` (texto explicativo).

## Casos de uso

| Caso | Entrada y condición | Resultado |
| --- | --- | --- |
| Registrar usuario | Usuario válido no registrado, contraseña válida y confirmación coincidente | HTTP 201, USUARIO_CREADO; cuenta persistida |
| Registrar duplicado | Usuario ya existente, sin distinguir mayúsculas | HTTP 409, USUARIO_DUPLICADO |
| Iniciar sesión | Usuario registrado y contraseña correcta | HTTP 200, AUTENTICACION_CORRECTA |
| Rechazar acceso | Usuario inexistente o contraseña incorrecta | HTTP 401, CREDENCIALES_INVALIDAS |
| Rechazar campos vacíos | Usuario o contraseña ausentes o en blanco | HTTP 400, CAMPOS_OBLIGATORIOS |
| Rechazar registro inválido | Usuario inválido, longitud incorrecta o confirmación distinta | HTTP 400, DATOS_INVALIDOS |

La confirmación de contraseña es una decisión de diseño del registro; el caso de la guía exige usuario y contraseña. No se necesita confirmación para autenticar.

## Arquitectura

```text
Cliente HTTP / Postman
        | solicitud de formulario
        v
Spring Boot -> ApiController -> UsuarioRepository -> data/usuario.properties
        | respuesta JSON
        v
Cliente HTTP / Postman
```

El controlador REST separa el contrato HTTP de la validación y persistencia. El repositorio conserva un mapa de cuentas y sincroniza el registro para impedir altas duplicadas concurrentes dentro de la misma instancia.

## Datos y validaciones

Una cuenta contiene nombre normalizado, sal aleatoria de 16 bytes y hash de 32 bytes. Se eliminan espacios exteriores del nombre y se convierte a minúsculas. El usuario admite de 3 a 40 caracteres ASCII: letras, números, punto, guion y guion bajo. La contraseña debe tener de 12 a 128 caracteres; no se recorta ni se convierte a minúsculas.

Las contraseñas se derivan con PBKDF2-HMAC-SHA256 y 600000 iteraciones, sin almacenarlas en texto legible. El archivo se reemplaza mediante un archivo temporal. Las respuestas de acceso fallido no distinguen entre usuario inexistente y clave incorrecta.

## Contrato HTTP

| Método y ruta | Finalidad | Estados principales |
| --- | --- | --- |
| GET /crm/api/estado | Consultar disponibilidad | 200 |
| POST /crm/api/registro | Crear cuenta | 201, 400, 409 |
| POST /crm/api/login | Verificar credenciales | 200, 400, 401 |

Para los POST se requiere `Content-Type: application/x-www-form-urlencoded`. Un cuerpo de otro tipo devuelve 415. Un método incorrecto devuelve 405 y cabecera `Allow`. Un fallo al guardar devuelve 500 con mensaje genérico. Las respuestas usan `application/json` y UTF-8, sin caché. El contrato detallado está en `ENDPOINT.txt`.

## Flujo de acceso

1. El cliente envía usuario y clave a `/crm/api/login`.
2. El servicio comprueba formato y campos obligatorios.
3. El repositorio normaliza el usuario y deriva la contraseña recibida con la sal almacenada.
4. Compara el hash calculado con el guardado.
5. Devuelve autenticación satisfactoria o error de credenciales.

La autenticación no crea una sesión ni otorga acceso a otros recursos. El almacenamiento en archivos corresponde a una demostración local con una sola instancia del servidor.
