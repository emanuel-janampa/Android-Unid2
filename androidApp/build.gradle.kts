plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "pe.upeu.ecorecicla"
    compileSdk = 36

    defaultConfig {
        applicationId = "pe.upeu.ecorecicla"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation(compose.runtime)
    implementation(compose.ui)
}
