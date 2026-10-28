package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.ResultadoSincronizacion
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.UsuarioActual

interface AuthRepository {
    suspend fun login(usuario: String, contrasena: String): Sesion
    suspend fun restoreSession(): Sesion?
    suspend fun logout()
    suspend fun currentUser(): UsuarioActual
}

interface EcoRepository {
    fun observeRoute(): Flow<RutaDelDia?>
    fun observePoints(): Flow<List<PuntoAcopio>>
    fun observeWeighings(pointId: Long): Flow<List<Pesaje>>
    fun observePendingCount(): Flow<Long>
    suspend fun refreshRoute()
    suspend fun refreshPoints(zona: String? = null)
    suspend fun refreshWeighings(pointId: Long)
    suspend fun registerWeighing(pointId: Long, input: RegistrarPesaje): Pesaje
    suspend fun correctWeighing(pesajeId: Long, input: RegistrarPesaje): Pesaje
    suspend fun voidWeighing(pesajeId: Long, motivo: String): Pesaje
    suspend fun createPoint(input: CrearPunto): PuntoAcopio
    suspend fun closeRoute(routeId: Long)
    suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String)
    suspend fun synchronize(): ResultadoSincronizacion
}
