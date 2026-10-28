# EcoRutas — Guía de uso y pruebas

**Proyecto:** EcoRecicla / EcoRutas  
**Aplicación:** Kotlin Multiplatform, Compose Multiplatform, Ktor Client, Koin y SQLDelight  
**Backend:** Spring Boot con Kotlin y PostgreSQL, ejecutado mediante Docker  
**JDK de desarrollo:** Java 21  

> Esta guía describe las cuentas semilla y las funciones presentes en la versión del proyecto revisada. Las cuentas son solo para demostración local; cambia las credenciales y la configuración antes de cualquier despliegue real.

## 1. ¿Para qué sirve EcoRutas?

EcoRutas permite que el personal de EcoRecicla consulte puntos de acopio y rutas, registre el peso de los materiales recolectados y sincronice los datos con el backend. La app conserva datos en SQLDelight para poder mostrar la información previamente descargada y guardar pesajes pendientes cuando el teléfono pierde conexión.

La app **no se conecta directamente a PostgreSQL**. Se comunica con la API REST EcoAPI mediante HTTP y el backend valida la sesión, los privilegios y las reglas de negocio.

## 2. Usuarios de demostración

Las cuentas siguientes se crean por `DataInitializer.kt` cuando la base de datos todavía no tiene usuarios y la inicialización de datos está habilitada.

| Usuario | Contraseña de demostración | Nombre mostrado | Rol |
|---|---|---|---|
| `operario1` | `Eco2026*` | Carlos Operario | `OPERARIO` |
| `supervisor1` | `Eco2026*` | María Supervisora | `SUPERVISOR` |
| `admin` | `Eco2026*` | Administrador EcoRecicla | `ADMINISTRADOR` |

Las contraseñas se guardan cifradas en la base mediante BCrypt. Si la base de datos ya contiene usuarios, reiniciar el backend no vuelve a crear ni restablece automáticamente estas cuentas.

### Rol OPERARIO — `operario1`

Puede:
- Consultar la ruta asignada, los puntos y los pesajes.
- Registrar pesajes en puntos de su ruta abierta de hoy.
- Corregir sus propios pesajes del mismo día, si la ruta continúa abierta.

No puede anular pesajes, cerrar rutas ni crear puntos de acopio. El backend debe rechazar esas operaciones aunque alguien intente invocarlas directamente.

### Rol SUPERVISOR — `supervisor1`

Puede:
- Consultar rutas, puntos y pesajes.
- Registrar pesajes en una ruta abierta de hoy.
- Corregir pesajes permitidos de la ruta actual.
- Anular pesajes indicando un motivo. La anulación conserva el registro en el historial.
- Registrar el motivo de omisión de un punto.
- Cerrar una ruta cuando cada punto planificado tenga un pesaje vigente o un motivo de omisión.

### Rol ADMINISTRADOR — `admin`

Puede:
- Consultar el padrón de puntos y los pesajes.
- Filtrar puntos por zona.
- Crear puntos de acopio con razón social, dirección, zona y material habitual.

En esta versión, el administrador **no registra pesajes**. El backend revisado tampoco expone un endpoint para crear usuarios desde la app; por tanto, estas tres cuentas son las disponibles de demostración hasta que se implemente la gestión de usuarios.

## 3. Cómo iniciar el backend

En la computadora que ejecuta Docker:

1. Abre PowerShell en la carpeta del backend, donde se encuentra `docker-compose.yml`.
2. Ejecuta:

   ```powershell
   docker compose up -d --build
   ```

3. Comprueba que los contenedores estén activos:

   ```powershell
   docker compose ps
   ```

4. Si necesitas ver los registros del servidor:

   ```powershell
   docker compose logs -f backend
   ```

La API se publica en el puerto `8080`. Para el emulador Android estándar, `10.0.2.2` apunta a la computadora anfitriona.

## 4. Conectar la aplicación Android

### Si usas el emulador Android Studio

La configuración habitual es:

```kotlin
const val API_BASE_URL = "http://10.0.2.2:8080"
```

Está en:

`shared/src/commonMain/kotlin/pe/upeu/ecorecicla/AppConfig.kt`

### Si usas un teléfono físico

1. Conecta la computadora y el teléfono a la misma red Wi-Fi.
2. En Windows, ejecuta `ipconfig` y localiza la dirección IPv4 del adaptador Wi-Fi, por ejemplo `192.168.1.25`.
3. Cambia temporalmente `API_BASE_URL` por la IP real de la computadora, conservando el puerto 8080. Ejemplo ilustrativo:

   ```kotlin
   const val API_BASE_URL = "http://192.168.1.25:8080"
   ```

   Reemplaza `192.168.1.25` por la IPv4 que tenga tu computadora; no copies el ejemplo literalmente si tu IP es distinta.
4. Deja Docker y el backend en ejecución. Si Windows Firewall bloquea la conexión, permite el acceso al puerto 8080 para la red privada.
5. Vuelve a compilar e instalar la app.

`10.0.2.2` sirve para el emulador estándar; en un teléfono físico normalmente debes usar la IPv4 LAN de la computadora. El tráfico HTTP sin TLS está habilitado solo para desarrollo local, no para producción.

## 5. Cómo usar la app

### Inicio de sesión

1. Abre **EcoRutas**.
2. Escribe un usuario y contraseña de la tabla anterior.
3. Pulsa **Entrar a mi ruta**.
4. Si no inicia sesión, verifica primero que el backend esté funcionando y que `API_BASE_URL` sea correcto.

La app recibe un JWT del backend y guarda la sesión cifrada mediante una clave almacenada en Android Keystore. El botón **Salir** / **Cerrar sesión y limpiar el equipo** borra la sesión local.

### Pestaña «Ruta»

- Muestra la ruta del día del usuario, su estado y los puntos en orden de visita.
- Toca un punto para abrir su detalle.
- En la base semilla inicial, la ruta de hoy tiene seis puntos: inicialmente cuatro atendidos y dos pendientes. Si ya registraste o anulaste datos durante las pruebas, el estado puede ser diferente.
- Un supervisor puede cerrar la ruta cuando todos los puntos tengan pesaje no anulado o motivo de omisión.
- La ruta debe estar descargada previamente para consultarla sin conexión.

### Pestaña «Puntos»

- Consulta el padrón de puntos de acopio.
- Escribe una zona (por ejemplo, `Norte`, `Centro` o `Sur`) y pulsa **Consultar filtro**.
- Pulsa **Todos** para quitar el filtro y refrescar los puntos.
- El administrador verá la acción **Crear punto de acopio**. Completa razón social, dirección, zona y material habitual y pulsa **Crear**.

### Detalle de un punto

- **Registrar:** escribe material, peso bruto en kg, tara en kg y, si corresponde, una observación. La app muestra una estimación del neto. El backend es la autoridad final y vuelve a calcular el peso neto; el resultado debe ser mayor que cero.
- **Actualizar:** consulta los pesajes más recientes del servidor cuando hay conexión.
- **Corregir:** disponible para pesajes permitidos por el rol, el propietario, la fecha y el estado de la ruta.
- **Anular:** solo aparece para usuarios con privilegio `PESAJE_ANULAR`; se exige un motivo y se conserva el registro anulado.
- **Registrar motivo de omisión:** disponible para un usuario con `RUTA_CERRAR` cuando el punto pertenece a la ruta abierta y aún está pendiente.

### Pestaña «Perfil»

Muestra el usuario, el rol y los privilegios de la sesión. Usa **Cerrar sesión y limpiar el equipo** al terminar un turno compartido.

### Botón «Sync»

Manda los pesajes pendientes al servidor antes de descargar la ruta y los puntos actualizados. La pantalla muestra el recuento de registros pendientes y un resumen de sincronización. Si vuelve la conexión después de haber estado desconectado, la app también intenta sincronizar automáticamente.

## 6. Cómo probar las funciones principales

Haz las pruebas de una en una y registra el código HTTP o el mensaje mostrado. Usa datos de demostración porque registrar, corregir, anular o cerrar una ruta modifica el estado de la base.

### Prueba A — Login y sesión

1. Inicia sesión como `operario1` / `Eco2026*`.
2. Comprueba que se abra la app y que aparezcan las pestañas Ruta, Puntos y Perfil.
3. En Perfil, confirma que el usuario sea `operario1`, el rol `OPERARIO` y se muestre la lista de privilegios.
4. Pulsa Salir y comprueba que vuelvas al login.

### Prueba B — Ruta y puntos

1. Inicia sesión como operario.
2. En Ruta, verifica fecha, estado y orden de los puntos.
3. En Puntos, filtra por `Norte` y luego pulsa Todos.
4. Abre un punto y revisa sus datos y sus pesajes.

### Prueba C — Registrar pesaje online

1. Con el backend activo, abre un punto de la ruta abierta de hoy.
2. Pulsa Registrar.
3. Introduce material `Papel`, bruto `25.5`, tara `2.5` y una observación opcional.
4. Pulsa Guardar y vuelve a consultar el punto.
5. Comprueba que el neto esperado es `23.0 kg` y que el pesaje está asociado al usuario que lo registró.

### Prueba D — Validación del peso

Intenta guardar un bruto de `2` y tara de `2`. La app debe mostrar validación y no enviar el registro porque el neto debe ser mayor que cero. El backend también valida la regla aunque se invoque desde otra herramienta.

### Prueba E — Permisos por rol

- **Operario:** registra pesajes y corrige solo los propios del día; no debe ver Anular, Crear punto ni Cerrar ruta.
- **Supervisor:** inicia sesión con `supervisor1` / `Eco2026*`; prueba Anular con un motivo y verifica que el registro permanezca en el historial. Puede omitir un punto y cerrar una ruta cuando cumpla la regla de cierre.
- **Administrador:** inicia sesión con `admin` / `Eco2026*`; crea un punto desde Puntos. No debe poder registrar pesajes.

El backend valida los privilegios; ocultar una acción en la interfaz es solo parte de la experiencia de usuario.

### Prueba F — Modo sin conexión (offline-first)

1. Con el teléfono online, inicia sesión y espera a que se descarguen y aparezcan la ruta y los puntos.
2. Abre un punto de la ruta de hoy que esté guardado localmente.
3. Activa el modo avión (o desconecta la red) después de comprobar que los datos ya están cargados.
4. Registra un pesaje válido. Debería guardarse localmente y marcarse como pendiente; el contador `pend.` debe aumentar. El registro local utiliza un ID provisional negativo.
5. Desactiva el modo avión y vuelve a tener conectividad con el backend.
6. Pulsa **Sync** y espera el resumen. Debe intentar subir los pendientes antes de descargar datos nuevos.
7. Comprueba que el contador de pendientes disminuya y que el pesaje sincronizado tenga el ID definitivo del servidor.

Si la ruta se cerró, el punto no corresponde a la ruta asignada o el servidor rechaza el registro por una regla de negocio, puede que el pendiente permanezca; revisa el mensaje y vuelve a probar con un punto elegible.

### Prueba G — Anulación y cierre de ruta

1. Inicia como supervisor.
2. Anula un pesaje válido con un motivo y comprueba que aparezca como anulado, no borrado.
3. Intenta cerrar una ruta que tenga puntos sin pesaje vigente ni motivo de omisión: el backend debe rechazar el cierre con conflicto (`409`).
4. Completa los puntos faltantes con pesaje o registra motivos de omisión válidos.
5. Vuelve a cerrar la ruta. El backend debe aceptar el cierre cuando todas las condiciones se cumplan.

## 7. Probar la API con Postman

Base URL:

`http://localhost:8080`

Ejecuta primero `POST /api/v1/auth/login`, copia el JWT y úsalo como `Authorization: Bearer <token>` en las solicitudes protegidas.

Endpoints principales:

| Método | Ruta | Privilegio / finalidad |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Login; público |
| `GET` | `/api/v1/auth/me` | Usuario actual; token válido |
| `GET` | `/api/v1/rutas/dia` | Ruta del día; `PUNTO_LEER` |
| `GET` | `/api/v1/puntos?page=0&size=20` | Listado paginado; `PUNTO_LEER` |
| `GET` | `/api/v1/puntos/{puntoId}/pesajes?page=0&size=20` | Pesajes de punto; `PESAJE_LEER` |
| `POST` | `/api/v1/puntos/{puntoId}/pesajes` | Registrar; `PESAJE_CREAR` |
| `PUT` | `/api/v1/pesajes/{pesajeId}` | Corregir; `PESAJE_EDITAR` y regla de propietario/fecha/ruta |
| `PATCH` | `/api/v1/pesajes/{pesajeId}/anular` | Anular; `PESAJE_ANULAR` |
| `POST` | `/api/v1/puntos` | Crear punto; `PUNTO_GESTIONAR` |
| `PUT` | `/api/v1/rutas/{rutaId}/puntos/{puntoId}/omision` | Motivo de omisión; `RUTA_CERRAR` |
| `POST` | `/api/v1/rutas/{rutaId}/cerrar` | Cerrar ruta; `RUTA_CERRAR` |

Códigos a reconocer:
- `200`: consulta o actualización correcta.
- `201`: creación correcta.
- `400`: datos inválidos; por ejemplo, peso neto menor o igual a cero.
- `401`: falta el token o no es válido / venció.
- `403`: usuario autenticado sin el privilegio o sin permiso sobre ese registro.
- `404`: recurso no encontrado.
- `409`: conflicto con una regla de negocio, por ejemplo ruta cerrada o incompleta.

## 8. Datos iniciales de demostración

Cuando se inicializa una base de datos vacía con el seed habilitado, el backend crea:
- 3 usuarios de demostración (uno por rol).
- 12 puntos de acopio distribuidos en Norte, Centro y Sur.
- 2 rutas: una de hoy abierta con 6 puntos y otra del día anterior cerrada.
- 15 pesajes para mostrar historial, un pesaje anulado y registros de distintos usuarios.

Las pruebas anteriores pueden cambiar el estado de la base. Para no perder registros, **no elimines el volumen de PostgreSQL solo para recuperar los datos semilla**.

## 9. Problemas frecuentes

- **No conecta el login:** verifica que Docker esté ejecutándose, que el backend esté en el puerto 8080 y que `API_BASE_URL` tenga la dirección correcta.
- **Emulador funciona, teléfono físico no:** cambia `10.0.2.2` por la IPv4 LAN de la computadora, confirma que ambos estén en la misma Wi-Fi y revisa el firewall.
- **401:** vuelve a iniciar sesión; el token pudo vencer o faltar.
- **403:** la cuenta no tiene el privilegio requerido. Prueba con el rol adecuado en vez de quitar la seguridad.
- **409 al registrar/corregir:** revisa que la ruta esté abierta, sea del día y esté asignada correctamente.
- **Los registros quedan pendientes:** comprueba la conectividad y pulsa Sync; el servidor puede rechazar la operación por permisos o reglas de negocio.
- **Cierre inesperado de la app:** abre Logcat en Android Studio y busca `FATAL EXCEPTION` y la última línea `Caused by`.

## 10. Limitaciones conocidas de esta versión

- No hay pantalla ni endpoint para crear usuarios nuevos desde la app; existen las tres cuentas semilla descritas arriba.
- La creación de puntos está disponible para el administrador; la creación de rutas está soportada por el backend, pero no se presenta como un flujo de usuario en la interfaz actual.
- La conexión HTTP sin TLS es para la demostración local y no debe publicarse como configuración de producción.
- La carpeta `iosApp` se conserva por la estructura KMP del curso, pero esta entrega está preparada y documentada para probarse en Android.

---

**Fin de la guía EcoRutas.**
