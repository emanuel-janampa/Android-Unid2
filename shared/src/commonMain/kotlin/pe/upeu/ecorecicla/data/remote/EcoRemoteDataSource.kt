package pe.upeu.ecorecicla.data.remote

import pe.upeu.ecorecicla.data.remote.dto.CrearPuntoRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaRespuestaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PuntoDto
import pe.upeu.ecorecicla.data.remote.dto.RegistrarPesajeRequestDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.UsuarioActualDto

/** Remote contract makes the data layer testable with Ktor MockEngine or a fake server. */
interface EcoRemoteDataSource {
    suspend fun login(usuario: String, contrasena: String): LoginResponseDto
    suspend fun me(): UsuarioActualDto
    suspend fun getRoute(fecha: String? = null): RutaDto
    suspend fun getPoints(page: Int = 0, size: Int = 100, zona: String? = null): PaginaRespuestaDto<PuntoDto>
    suspend fun getWeighings(pointId: Long, page: Int = 0, size: Int = 100): PaginaRespuestaDto<PesajeDto>
    suspend fun registerWeighing(pointId: Long, request: RegistrarPesajeRequestDto): PesajeDto
    suspend fun correctWeighing(pesajeId: Long, request: RegistrarPesajeRequestDto): PesajeDto
    suspend fun voidWeighing(pesajeId: Long, motivo: String): PesajeDto
    suspend fun createPoint(request: CrearPuntoRequestDto): PuntoDto
    suspend fun omitPoint(routeId: Long, pointId: Long, motivo: String): RutaDto
    suspend fun closeRoute(routeId: Long): RutaDto
}
