package pe.fulbo.app.feature.auth.domain

import kotlinx.coroutines.flow.Flow
import pe.fulbo.app.core.model.PerfilUsuario
import pe.fulbo.app.core.model.ResenaJugador
import pe.fulbo.app.core.util.Resultado

interface AuthRepository {
    val usuarioActualUid: String?
    val estaAutenticado: Boolean
    val authStateChanged: Flow<String?>
    fun observarMiPerfil(): Flow<PerfilUsuario?>
    fun observarMisResenas(): Flow<List<ResenaJugador>>
    suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Unit>
    suspend fun crearCuenta(correo: String, contrasena: String): Resultado<String>
    suspend fun completarPerfil(datos: DatosCompletarPerfil): Resultado<Unit>
    suspend fun cerrarSesion(): Resultado<Unit>
    suspend fun eliminarCuenta(): Resultado<Unit>
}
