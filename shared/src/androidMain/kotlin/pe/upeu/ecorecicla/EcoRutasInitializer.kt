package pe.upeu.ecorecicla

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pe.upeu.ecorecicla.data.local.ConnectivityMonitor
import pe.upeu.ecorecicla.data.local.EcoReciclaDatabase
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.di.AndroidConnectivityMonitor
import pe.upeu.ecorecicla.di.AndroidSecureSessionStore
import pe.upeu.ecorecicla.di.appModule

object EcoRutasInitializer {
    fun initialize(application: Application) {
        val sessionToken = SessionToken()
        val driver = AndroidSqliteDriver(EcoReciclaDatabase.Schema, application, "ecorecicla-local.db")
        val database = EcoReciclaDatabase(driver)
        val secureStore = AndroidSecureSessionStore(application)
        val connectivityMonitor: ConnectivityMonitor = AndroidConnectivityMonitor(application)
        val client = createHttpClient(sessionToken)

        startKoin {
            androidContext(application)
            modules(appModule(client, database, secureStore, sessionToken, connectivityMonitor))
        }
    }

    private fun createHttpClient(sessionToken: SessionToken): HttpClient = HttpClient(Android) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        defaultRequest {
            header(HttpHeaders.Accept, "application/json")
            sessionToken.value?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
