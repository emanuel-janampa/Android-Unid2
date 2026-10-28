package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.EcoRepository

class LoginUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(usuario: String, contrasena: String) = auth.login(usuario, contrasena)
}

class RestoreSessionUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.restoreSession()
}

class ObserveRouteUseCase(private val repository: EcoRepository) {
    operator fun invoke() = repository.observeRoute()
}

class RegisterWeighingUseCase(private val repository: EcoRepository) {
    suspend operator fun invoke(pointId: Long, input: RegistrarPesaje) = repository.registerWeighing(pointId, input)
}

class SynchronizeUseCase(private val repository: EcoRepository) {
    suspend operator fun invoke() = repository.synchronize()
}

class CreatePointUseCase(private val repository: EcoRepository) {
    suspend operator fun invoke(input: CrearPunto) = repository.createPoint(input)
}
/** Coordinates dashboard actions in the domain layer; presentation does not call the data repository directly. */
class DashboardUseCases(private val repository: EcoRepository) {
    fun observeRoute() = repository.observeRoute()
    fun observePoints() = repository.observePoints()
    fun observeWeighings(pointId: Long) = repository.observeWeighings(pointId)
    fun observePendingCount() = repository.observePendingCount()
    suspend fun refreshRoute() = repository.refreshRoute()
    suspend fun refreshPoints(zona: String? = null) = repository.refreshPoints(zona)
    suspend fun refreshWeighings(pointId: Long) = repository.refreshWeighings(pointId)
    suspend fun registerWeighing(pointId: Long, input: RegistrarPesaje) = repository.registerWeighing(pointId, input)
    suspend fun correctWeighing(pesajeId: Long, input: RegistrarPesaje) = repository.correctWeighing(pesajeId, input)
    suspend fun voidWeighing(pesajeId: Long, motivo: String) = repository.voidWeighing(pesajeId, motivo)
    suspend fun createPoint(input: CrearPunto) = repository.createPoint(input)
    suspend fun closeRoute(routeId: Long) = repository.closeRoute(routeId)
    suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String) = repository.omitPoint(routeId, pointId, motivo)
    suspend fun synchronize() = repository.synchronize()
}

