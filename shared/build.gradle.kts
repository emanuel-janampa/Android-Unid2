import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // Keep the shared module ready for the iOS host in the standard KMP structure.
    // Compose Multiplatform 1.11+ does not support Apple's x86_64 target (iosX64).
    // Keep the supported iOS device and Apple Silicon simulator targets only.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

            implementation("io.ktor:ktor-client-core:3.6.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
            implementation("io.ktor:ktor-client-logging:3.6.0")

            implementation("io.insert-koin:koin-core:4.2.2")

            implementation("app.cash.sqldelight:runtime:2.4.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.4.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:3.6.0")
            implementation("app.cash.sqldelight:android-driver:2.4.0")
            implementation("io.insert-koin:koin-android:4.2.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.6.0")
            implementation("app.cash.sqldelight:native-driver:2.4.0")
        }
    }
}

android {
    namespace = "pe.upeu.ecorecicla.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

sqldelight {
    databases {
        create("EcoReciclaDatabase") {
            packageName.set("pe.upeu.ecorecicla.data.local")
        }
    }
}
