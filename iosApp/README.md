# iosApp

Carpeta reservada para el host SwiftUI en la estructura Kotlin Multiplatform del curso. El objetivo del examen entregado es la aplicación Android, por lo que no se ha incluido un proyecto Xcode ejecutable en Windows. El código común de dominio/datos/UI está en `../shared/src/commonMain`.


### Nota de compatibilidad iOS

El módulo `shared` declara `iosArm64` y `iosSimulatorArm64`. No declara `iosX64` porque Compose Multiplatform 1.11.1 y posteriores eliminaron el soporte para Apple x86_64. La estructura `iosApp` se conserva. La evaluación de esta entrega está centrada en Android; compilar iOS requiere macOS y Xcode.
