package pe.upeu.ecorecicla.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Sesion(
    val token: String,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>,
    val expiraEnSegundos: Long = 3600
)

@Serializable
data class UsuarioActual(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String>
)

@Serializable
data class PuntoAcopio(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int = 0,
    val atendido: Boolean = false,
    val motivoOmision: String? = null
)

@Serializable
data class RutaDelDia(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val puntos: List<PuntoAcopio>,
    val ultimaSincronizacion: String? = null
)

sealed interface EstadoPesaje {
    data object Registrado : EstadoPesaje
    data class Anulado(val motivo: String) : EstadoPesaje
}

data class Pesaje(
    val id: Long,
    val puntoId: Long,
    val rutaId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val observacion: String?,
    val operario: String,
    val estado: EstadoPesaje,
    val pendiente: Boolean,
    val operacionPendiente: String = "NONE"
)

data class RegistrarPesaje(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null
)

data class CrearPunto(
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String
)

data class ResultadoSincronizacion(
    val enviados: Int,
    val pendientes: Int,
    val mensaje: String
)

/** Produces client-only negative IDs that cannot collide with positive server IDs. */
fun nextProvisionalId(existingIds: Iterable<Long>): Long =
    (existingIds.filter { it < 0L }.minOrNull() ?: 0L) - 1L

fun makePendingWeighing(
    localId: Long, pointId: Long, routeId: Long, input: RegistrarPesaje, username: String, timestamp: String
): Pesaje = Pesaje(
    id = localId, puntoId = pointId, rutaId = routeId, fechaHora = timestamp, material = input.material,
    pesoBruto = input.pesoBruto, tara = input.tara, pesoNeto = input.pesoBruto - input.tara,
    observacion = input.observacion, operario = username, estado = EstadoPesaje.Registrado, pendiente = true,
    operacionPendiente = "CREATE"
)
