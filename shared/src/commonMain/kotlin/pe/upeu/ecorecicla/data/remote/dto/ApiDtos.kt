package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.UsuarioActual

@Serializable
data class LoginRequestDto(val usuario: String, val contrasena: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String = "Bearer",
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
    val expiraEnSegundos: Long = 3600
) {
    fun toDomain() = Sesion(token, usuario, nombre, rol, privilegios, expiraEnSegundos)
}

@Serializable
data class UsuarioActualDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
) {
    fun toDomain() = UsuarioActual(id, usuario, nombre, rol, privilegios)
    fun toSession(token: String) = Sesion(token, usuario, nombre, rol, privilegios)
}

@Serializable
data class ApiErrorDto(
    val timestamp: String = "",
    val status: Int = 0,
    val error: String = "Error",
    val message: String = "La solicitud no pudo completarse.",
    val path: String = "",
    val validationErrors: Map<String, String> = emptyMap()
)

@Serializable
data class PaginaRespuestaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

@Serializable
data class PuntoDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
) {
    fun toDomain() = PuntoAcopio(id, razonSocial, direccion, zona, materialHabitual)
}

@Serializable
data class CrearPuntoRequestDto(
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
)

fun CrearPunto.toDto() = CrearPuntoRequestDto(razonSocial, direccion, zona, materialHabitual)

@Serializable
data class RutaPuntoDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean,
    val motivoOmision: String? = null
) {
    fun toDomain() = PuntoAcopio(id, razonSocial, direccion, zona, materialHabitual, secuencia, atendido, motivoOmision)
}

@Serializable
data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val puntos: List<RutaPuntoDto>
) {
    fun toDomain(ultimaSincronizacion: String? = null) = RutaDelDia(
        id = id,
        fecha = fecha,
        estado = estado,
        operario = operario,
        puntos = puntos.sortedBy { it.secuencia }.map { it.toDomain() },
        ultimaSincronizacion = ultimaSincronizacion
    )
}

@Serializable
data class RegistrarPesajeRequestDto(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null
)

fun RegistrarPesaje.toDto() = RegistrarPesajeRequestDto(material.trim(), pesoBruto, tara, observacion?.trim()?.ifBlank { null })

@Serializable
data class AnularPesajeRequestDto(val motivo: String)

@Serializable
data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val rutaId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val observacion: String? = null,
    val operario: String,
    val anulado: Boolean = false,
    val motivoAnulacion: String? = null,
    val pendiente: Boolean = false
) {
    fun toDomain(localPending: Boolean = false) = Pesaje(
        id = id,
        puntoId = puntoId,
        rutaId = rutaId,
        fechaHora = fechaHora,
        material = material,
        pesoBruto = pesoBruto,
        tara = tara,
        pesoNeto = pesoNeto,
        observacion = observacion,
        operario = operario,
        estado = if (anulado) EstadoPesaje.Anulado(motivoAnulacion.orEmpty()) else EstadoPesaje.Registrado,
        pendiente = localPending || pendiente,
        operacionPendiente = if (localPending || pendiente) "CREATE" else "NONE"
    )
}
