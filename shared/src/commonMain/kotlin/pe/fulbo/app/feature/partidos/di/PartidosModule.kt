package pe.fulbo.app.feature.partidos.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pe.fulbo.app.feature.partidos.data.FirestorePartidosRepository
import pe.fulbo.app.feature.partidos.domain.PartidosRepository

val partidosModule = module {
    singleOf(::FirestorePartidosRepository) { bind<PartidosRepository>() }
}
