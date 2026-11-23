package pe.fulbo.app.feature.canchas.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Modelo de dominio para las canchas deportivas.
 * Totalmente compatible con la UI de listado y de mapa interactivo.
 */
data class Cancha(
    val id: String,
    val nombre: String,
    val distrito: String,
    val superficie: String,
    val formato: String,
    val techada: Boolean,
    val rating: Double,
    val precioHora: Double,
    val distanciaMetros: Int = 1000,
    val proximoLibre: String? = null,
    val ultimaHora: Boolean = false,
    val direccion: String = "",
    val ciudad: String = "Lima",
    val latitud: Double = -12.1211,
    val longitud: Double = -77.0297,
    val numResenas: Int = 0,
    val abreMin: Int = 480,    // 08:00 am
    val cierraMin: Int = 1380,  // 11:00 pm
    val telefono: String? = null,
    val activa: Boolean = true,
) {
    /** "650 m" o "2.4 km" (sin String.format: compatible con Kotlin Multiplatform). */
    val distanciaTexto: String
        get() = if (distanciaMetros < 1000) "$distanciaMetros m"
        else {
            val decimas = (distanciaMetros + 50) / 100   // redondea a 0.1 km
            "${decimas / 10}.${decimas % 10} km"
        }

    /** Formato amigable de horario: "Hasta las 11:00 pm". */
    val horarioTexto: String
        get() {
            val horaCierre = cierraMin / 60
            val minCierre = cierraMin % 60
            val periodo = if (horaCierre >= 12 && horaCierre < 24) "pm" else "am"
            val hora12 = if (horaCierre > 12) horaCierre - 12 else if (horaCierre == 0 || horaCierre == 24) 12 else horaCierre
            val minStr = if (minCierre < 10) "0$minCierre" else "$minCierre"
            return "Hasta las $hora12:$minStr $periodo"
        }

    /** Devuelve una copia de la cancha con la distancia recalculada a partir de coordenadas GPS. */
    fun conUbicacionUsuario(userLat: Double, userLng: Double): Cancha {
        val dist = calcularDistanciaMetros(userLat, userLng, latitud, longitud)
        return copy(distanciaMetros = dist)
    }
}

/**
 * Fórmula de Haversine para calcular distancia esférica en metros entre dos coordenadas.
 */
fun calcularDistanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371000.0 // Radio de la Tierra en metros
    val dLat = (lat2 - lat1) * (PI / 180.0)
    val dLon = (lon2 - lon1) * (PI / 180.0)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).toInt()
}
