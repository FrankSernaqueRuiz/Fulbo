package pe.fulbo.app.feature.canchas.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pe.fulbo.app.feature.canchas.CanchasViewModel
import pe.fulbo.app.feature.canchas.data.CanchasRepository
import pe.fulbo.app.feature.canchas.data.FirestoreCanchasRepository

val canchasModule = module {
    single { FirestoreCanchasRepository(get()) } bind CanchasRepository::class
    // authRepo se inyecta automáticamente desde authModule (mismo tipo AuthRepository)
    viewModelOf(::CanchasViewModel)
}
