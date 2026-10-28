package pe.upeu.ecorecicla.data.repository

import io.ktor.client.plugins.HttpRequestTimeoutException
import pe.upeu.ecorecicla.data.local.SecureSessionStore
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.data.remote.EcoRemoteDataSource
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.UsuarioActual
import pe.upeu.ecorecicla.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: EcoRemoteDataSource,
    private val storage: SecureSessionStore,
    private val sessionToken: SessionToken
) : AuthRepository {
    override suspend fun login(usuario: String, contrasena: String): Sesion = safeApiCall {
        val session = api.login(usuario.trim(), contrasena).toDomain()
        storage.write(session)
        sessionToken.update(session)
        session
    }

    override suspend fun restoreSession(): Sesion? {
        val cached = storage.read() ?: return null
        sessionToken.update(cached)
        return try {
            val user = api.me()
            user.toSession(cached.token).also { storage.write(it); sessionToken.update(it) }
        } catch (failure: ApiFailure.Unauthorized) {
            logout()
            null
        } catch (_: ApiFailure.Offline) {
            cached
        } catch (_: ApiFailure.Timeout) {
            cached
        } catch (_: Exception) {
            cached
        }
    }

    override suspend fun logout() {
        storage.clear()
        sessionToken.clear()
    }

    override suspend fun currentUser(): UsuarioActual = safeApiCall { api.me().toDomain() }
}

internal suspend fun <T> safeApiCall(block: suspend () -> T): T = try {
    block()
} catch (failure: ApiFailure) {
    throw failure
} catch (timeout: HttpRequestTimeoutException) {
    throw ApiFailure.Timeout(timeout.message ?: "Tiempo de espera agotado.")
} catch (failure: Exception) {
    throw ApiFailure.Offline(failure.message ?: "No se pudo conectar con EcoAPI.")
}
