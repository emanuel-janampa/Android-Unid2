package pe.upeu.ecorecicla.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.model.makePendingWeighing
import pe.upeu.ecorecicla.domain.model.nextProvisionalId

interface LocalEcoStore {
    fun observePoints(): Flow<List<PuntoAcopio>>
    fun observeRoute(): Flow<RutaDelDia?>
    fun observeWeighings(pointId: Long): Flow<List<Pesaje>>
    fun observePendingCount(): Flow<Long>
    suspend fun savePoints(points: List<PuntoAcopio>)
    suspend fun saveRoute(route: RutaDto)
    suspend fun saveServerWeighings(pointId: Long, weights: List<PesajeDto>)
    suspend fun createPendingWeighing(pointId: Long, input: RegistrarPesaje, username: String): Pesaje
    suspend fun pendingWeighings(): List<Pesaje>
    suspend fun acknowledgePending(localId: Long, serverWeight: PesajeDto)
    suspend fun saveServerWeighing(weight: PesajeDto)
    suspend fun updatePendingWeighing(localId: Long, input: RegistrarPesaje): Pesaje
    suspend fun markWeighingVoidLocally(weighingId: Long, reason: String): Pesaje
}

class SqlDelightLocalEcoStore(private val database: EcoReciclaDatabase) : LocalEcoStore {
    private val q = database.ecoReciclaQueries

    override fun observePoints(): Flow<List<PuntoAcopio>> = q.selectAllPoints()
        .asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.map { PuntoAcopio(it.id, it.razonSocial, it.direccion, it.zona, it.materialHabitual) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRoute(): Flow<RutaDelDia?> = q.selectLatestRoute()
        .asFlow().mapToOneOrNull(Dispatchers.IO)
        .flatMapLatest { route ->
            if (route == null) flowOf(null)
            else q.selectRoutePoints(route.id).asFlow().mapToList(Dispatchers.IO).map { points ->
                RutaDelDia(
                    id = route.id,
                    fecha = route.fecha,
                    estado = route.estado,
                    operario = route.operario,
                    ultimaSincronizacion = route.ultimaSincronizacion,
                    puntos = points.map { row ->
                        PuntoAcopio(
                            id = row.id,
                            razonSocial = row.razonSocial,
                            direccion = row.direccion,
                            zona = row.zona,
                            materialHabitual = row.materialHabitual,
                            secuencia = row.secuencia.toInt(),
                            atendido = row.atendido != 0L,
                            motivoOmision = row.motivoOmision
                        )
                    }
                )
            }
        }

    override fun observeWeighings(pointId: Long): Flow<List<Pesaje>> = q.selectWeighingsByPoint(pointId)
        .asFlow().mapToList(Dispatchers.IO).map { rows -> rows.map { row -> mapWeighing(row.id, row.puntoId, row.rutaId, row.fechaHora, row.material, row.pesoBruto, row.tara, row.pesoNeto, row.observacion, row.operario, row.anulado, row.motivoAnulacion, row.pendiente, row.operacionPendiente) } }

    override fun observePendingCount(): Flow<Long> = q.selectPendingCount()
        .asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0L }

    override suspend fun savePoints(points: List<PuntoAcopio>) {
        database.transaction {
            points.forEach { p -> q.upsertPoint(p.id, p.razonSocial, p.direccion, p.zona, p.materialHabitual) }
        }
    }

    override suspend fun saveRoute(route: RutaDto) {
        val syncedAt = Clock.System.now().toString()
        database.transaction {
            q.upsertRoute(route.id, route.fecha, route.estado, route.operario, syncedAt)
            route.puntos.forEach { point ->
                q.upsertPoint(point.id, point.razonSocial, point.direccion, point.zona, point.materialHabitual)
                q.upsertRoutePoint(route.id, point.id, point.secuencia.toLong(), if (point.atendido) 1L else 0L, point.motivoOmision)
            }
        }
    }

    override suspend fun saveServerWeighings(pointId: Long, weights: List<PesajeDto>) {
        database.transaction {
            weights.forEach { saveWeighingInTransaction(it) }
        }
    }

    override suspend fun createPendingWeighing(pointId: Long, input: RegistrarPesaje, username: String): Pesaje {
        val route = q.selectLatestRoute().executeAsOneOrNull()
            ?: throw ApiFailure.Conflict("Sin una ruta descargada no se puede guardar un pesaje sin conexión. Sincroniza primero.")
        if (route.estado.equals("CERRADA", ignoreCase = true)) {
            throw ApiFailure.Conflict("No se pueden registrar pesajes en una ruta cerrada.")
        }
        val plannedPoint = q.selectRoutePoints(route.id).executeAsList().any { it.id == pointId }
        if (!plannedPoint) {
            throw ApiFailure.Conflict("Este punto no pertenece a la ruta guardada del día.")
        }
        val negativeIds = q.selectPendingWeighings().executeAsList().map { it.id }
        val nextId = nextProvisionalId(negativeIds)
        val row = makePendingWeighing(nextId, pointId, route.id, input, username, Clock.System.now().toString())
        database.transaction {
            q.upsertWeighing(
                row.id, row.puntoId, row.rutaId, row.fechaHora, row.material, row.pesoBruto,
                row.tara, row.pesoNeto, row.observacion, row.operario, 0L, null, 1L, row.operacionPendiente
            )
            q.markRoutePointAttended(row.rutaId, pointId)
        }
        return row
    }

    override suspend fun pendingWeighings(): List<Pesaje> = q.selectPendingWeighings().executeAsList().map { row -> mapWeighing(row.id, row.puntoId, row.rutaId, row.fechaHora, row.material, row.pesoBruto, row.tara, row.pesoNeto, row.observacion, row.operario, row.anulado, row.motivoAnulacion, row.pendiente, row.operacionPendiente) }

    override suspend fun acknowledgePending(localId: Long, serverWeight: PesajeDto) {
        database.transaction {
            q.deleteWeighing(localId)
            saveWeighingInTransaction(serverWeight.copy(pendiente = false))
        }
    }

    override suspend fun saveServerWeighing(weight: PesajeDto) {
        q.upsertWeighing(
            weight.id, weight.puntoId, weight.rutaId, weight.fechaHora, weight.material,
            weight.pesoBruto, weight.tara, weight.pesoNeto, weight.observacion,
            weight.operario, if (weight.anulado) 1L else 0L, weight.motivoAnulacion,
            if (weight.pendiente) 1L else 0L, if (weight.pendiente) "UPDATE" else "NONE"
        )
    }

    override suspend fun updatePendingWeighing(localId: Long, input: RegistrarPesaje): Pesaje {
        val old = q.selectWeighingById(localId).executeAsOneOrNull()
            ?: throw ApiFailure.NotFound("No existe el pesaje local.")
        val operation = if (localId < 0L) "CREATE" else "UPDATE"
        q.upsertWeighing(
            old.id, old.puntoId, old.rutaId, Clock.System.now().toString(), input.material,
            input.pesoBruto, input.tara, input.pesoBruto - input.tara, input.observacion,
            old.operario, old.anulado, old.motivoAnulacion, 1L, operation
        )
        return mapWeighing(old.id, old.puntoId, old.rutaId, Clock.System.now().toString(), input.material,
            input.pesoBruto, input.tara, input.pesoBruto - input.tara, input.observacion,
            old.operario, old.anulado, old.motivoAnulacion, 1L, operation)
    }

    override suspend fun markWeighingVoidLocally(weighingId: Long, reason: String): Pesaje {
        val old = q.selectWeighingById(weighingId).executeAsOneOrNull()
            ?: throw ApiFailure.NotFound("No existe el pesaje local.")
        val operation = if (weighingId < 0L) "CREATE_VOID" else "VOID"
        q.upsertWeighing(
            old.id, old.puntoId, old.rutaId, old.fechaHora, old.material, old.pesoBruto, old.tara,
            old.pesoNeto, old.observacion, old.operario, 1L, reason, 1L, operation
        )
        return mapWeighing(old.id, old.puntoId, old.rutaId, old.fechaHora, old.material, old.pesoBruto,
            old.tara, old.pesoNeto, old.observacion, old.operario, 1L, reason, 1L, operation)
    }

    private fun saveWeighingInTransaction(weight: PesajeDto) {
        q.upsertWeighing(
            weight.id, weight.puntoId, weight.rutaId, weight.fechaHora, weight.material,
            weight.pesoBruto, weight.tara, weight.pesoNeto, weight.observacion,
            weight.operario, if (weight.anulado) 1L else 0L, weight.motivoAnulacion,
            if (weight.pendiente) 1L else 0L, "NONE"
        )
    }

    private fun mapWeighing(
        id: Long,
        puntoId: Long,
        rutaId: Long,
        fechaHora: String,
        material: String,
        pesoBruto: Double,
        tara: Double,
        pesoNeto: Double,
        observacion: String?,
        operario: String,
        anulado: Long,
        motivoAnulacion: String?,
        pendiente: Long,
        operacionPendiente: String
    ): Pesaje = Pesaje(
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
        estado = if (anulado != 0L) EstadoPesaje.Anulado(motivoAnulacion.orEmpty()) else EstadoPesaje.Registrado,
        pendiente = pendiente != 0L,
        operacionPendiente = operacionPendiente
    )
}
