package pe.upeu.ecorecicla.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import pe.upeu.ecorecicla.domain.model.Sesion

/** Secure session persistence is implemented with Android Keystore in androidMain. */
interface SecureSessionStore {
    suspend fun read(): Sesion?
    suspend fun write(session: Sesion)
    suspend fun clear()
}

class SessionToken {
    var value: String? = null
    var username: String? = null
    val sessionFlow = MutableStateFlow<Sesion?>(null)

    fun update(session: Sesion) {
        value = session.token
        username = session.usuario
        sessionFlow.value = session
    }

    fun clear() {
        value = null
        username = null
        sessionFlow.value = null
    }
}
