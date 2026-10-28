package pe.upeu.ecorecicla.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.data.local.ConnectivityMonitor
import pe.upeu.ecorecicla.domain.model.ApiFailure
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.ResultadoSincronizacion
import pe.upeu.ecorecicla.domain.model.RutaDelDia
import pe.upeu.ecorecicla.domain.usecase.DashboardUseCases

 data class DashboardUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val route: RutaDelDia? = null,
    val points: List<pe.upeu.ecorecicla.domain.model.PuntoAcopio> = emptyList(),
    val weighings: List<Pesaje> = emptyList(),
    val activePointId: Long? = null,
    val pendingCount: Long = 0,
    val zoneFilter: String = "",
    val message: String? = null,
    val error: String? = null,
    val syncResult: ResultadoSincronizacion? = null
)

class DashboardViewModel(
    private val useCases: DashboardUseCases,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var weightsJob: Job? = null

    init {
        viewModelScope.launch {
            useCases.observeRoute().collect { route -> _uiState.update { it.copy(route = route, loading = false) } }
        }
        viewModelScope.launch {
            useCases.observePoints().collect { points -> _uiState.update { it.copy(points = points, loading = false) } }
        }
        viewModelScope.launch {
            useCases.observePendingCount().collect { count -> _uiState.update { it.copy(pendingCount = count) } }
        }
        viewModelScope.launch {
            var previousOnline: Boolean? = null
            connectivityMonitor.online.distinctUntilChanged().collect { online ->
                if (online && previousOnline == false) {
                    // The user has regained signal: upload pending rows before refreshing server data.
                    synchronize()
                }
                previousOnline = online
            }
        }
        refreshAll(showErrors = false)
    }

    fun dismissMessages() = _uiState.update { it.copy(message = null, error = null, syncResult = null) }

    fun setZoneFilter(value: String) = _uiState.update { it.copy(zoneFilter = value) }

    fun refreshAll(showErrors: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            var routeFailure: Throwable? = null
            var pointsFailure: Throwable? = null
            // Load the points even when an administrator has no route assigned for today.
            try { useCases.refreshRoute() } catch (failure: Exception) { routeFailure = failure }
            try { useCases.refreshPoints() } catch (failure: Exception) { pointsFailure = failure }
            val noRouteAssigned = routeFailure is ApiFailure.NotFound && pointsFailure == null && _uiState.value.route == null
            val failure = routeFailure ?: pointsFailure
            _uiState.update { current ->
                when {
                    routeFailure == null && pointsFailure == null -> current.copy(
                        busy = false, error = null, message = "Datos actualizados desde EcoAPI."
                    )
                    noRouteAssigned -> current.copy(
                        busy = false, error = null, message = "Padrón actualizado. Este usuario no tiene una ruta asignada para hoy."
                    )
                    !showErrors -> current.copy(
                        busy = false, error = null,
                        message = if (current.route != null || current.points.isNotEmpty())
                            "Mostrando los datos locales disponibles; sincroniza al recuperar la conexión."
                        else "No se pudieron descargar datos. Comprueba la conexión con EcoAPI."
                    )
                    else -> current.copy(
                        busy = false,
                        error = friendlyMessage(requireNotNull(failure)),
                        message = if (current.route != null || current.points.isNotEmpty()) "Se conservan los datos locales disponibles." else null
                    )
                }
            }
        }
    }

    fun refreshZone() {
        val zone = _uiState.value.zoneFilter.trim().ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                useCases.refreshPoints(zone)
                _uiState.update { it.copy(busy = false, message = if (zone == null) "Listado actualizado." else "Filtro consultado: $zone") }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun openPoint(pointId: Long) {
        weightsJob?.cancel()
        _uiState.update { it.copy(activePointId = pointId, weighings = emptyList(), error = null) }
        weightsJob = viewModelScope.launch {
            useCases.observeWeighings(pointId).collect { list -> _uiState.update { it.copy(weighings = list) } }
        }
        viewModelScope.launch {
            try { useCases.refreshWeighings(pointId) }
            catch (failure: Exception) { _uiState.update { it.copy(error = friendlyMessage(failure)) } }
        }
    }

    fun closePoint() {
        weightsJob?.cancel()
        _uiState.update { it.copy(activePointId = null, weighings = emptyList()) }
    }

    fun registerWeighing(pointId: Long, input: RegistrarPesaje) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val result = useCases.registerWeighing(pointId, input)
                _uiState.update { it.copy(busy = false, message = if (result.pendiente) "Pesaje guardado localmente; se sincronizará cuando haya conexión." else "Pesaje registrado.") }
                openPoint(pointId)
                refreshAll(showErrors = false)
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun correctWeighing(weighing: Pesaje, input: RegistrarPesaje) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                val updated = useCases.correctWeighing(weighing.id, input)
                _uiState.update { it.copy(busy = false, message = if (updated.pendiente) "Corrección guardada como pendiente." else "Pesaje corregido.") }
                openPoint(weighing.puntoId)
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun voidWeighing(weighing: Pesaje, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                useCases.voidWeighing(weighing.id, reason)
                _uiState.update { it.copy(busy = false, message = "Pesaje anulado; el historial se conserva.") }
                openPoint(weighing.puntoId)
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun createPoint(input: CrearPunto) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                useCases.createPoint(input)
                useCases.refreshPoints()
                _uiState.update { it.copy(busy = false, message = "Punto de acopio creado.") }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun closeRoute(routeId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                useCases.closeRoute(routeId)
                _uiState.update { it.copy(busy = false, message = "Ruta cerrada correctamente.") }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun omitPoint(routeId: Long, pointId: Long, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                useCases.omitPoint(routeId, pointId, reason)
                _uiState.update { it.copy(busy = false, message = "Motivo de omisión guardado.") }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    fun synchronize() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val result = useCases.synchronize()
                _uiState.update { it.copy(busy = false, syncResult = result, message = result.mensaje) }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = friendlyMessage(failure)) }
            }
        }
    }

    private fun friendlyMessage(failure: Throwable): String = when (failure) {
        is ApiFailure -> failure.message ?: "Ocurrió un error inesperado."
        else -> failure.message ?: "Ocurrió un error inesperado."
    }
}
