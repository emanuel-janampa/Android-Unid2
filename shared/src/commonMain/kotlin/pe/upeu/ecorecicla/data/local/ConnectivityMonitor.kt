package pe.upeu.ecorecicla.data.local

import kotlinx.coroutines.flow.Flow

/** Platform connectivity signal used to retry offline-first synchronization on reconnection. */
interface ConnectivityMonitor {
    val online: Flow<Boolean>
}
