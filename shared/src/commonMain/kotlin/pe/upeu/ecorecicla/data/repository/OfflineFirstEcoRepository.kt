package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.usecase.SynchronizationOrder
import kotlinx.datetime.Clock
import pe.upeu.ecorecicla.data.local.LocalEcoStore
import pe.upeu.ecorecicla.data.remote.EcoRemoteDataSource
import pe.upeu.ecorecicla.data.remote.dto.toDto
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.ResultadoSincronizacion
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.repository.EcoRepository

class OfflineFirstEcoRepository(
    private val api: EcoRemoteDataSource,
    private val local: LocalEcoStore,
    private val activeSession: () -> String
) : EcoRepository {
    override fun observeRoute(): Flow<RutaDelDia?> = local.observeRoute()
    override fun observePoints(): Flow<List<PuntoAcopio>> = local.observePoints()
    override fun observeWeighings(pointId: Long): Flow<List<Pesaje>> = local.observeWeighings(pointId)
    override fun observePendingCount(): Flow<Long> = local.observePendingCount()

    override suspend fun refreshRoute() = safeApiCall {
        local.saveRoute(api.getRoute())
    }

    override suspend fun refreshPoints(zona: String?) = safeApiCall {
        val page = api.getPoints(page = 0, size = 100, zona = zona)
        local.savePoints(page.contenido.map { it.toDomain() })
    }

    override suspend fun refreshWeighings(pointId: Long) = safeApiCall {
        val page = api.getWeighings(pointId, page = 0, size = 100)
        local.saveServerWeighings(pointId, page.contenido)
    }

    override suspend fun registerWeighing(pointId: Long, input: RegistrarPesaje): Pesaje {
        validateWeight(input)
        val created = local.createPendingWeighing(pointId, input, activeSession())
        // First persist locally. If there's no network, the entry remains with a negative ID and pending=true.
        val report = synchronize()
        return if (report.pendientes == 0 && report.enviados > 0) created.copy(pendiente = false) else created
    }

    override suspend fun correctWeighing(pesajeId: Long, input: RegistrarPesaje): Pesaje {
        validateWeight(input)
        if (pesajeId < 0) return local.updatePendingWeighing(pesajeId, input)
        return try {
            safeApiCall {
                api.correctWeighing(pesajeId, input.toDto()).also { local.saveServerWeighing(it) }.toDomain()
            }
        } catch (failure: ApiFailure.Offline) {
            local.updatePendingWeighing(pesajeId, input)
        } catch (failure: ApiFailure.Timeout) {
            local.updatePendingWeighing(pesajeId, input)
        }
    }

    override suspend fun voidWeighing(pesajeId: Long, motivo: String): Pesaje {
        if (motivo.isBlank()) throw ApiFailure.Validation("Debes indicar el motivo de anulación.")
        if (pesajeId < 0) return local.markWeighingVoidLocally(pesajeId, motivo.trim())
        return try {
            safeApiCall {
                api.voidWeighing(pesajeId, motivo.trim()).also { local.saveServerWeighing(it) }.toDomain()
            }
        } catch (failure: ApiFailure.Offline) {
            local.markWeighingVoidLocally(pesajeId, motivo.trim())
        } catch (failure: ApiFailure.Timeout) {
            local.markWeighingVoidLocally(pesajeId, motivo.trim())
        }
    }

    override suspend fun createPoint(input: CrearPunto): PuntoAcopio = safeApiCall {
        val point = api.createPoint(input.toDto()).toDomain()
        local.savePoints(listOf(point))
        point
    }

    override suspend fun closeRoute(routeId: Long) {
        safeApiCall { local.saveRoute(api.closeRoute(routeId)) }
    }

    override suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String) {
        safeApiCall { local.saveRoute(api.omitPoint(routeId, pointId, motivo.trim())) }
    }

    /** Upload local pending rows before downloading server state, as required by the exam. */
    override suspend fun synchronize(): ResultadoSincronizacion {
        var sent = 0
        var failures = 0
        val pending = runCatching { local.pendingWeighings() }.getOrElse { emptyList() }
        SynchronizationOrder().execute(
            uploadPending = {
                pending.forEach { item ->
                    try {
                        val response = safeApiCall {
                            val request = RegistrarPesaje(item.material, item.pesoBruto, item.tara, item.observacion).toDto()
                            when (item.operacionPendiente) {
                                "UPDATE" -> api.correctWeighing(item.id, request)
                                "VOID" -> api.voidWeighing(item.id, (item.estado as? pe.upeu.ecorecicla.domain.model.EstadoPesaje.Anulado)?.motivo ?: "Anulación offline")
                                "CREATE_VOID" -> {
                                    val created = api.registerWeighing(item.puntoId, request)
                                    api.voidWeighing(created.id, (item.estado as? pe.upeu.ecorecicla.domain.model.EstadoPesaje.Anulado)?.motivo ?: "Anulación offline")
                                }
                                else -> api.registerWeighing(item.puntoId, request)
                            }
                        }
                        local.acknowledgePending(item.id, response)
                        sent++
                    } catch (_: Exception) {
                        failures++
                    }
                }
                sent
            },
            downloadLatest = {
                try { refreshRoute() } catch (_: Exception) { failures++ }
                try { refreshPoints() } catch (_: Exception) { failures++ }
            }
        )

        val remaining = runCatching { local.pendingWeighings().size }.getOrDefault(failures)
        val message = when {
            pending.isEmpty() && failures == 0 -> "Todo está sincronizado."
            sent > 0 && remaining == 0 -> "Sincronización completada: $sent registro(s) enviado(s)."
            sent > 0 -> "Se enviaron $sent registro(s); quedan $remaining pendiente(s)."
            remaining > 0 -> "Sin conexión o conflicto: $remaining registro(s) siguen pendientes."
            else -> "Sincronización finalizada; revisa la conexión y vuelve a intentar."
        }
        return ResultadoSincronizacion(sent, remaining, message)
    }

    private fun validateWeight(input: RegistrarPesaje) {
        if (input.material.isBlank()) throw ApiFailure.Validation("El material es obligatorio.", mapOf("material" to "El material es obligatorio."))
        if (input.pesoBruto <= input.tara) throw ApiFailure.Validation(
            "El peso neto debe ser mayor que cero.", mapOf("pesoBruto" to "El peso bruto debe ser mayor que la tara.")
        )
        if (input.tara < 0) throw ApiFailure.Validation("La tara no puede ser negativa.", mapOf("tara" to "La tara no puede ser negativa."))
    }
}
