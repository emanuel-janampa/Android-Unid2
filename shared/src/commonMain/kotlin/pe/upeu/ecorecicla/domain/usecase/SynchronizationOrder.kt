package pe.upeu.ecorecicla.domain.usecase

/** Enforces offline-first ordering: upload pending local entries before downloading current server state. */
class SynchronizationOrder {
    suspend fun execute(uploadPending: suspend () -> Int, downloadLatest: suspend () -> Unit): Int {
        val uploaded = uploadPending()
        downloadLatest()
        return uploaded
    }
}
