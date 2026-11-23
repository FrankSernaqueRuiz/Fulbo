package pe.fulbo.app.feature.auth.domain

import kotlinx.serialization.Serializable

@Serializable
data class DatosCompletarPerfil(
    val nombre: String,
    val apellido: String,
    val alias: String,
    val fechaNacimiento: String, // "YYYY-MM-DD"
    val ciudad: String = "Lima",
    val posicion: String = "medio",
    val nivel: String = "intermedio",
    val aceptaTerminos: Boolean = true,
)
