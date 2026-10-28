package pe.upeu.ecorecicla.domain.model

sealed class ApiFailure(message: String) : Exception(message) {
    class Validation(message: String, val fields: Map<String, String> = emptyMap()) : ApiFailure(message)
    class Unauthorized(message: String = "La sesión venció. Inicia sesión nuevamente.") : ApiFailure(message)
    class Forbidden(message: String = "No tienes privilegios para realizar esta operación.") : ApiFailure(message)
    class NotFound(message: String = "No se encontró el recurso solicitado.") : ApiFailure(message)
    class Conflict(message: String) : ApiFailure(message)
    class Server(message: String = "El servidor tuvo un problema. Inténtalo de nuevo.") : ApiFailure(message)
    class Offline(message: String = "No hay conexión. Los cambios compatibles quedarán pendientes.") : ApiFailure(message)
    class Timeout(message: String = "La solicitud tardó demasiado. Inténtalo de nuevo.") : ApiFailure(message)
    class Unexpected(message: String) : ApiFailure(message)
}

fun failureForStatus(status: Int, message: String = "Error HTTP $status", fields: Map<String, String> = emptyMap()): ApiFailure =
    when (status) {
        400 -> ApiFailure.Validation(message, fields)
        401 -> ApiFailure.Unauthorized(message)
        403 -> ApiFailure.Forbidden(message)
        404 -> ApiFailure.NotFound(message)
        409 -> ApiFailure.Conflict(message)
        in 500..599 -> ApiFailure.Server(message)
        else -> ApiFailure.Unexpected(message)
    }
