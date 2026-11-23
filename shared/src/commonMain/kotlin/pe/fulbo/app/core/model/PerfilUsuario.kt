package pe.fulbo.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PerfilUsuario(
    val uid: String,
    val nombre: String,
    val apellido: String,
    val alias: String,
    val ciudad: String = "Lima",
    val posicion: String = "medio",
    val nivel: String = "intermedio",
    val repGlobal: Double? = null,
    val repPuntualidad: Float? = null,
    val repAsistencia: Float? = null,
    val repPago: Float? = null,
    val repTrato: Float? = null,
    val numCalificaciones: Int = 0,
    val partidosJugados: Int = 0,
    val faltas: Int = 0,
    val plan: String = "gratuito",
    val fotoUrl: String? = null,
) {
    val nombreCompleto: String
        get() = "$nombre $apellido".trim()

    val iniciales: String
        get() = listOf(nombre.take(1), apellido.take(1))
            .filter { it.isNotBlank() }
            .joinToString("")
            .uppercase()
            .ifBlank { "FB" }
}
