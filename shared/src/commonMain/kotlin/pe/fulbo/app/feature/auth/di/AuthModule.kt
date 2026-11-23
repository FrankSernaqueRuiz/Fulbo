package pe.fulbo.app.feature.auth.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pe.fulbo.app.feature.auth.data.FirebaseAuthRepository
import pe.fulbo.app.feature.auth.domain.AuthRepository

val authModule = module {
    singleOf(::FirebaseAuthRepository) { bind<AuthRepository>() }
}
