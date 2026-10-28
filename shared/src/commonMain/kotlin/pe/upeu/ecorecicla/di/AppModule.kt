package pe.upeu.ecorecicla.di

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.ecorecicla.AppConfig
import pe.upeu.ecorecicla.data.local.ConnectivityMonitor
import pe.upeu.ecorecicla.data.local.EcoReciclaDatabase
import pe.upeu.ecorecicla.data.local.LocalEcoStore
import pe.upeu.ecorecicla.data.local.SecureSessionStore
import pe.upeu.ecorecicla.data.local.SessionToken
import pe.upeu.ecorecicla.data.local.SqlDelightLocalEcoStore
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.EcoRemoteDataSource
import pe.upeu.ecorecicla.data.repository.AuthRepositoryImpl
import pe.upeu.ecorecicla.data.repository.OfflineFirstEcoRepository
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.EcoRepository
import pe.upeu.ecorecicla.domain.usecase.DashboardUseCases
import pe.upeu.ecorecicla.presentation.DashboardViewModel
import pe.upeu.ecorecicla.presentation.LoginViewModel

fun appModule(
    httpClient: HttpClient,
    database: EcoReciclaDatabase,
    sessionStore: SecureSessionStore,
    sessionToken: SessionToken,
    connectivityMonitor: ConnectivityMonitor
): Module = module {
    single { httpClient }
    single { database }
    single<SecureSessionStore> { sessionStore }
    single { sessionToken }
    single<ConnectivityMonitor> { connectivityMonitor }
    // Register the concrete client and expose it through the interface consumed by repositories.
    // Without this binding, Koin fails while constructing AuthRepository and LoginViewModel.
    single { EcoApi(get(), AppConfig.API_BASE_URL, get(), get()) }
    single<EcoRemoteDataSource> { get<EcoApi>() }
    single<LocalEcoStore> { SqlDelightLocalEcoStore(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<EcoRepository> {
        OfflineFirstEcoRepository(get(), get(), { get<SessionToken>().username ?: "operario1" })
    }
    single { DashboardUseCases(get()) }
    factory { LoginViewModel(get(), get()) }
    factory { DashboardViewModel(get(), get()) }
}
