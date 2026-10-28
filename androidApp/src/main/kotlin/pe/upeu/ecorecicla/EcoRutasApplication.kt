package pe.upeu.ecorecicla

import android.app.Application

/** Android entry point. Feature code and dependency setup live in :shared. */
class EcoRutasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EcoRutasInitializer.initialize(this)
    }
}
