package pe.upeu.ecorecicla.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository

 data class LoginUiState(
    val busy: Boolean = true,
    val session: Sesion? = null,
    val error: String? = null
)

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionToken: SessionToken
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionToken.sessionFlow.collect { session ->
                _uiState.value = _uiState.value.copy(session = session)
            }
        }
        viewModelScope.launch {
            val restored = runCatching { repository.restoreSession() }.getOrNull()
            _uiState.value = LoginUiState(busy = false, session = restored)
        }
    }

    fun login(usuario: String, contrasena: String) {
        if (usuario.isBlank() || contrasena.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Escribe tu usuario y contraseña.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val session = repository.login(usuario, contrasena)
                _uiState.value = LoginUiState(busy = false, session = session)
            } catch (failure: Exception) {
                _uiState.value = LoginUiState(
                    busy = false,
                    error = when (failure) {
                        is ApiFailure.Unauthorized -> "Usuario o contraseña incorrectos."
                        is ApiFailure.Offline -> "No hay conexión con EcoAPI. Comprueba que el backend esté encendido."
                        is ApiFailure.Timeout -> "El servidor tardó demasiado en responder. Inténtalo otra vez."
                        is ApiFailure -> failure.message
                        else -> "No se pudo iniciar sesión. Comprueba el servidor y vuelve a intentar."
                    }
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = LoginUiState(busy = false, session = null)
        }
    }
}
