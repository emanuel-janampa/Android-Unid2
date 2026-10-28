# Corrección de `DashboardViewModel.kt`

El compilador indicaba `Return type mismatch: expected 'String', actual 'String?'` en `friendlyMessage`.

La propiedad `Throwable.message` es nullable (`String?`), por lo que incluso para `ApiFailure` hay que proporcionar un valor alternativo. La función ahora usa:

```kotlin
private fun friendlyMessage(failure: Throwable): String = when (failure) {
    is ApiFailure -> failure.message ?: "Ocurrió un error inesperado."
    else -> failure.message ?: "Ocurrió un error inesperado."
}
```

Se conserva la estructura KMP `androidApp`, `shared` e `iosApp` y la configuración para JDK 21. Abra la carpeta `MyApplication` en Android Studio y ejecute `./gradlew :androidApp:assembleDebug` (en Windows, `gradlew.bat :androidApp:assembleDebug`).
