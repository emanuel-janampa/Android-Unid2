package pe.upeu.ecorecicla

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import pe.upeu.ecorecicla.data.local.LocalEcoStore
import pe.upeu.ecorecicla.data.remote.EcoRemoteDataSource
import pe.upeu.ecorecicla.data.remote.dto.AnularPesajeRequestDto
import pe.upeu.ecorecicla.data.remote.dto.CrearPuntoRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaRespuestaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PuntoDto
import pe.upeu.ecorecicla.data.remote.dto.RegistrarPesajeRequestDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.RutaPuntoDto
import pe.upeu.ecorecicla.data.remote.dto.UsuarioActualDto
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.model.makePendingWeighing
import pe.upeu.ecorecicla.domain.model.nextProvisionalId
import pe.upeu.ecorecicla.data.repository.OfflineFirstEcoRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineAndSyncTest {
    @Test
    fun dtoIsConvertedToDomainWithoutLosingRouteSequence() {
        val point = RutaPuntoDto(
            id = 14, razonSocial = "Mercado Verde", direccion = "Av. Central 100", zona = "Norte",
            materialHabitual = "Papel", secuencia = 4, atendido = true, motivoOmision = null
        ).toDomain()
        assertEquals(14L, point.id)
        assertEquals(4, point.secuencia)
        assertTrue(point.atendido)
    }

    @Test
    fun offlineWeighingReceivesNegativeProvisionalIdAndPendingMarker() {
        val id = nextProvisionalId(listOf(5L, 7L, -1L, -2L))
        val weighing = makePendingWeighing(
            localId = id,
            pointId = 14,
            routeId = 2,
            input = RegistrarPesaje("Papel", 12.5, 2.5, "Sin cobertura"),
            username = "operario1",
            timestamp = "2026-10-28T10:00:00Z"
        )
        assertEquals(-3L, weighing.id)
        assertTrue(weighing.pendiente)
        assertEquals(10.0, weighing.pesoNeto)
    }

    @Test
    fun repositoryStoresNewWeighingOfflineUsingInMemoryLocalStore() = runTest {
        val local = InMemoryEcoStore().apply { saveRoute(demoRoute()) }
        val remote = FakeEcoRemote().apply { offline = true }
        val repository = OfflineFirstEcoRepository(remote, local) { "operario1" }

        val weighing = repository.registerWeighing(14L, RegistrarPesaje("Papel", 12.5, 2.5, "Sin cobertura"))

        assertTrue(weighing.pendiente)
        assertTrue(weighing.id < 0)
        assertEquals(1, local.pendingWeighings().size)
        assertEquals(10.0, local.pendingWeighings().single().pesoNeto)
    }

    @Test
    fun synchronizeUploadsPendingRowsBeforeDownloadingUpdatedState() = runTest {
        val local = InMemoryEcoStore().apply { saveRoute(demoRoute()) }
        local.createPendingWeighing(14L, RegistrarPesaje("Papel", 10.0, 1.0, "offline"), "operario1")
        val remote = FakeEcoRemote()
        val repository = OfflineFirstEcoRepository(remote, local) { "operario1" }

        val result = repository.synchronize()

        assertEquals(listOf("upload", "route", "points"), remote.events)
        assertEquals(1, result.enviados)
        assertEquals(0, result.pendientes)
        assertFalse(local.pendingWeighings().any())
    }

    private fun demoRoute() = RutaDto(
        id = 2,
        fecha = "2026-10-28",
        estado = "ABIERTA",
        operario = "operario1",
        puntos = listOf(
            RutaPuntoDto(14, "Mercado Verde", "Av. Central 100", "Norte", "Papel", 1, false, null)
        )
    )

    private class FakeEcoRemote : EcoRemoteDataSource {
        var offline: Boolean = false
        val events = mutableListOf<String>()
        private var nextId = 101L
        private fun online() { if (offline) throw ApiFailure.Offline("Sin conexión") }

        override suspend fun login(usuario: String, contrasena: String) = LoginResponseDto("test-token", usuario = usuario, nombre = "Operario", rol = "OPERARIO", privilegios = setOf("PUNTO_LEER", "PESAJE_CREAR"))
        override suspend fun me() = UsuarioActualDto(1, "operario1", "Operario", "OPERARIO", setOf("PUNTO_LEER", "PESAJE_CREAR"))
        override suspend fun getRoute(fecha: String?): RutaDto {
            events += "route"; online(); return demoRoute()
        }
        override suspend fun getPoints(page: Int, size: Int, zona: String?): PaginaRespuestaDto<PuntoDto> {
            events += "points"; online()
            val points = listOf(PuntoDto(14, "Mercado Verde", "Av. Central 100", "Norte", "Papel"))
            return PaginaRespuestaDto(points, 0, size, points.size.toLong(), 1, true)
        }
        override suspend fun getWeighings(pointId: Long, page: Int, size: Int): PaginaRespuestaDto<PesajeDto> {
            online(); return PaginaRespuestaDto(emptyList(), page, size, 0, 0, true)
        }
        override suspend fun registerWeighing(pointId: Long, request: RegistrarPesajeRequestDto): PesajeDto {
            events += "upload"; online()
            return PesajeDto(nextId++, pointId, 2, "2026-10-28T10:00:00Z", request.material, request.pesoBruto, request.tara, request.pesoBruto - request.tara, request.observacion, "operario1")
        }
        override suspend fun correctWeighing(pesajeId: Long, request: RegistrarPesajeRequestDto): PesajeDto {
            online(); return PesajeDto(pesajeId, 14, 2, "2026-10-28T10:00:00Z", request.material, request.pesoBruto, request.tara, request.pesoBruto - request.tara, request.observacion, "operario1")
        }
        override suspend fun voidWeighing(pesajeId: Long, motivo: String): PesajeDto {
            online(); return PesajeDto(pesajeId, 14, 2, "2026-10-28T10:00:00Z", "Papel", 10.0, 1.0, 9.0, null, "operario1", true, motivo)
        }
        override suspend fun createPoint(request: CrearPuntoRequestDto): PuntoDto {
            online(); return PuntoDto(99, request.razonSocial, request.direccion, request.zona, request.materialHabitual)
        }
        override suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String): RutaDto { online(); return demoRoute() }
        override suspend fun closeRoute(routeId: Long): RutaDto { online(); return demoRoute().copy(estado = "CERRADA") }
    }

    private class InMemoryEcoStore : LocalEcoStore {
        private val _points = MutableStateFlow<List<PuntoAcopio>>(emptyList())
        private val _route = MutableStateFlow<RutaDelDia?>(null)
        private val _weighings = MutableStateFlow<List<Pesaje>>(emptyList())
        override fun observePoints(): Flow<List<PuntoAcopio>> = _points
        override fun observeRoute(): Flow<RutaDelDia?> = _route
        override fun observeWeighings(pointId: Long): Flow<List<Pesaje>> = _weighings.map { list -> list.filter { it.puntoId == pointId }.sortedByDescending { it.fechaHora } }
        override fun observePendingCount(): Flow<Long> = _weighings.map { it.count { item -> item.pendiente }.toLong() }
        override suspend fun savePoints(points: List<PuntoAcopio>) {
            _points.value = (_points.value.filterNot { current -> points.any { it.id == current.id } } + points).sortedBy { it.razonSocial }
        }
        override suspend fun saveRoute(route: RutaDto) {
            val domain = route.toDomain("2026-10-28T10:00:00Z")
            _route.value = domain
            savePoints(domain.puntos)
        }
        override suspend fun saveServerWeighings(pointId: Long, weights: List<PesajeDto>) {
            weights.forEach { saveServerWeighing(it) }
        }
        override suspend fun createPendingWeighing(pointId: Long, input: RegistrarPesaje, username: String): Pesaje {
            val route = _route.value ?: throw ApiFailure.Conflict("No hay ruta local")
            val id = nextProvisionalId(_weighings.value.map { it.id })
            val row = makePendingWeighing(id, pointId, route.id, input, username, "2026-10-28T10:00:00Z")
            _weighings.value = _weighings.value + row
            _route.value = route.copy(puntos = route.puntos.map { if (it.id == pointId) it.copy(atendido = true) else it })
            return row
        }
        override suspend fun pendingWeighings(): List<Pesaje> = _weighings.value.filter { it.pendiente }.sortedBy { it.fechaHora }
        override suspend fun acknowledgePending(localId: Long, serverWeight: PesajeDto) {
            _weighings.value = _weighings.value.filterNot { it.id == localId } + serverWeight.toDomain()
        }
        override suspend fun saveServerWeighing(weight: PesajeDto) {
            _weighings.value = _weighings.value.filterNot { it.id == weight.id } + weight.toDomain()
        }
        override suspend fun updatePendingWeighing(localId: Long, input: RegistrarPesaje): Pesaje {
            val old = _weighings.value.firstOrNull { it.id == localId } ?: throw ApiFailure.NotFound()
            val new = old.copy(material = input.material, pesoBruto = input.pesoBruto, tara = input.tara, pesoNeto = input.pesoBruto - input.tara, observacion = input.observacion, pendiente = true, operacionPendiente = if (localId < 0) "CREATE" else "UPDATE")
            _weighings.value = _weighings.value.filterNot { it.id == localId } + new
            return new
        }
        override suspend fun markWeighingVoidLocally(weighingId: Long, reason: String): Pesaje {
            val old = _weighings.value.firstOrNull { it.id == weighingId } ?: throw ApiFailure.NotFound()
            val new = old.copy(estado = EstadoPesaje.Anulado(reason), pendiente = true, operacionPendiente = if (weighingId < 0) "CREATE_VOID" else "VOID")
            _weighings.value = _weighings.value.filterNot { it.id == weighingId } + new
            return new
        }
    }
}
