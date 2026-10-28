$ErrorActionPreference = 'Stop'

Write-Host 'Deteniendo procesos Gradle...' -ForegroundColor Cyan
& .\gradlew.bat --stop

foreach ($path in @('.gradle', 'shared\build', 'androidApp\build')) {
    if (Test-Path $path) {
        Write-Host "Eliminando caché/salidas locales: $path" -ForegroundColor Yellow
        Remove-Item $path -Recurse -Force
    }
}

Write-Host 'Compilando la app Android...' -ForegroundColor Cyan
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw 'La compilación falló. Copia el primer error de Gradle para diagnosticarlo.'
}

Write-Host 'BUILD SUCCESSFUL: APK compilado.' -ForegroundColor Green
Write-Host 'Busca el APK en androidApp\build\outputs\apk\debug\androidApp-debug.apk'
