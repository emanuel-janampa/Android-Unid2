# Solución del error KMP `appleMain` / `iosX64`

Esta entrega declara solo estos destinos iOS en `shared/build.gradle.kts`:

```kotlin
listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
        baseName = "Shared"
        isStatic = true
    }
}
```

Compose Multiplatform 1.11.1 y versiones posteriores retiraron el destino Apple x86_64 (`iosX64`). La evaluación de este proyecto se centra en Android, pero se conserva la estructura `androidApp`, `shared`, `iosApp` y los destinos iOS compatibles.

## Muy importante: abrir una copia limpia

1. Cierra Android Studio.
2. Extrae este ZIP en una carpeta NUEVA, por ejemplo `C:\Users\<usuario>\AndroidStudioProjects\MyApplication-Final`. No extraigas encima de un proyecto viejo.
3. En Android Studio abre la carpeta `MyApplication` que contiene `settings.gradle.kts`.
4. En `shared/build.gradle.kts`, confirma que el bloque `listOf(...)` no contiene `iosX64()` ni se usa el atajo `ios()`.
5. Ejecuta desde PowerShell, situado en la raíz del proyecto:

```powershell
.\gradlew.bat --stop
if (Test-Path .gradle) { Remove-Item .gradle -Recurse -Force }
if (Test-Path shared\build) { Remove-Item shared\build -Recurse -Force }
if (Test-Path androidApp\build) { Remove-Item androidApp\build -Recurse -Force }
.\gradlew.bat :androidApp:assembleDebug
```

Estos pasos borran solo la caché local del proyecto y sus salidas de compilación. No eliminan el caché global de Gradle ni tus otros proyectos.

Si el IDE continúa mostrando el mismo error, selecciona `File > Invalidate Caches... > Invalidate and Restart` y vuelve a sincronizar el proyecto. Si el error persiste incluso compilando desde PowerShell, verifica que la terminal esté en la carpeta extraída nueva y no en un proyecto anterior.

## Comprobación de destinos

Desde la raíz del proyecto, ejecuta:

```powershell
Get-ChildItem -Recurse -Include *.gradle,*.gradle.kts | Select-String -Pattern 'iosX64\s*\(|\bios\s*\('
```

No debe aparecer una declaración de destino `iosX64()` ni un destino abreviado `ios()` en los archivos Gradle.
