package pe.upeu.ecorecicla

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.local.SecureSessionStore
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.ApiFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiClientTest {
    private class TestSecureSessionStore : SecureSessionStore {
        private var session: Sesion? = null
        override suspend fun read(): Sesion? = session
        override suspend fun write(session: Sesion) { this.session = session }
        override suspend fun clear() { session = null }
    }

    @Test
    fun loginResponseIsDecodedThroughMockHttpEngine() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"token":"token-demo","tipo":"Bearer","usuario":"operario1","nombre":"Carlos Operario","rol":"OPERARIO","privilegios":["PUNTO_LEER","PESAJE_CREAR"],"expiraEnSegundos":3600}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = false
        }
        try {
            val result = EcoApi(client, "http://unit.test", SessionToken(), TestSecureSessionStore()).login("operario1", "Eco2026*")
            assertEquals("operario1", result.usuario)
            assertEquals("OPERARIO", result.rol)
            assertEquals(3600L, result.expiraEnSegundos)
        } finally {
            client.close()
        }
    }

    @Test
    fun forbiddenHttpResponseMapsToTypedFailure() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"timestamp":"2026-10-28T10:00:00","status":403,"error":"Forbidden","message":"No autorizado","path":"/api/v1/puntos","validationErrors":{}}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = false
        }
        try {
            val failure = assertFailsWith<ApiFailure.Forbidden> {
                EcoApi(client, "http://unit.test", SessionToken(), TestSecureSessionStore()).getPoints()
            }
            assertEquals("No autorizado", failure.message)
        } finally {
            client.close()
        }
    }
}
