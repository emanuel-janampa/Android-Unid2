# Corrección de LocalEcoStore.kt: Missing return statement

En `shared/src/commonMain/kotlin/pe/upeu/ecorecicla/data/local/LocalEcoStore.kt`, las funciones `updatePendingWeighing` y `markWeighingVoidLocally` declaran que devuelven `Pesaje` y usan un cuerpo con llaves. Por eso deben devolver explícitamente el resultado de `mapWeighing`.

Se añadieron las palabras `return` en las dos llamadas finales:

```kotlin
return mapWeighing(...)
```

Después de abrir el proyecto en Android Studio, sincroniza Gradle y ejecuta desde la raíz:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

Esta corrección aborda el error `Missing return statement`. No se pudo verificar la compilación completa desde este entorno porque no puede resolver `services.gradle.org` para descargar la distribución de Gradle.
