# Revisión de compatibilidad EcoRutas ↔ EcoAPI

Se revisó el ZIP del backend `Backend-Unid2-develop.zip` sin modificarlo. Los cambios de esta entrega solo afectan al proyecto Android/KMP.

## Corrección de compilación

En `EcoRutasApp.kt`, `state.route` se leía varias veces tras comprobar que no era nulo. Como `state` viene de una propiedad delegada (`collectAsState()`), Kotlin no garantiza el smart cast a través de accesos repetidos. Se copia el valor en una variable local estable (`val route = state.route`) antes de las comprobaciones. La misma pauta se usa en `RouteScreen`.

## Contrato HTTP revisado contra el backend

| Funcionalidad | Endpoint en el backend | DTO/cuerpo usado por Android |
|---|---|---|
| Inicio de sesión | `POST /api/v1/auth/login` | `usuario`, `contrasena`; respuesta `token`, `tipo`, `usuario`, `nombre`, `rol`, `privilegios`, `expiraEnSegundos` |
| Usuario actual | `GET /api/v1/auth/me` | `id`, `usuario`, `nombre`, `rol`, `privilegios` |
| Ruta del día | `GET /api/v1/rutas/dia?fecha=` | `id`, `fecha`, `estado`, `operario`, `puntos[]` |
| Puntos paginados | `GET /api/v1/puntos?page=&size=&zona=` | envoltorio `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima` |
| Pesajes de un punto | `GET /api/v1/puntos/{puntoId}/pesajes?page=&size=` | respuesta paginada de `PesajeDto` |
| Registrar pesaje | `POST /api/v1/puntos/{puntoId}/pesajes` | `material`, `pesoBruto`, `tara`, `observacion`; neto calculado por el servidor |
| Corregir pesaje | `PUT /api/v1/pesajes/{pesajeId}` | mismos campos que registrar pesaje |
| Anular pesaje | `PATCH /api/v1/pesajes/{pesajeId}/anular` | `motivo` |
| Crear punto | `POST /api/v1/puntos` | `razonSocial`, `direccion`, `zona`, `materialHabitual` |
| Omitir punto | `PUT /api/v1/rutas/{rutaId}/puntos/{puntoId}/omision` | `motivo` |
| Cerrar ruta | `POST /api/v1/rutas/{rutaId}/cerrar` | sin cuerpo |

Los endpoints, nombres de campos, verbos HTTP y envoltorio de paginación de la capa Android coinciden con los controladores y DTO visibles en el ZIP del backend entregado. No se modificaron los archivos de backend ni sus contratos.

## URL de conexión

`shared/src/commonMain/kotlin/pe/upeu/ecorecicla/AppConfig.kt` usa `http://10.0.2.2:8080`, apropiado para el emulador Android estándar si el backend publica el puerto 8080 del host. En un teléfono físico debe usarse la IP LAN de la computadora y permitir el acceso en el firewall.

## Alcance de la verificación

La revisión comparó estáticamente los endpoints y DTO de ambos ZIP y corrigió el error de smart cast informado. Este entorno no dispone de Android SDK ni del Gradle distribution cache para asegurar un `assembleDebug` completo, por lo que la compilación final debe verificarse en Android Studio con JDK 21.
