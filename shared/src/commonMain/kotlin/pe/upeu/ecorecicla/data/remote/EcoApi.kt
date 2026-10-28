package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.client.statement.HttpResponse
import pe.upeu.ecorecicla.data.local.SecureSessionStore
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.data.remote.dto.AnularPesajeRequestDto
import pe.upeu.ecorecicla.data.remote.dto.ApiErrorDto
import pe.upeu.ecorecicla.data.remote.dto.CrearPuntoRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaRespuestaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PuntoDto
import pe.upeu.ecorecicla.data.remote.dto.RegistrarPesajeRequestDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.UsuarioActualDto
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.failureForStatus

class EcoApi(
    private val client: HttpClient,
    private val baseUrl: String,
    private val sessionToken: SessionToken,
    private val sessionStore: SecureSessionStore
) : EcoRemoteDataSource {
    private fun endpoint(path: String) = baseUrl.trimEnd('/') + path

    override suspend fun login(usuario: String, contrasena: String): LoginResponseDto = decode(
        client.post(endpoint("/api/v1/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(usuario, contrasena))
        }
    )

    override suspend fun me(): UsuarioActualDto = decode(client.get(endpoint("/api/v1/auth/me")))

    override suspend fun getRoute(fecha: String?): RutaDto = decode(
        client.get(endpoint("/api/v1/rutas/dia")) {
            if (fecha != null) parameter("fecha", fecha)
        }
    )

    override suspend fun getPoints(page: Int, size: Int, zona: String?): PaginaRespuestaDto<PuntoDto> = decode(
        client.get(endpoint("/api/v1/puntos")) {
            parameter("page", page)
            parameter("size", size)
            if (!zona.isNullOrBlank()) parameter("zona", zona)
        }
    )

    override suspend fun getWeighings(pointId: Long, page: Int, size: Int): PaginaRespuestaDto<PesajeDto> = decode(
        client.get(endpoint("/api/v1/puntos/$pointId/pesajes")) {
            parameter("page", page)
            parameter("size", size)
        }
    )

    override suspend fun registerWeighing(pointId: Long, request: RegistrarPesajeRequestDto): PesajeDto = decode(
        client.post(endpoint("/api/v1/puntos/$pointId/pesajes")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    )

    override suspend fun correctWeighing(pesajeId: Long, request: RegistrarPesajeRequestDto): PesajeDto = decode(
        client.put(endpoint("/api/v1/pesajes/$pesajeId")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    )

    override suspend fun voidWeighing(pesajeId: Long, motivo: String): PesajeDto = decode(
        client.patch(endpoint("/api/v1/pesajes/$pesajeId/anular")) {
            contentType(ContentType.Application.Json)
            setBody(AnularPesajeRequestDto(motivo))
        }
    )

    override suspend fun createPoint(request: CrearPuntoRequestDto): PuntoDto = decode(
        client.post(endpoint("/api/v1/puntos")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    )

    override suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String): RutaDto = decode(
        client.put(endpoint("/api/v1/rutas/$routeId/puntos/$pointId/omision")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("motivo" to motivo))
        }
    )

    override suspend fun closeRoute(routeId: Long): RutaDto = decode(client.post(endpoint("/api/v1/rutas/$routeId/cerrar")))

    private suspend inline fun <reified T : Any> decode(response: HttpResponse): T {
        if (response.status.value in 200..299) return response.body<T>()
        if (response.status == HttpStatusCode.Unauthorized) {
            sessionToken.clear()
            sessionStore.clear()
        }
        val error = runCatching { response.body<ApiErrorDto>() }.getOrNull()
        throw failureForStatus(
            response.status.value,
            error?.message ?: response.status.description.ifBlank { "Error HTTP ${response.status.value}" },
            error?.validationErrors ?: emptyMap()
        )
    }
}
