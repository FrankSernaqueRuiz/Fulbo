package pe.fulbo.app.feature.canchas.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.feature.canchas.domain.Cancha

/**
 * Contrato que usa el ViewModel. La UI y el ViewModel nunca hablan con Firebase directamente.
 */
interface CanchasRepository {
    fun observarCanchas(ciudad: String = "Lima"): Flow<List<Cancha>>
    suspend fun refrescar(lat: Double, lng: Double, texto: String? = null, ciudad: String = "Lima"): Resultado<Unit>
}

/**
 * Implementación de prueba para tests o previsualizaciones.
 */
class FakeCanchasRepository : CanchasRepository {

    private val canchas = MutableStateFlow<List<Cancha>>(emptyList())

    override fun observarCanchas(ciudad: String): Flow<List<Cancha>> = canchas.asStateFlow()

    override suspend fun refrescar(lat: Double, lng: Double, texto: String?, ciudad: String): Resultado<Unit> {
        delay(600) // simula red
        canchas.value = DATOS.filter {
            (texto.isNullOrBlank() || it.nombre.contains(texto, true) || it.distrito.contains(texto, true)) &&
            (ciudad.isBlank() || it.ciudad.equals(ciudad, ignoreCase = true))
        }
        return Resultado.Exito(Unit)
    }

    private companion object {
        val DATOS = listOf(
            Cancha(
                id = "1",
                nombre = "La Bombonerita",
                distrito = "Miraflores",
                superficie = "Sintético",
                formato = "7v7",
                techada = true,
                rating = 4.8,
                precioHora = 120.0,
                distanciaMetros = 650,
                proximoLibre = "Libre 19:00",
                ultimaHora = false,
                direccion = "Av. José Pardo 450",
                ciudad = "Lima",
                latitud = -12.1211,
                longitud = -77.0297,
            ),
            Cancha(
                id = "2",
                nombre = "Surquillo Pro",
                distrito = "Surquillo",
                superficie = "Sintético",
                formato = "5v5",
                techada = false,
                rating = 4.5,
                precioHora = 95.0,
                distanciaMetros = 1200,
                proximoLibre = "Última hora 21:00",
                ultimaHora = true,
                direccion = "Calle Lizardo Montero 230",
                ciudad = "Lima",
                latitud = -12.1150,
                longitud = -77.0220,
            ),
            Cancha(
                id = "3",
                nombre = "Estadio Chico 7",
                distrito = "San Isidro",
                superficie = "Sintético",
                formato = "7v7",
                techada = true,
                rating = 4.6,
                precioHora = 140.0,
                distanciaMetros = 2400,
                proximoLibre = "Libre 20:00",
                ultimaHora = false,
                direccion = "Av. Rivera Navarrete 780",
                ciudad = "Lima",
                latitud = -12.0980,
                longitud = -77.0350,
            ),
            Cancha(
                id = "4",
                nombre = "Cancha El Potrero",
                distrito = "Barranco",
                superficie = "Grass natural",
                formato = "11v11",
                techada = false,
                rating = 4.2,
                precioHora = 110.0,
                distanciaMetros = 3100,
                proximoLibre = "Libre 18:00",
                ultimaHora = false,
                direccion = "Av. Pedro de Osma 310",
                ciudad = "Lima",
                latitud = -12.1480,
                longitud = -77.0200,
            ),
        )
    }
}
