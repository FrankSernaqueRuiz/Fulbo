package pe.fulbo.app.feature.canchas.data

import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pe.fulbo.app.core.firebase.Colecciones
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.feature.canchas.domain.Cancha

/**
 * Repositorio de Canchas conectado a Cloud Firestore.
 * Lee la colección `canchas` y aplica fallback con canchas reales de la ciudad elegida
 * si aún no existen documentos en la base de datos de producción o del emulador.
 */
class FirestoreCanchasRepository(
    private val firestore: FirebaseFirestore,
) : CanchasRepository {

    override fun observarCanchas(ciudad: String): Flow<List<Cancha>> {
        val ciudadBuscada = ciudad.trim()
        return firestore.collection(Colecciones.CANCHAS)
            .snapshots
            .map { snapshot ->
                val canchasDesdeFirestore = snapshot.documents.mapNotNull { doc ->
                    try {
                        val activa = try { doc.get<Boolean>("activa") } catch (_: Exception) { true }
                        if (!activa) return@mapNotNull null

                        val docCiudad = try { doc.get<String>("ciudad") } catch (_: Exception) { "Lima" }
                        if (ciudadBuscada.isNotBlank() && !docCiudad.trim().equals(ciudadBuscada, ignoreCase = true)) {
                            return@mapNotNull null
                        }

                        val id = doc.id
                        val nombre = doc.get<String>("nombre")
                        val direccion = try { doc.get<String>("direccion") } catch (_: Exception) { "" }
                        val distrito = try { doc.get<String>("distrito") } catch (_: Exception) { "" }
                        val superficie = try { doc.get<String>("superficie") } catch (_: Exception) { "Sintético" }
                        val formatos = try { doc.get<List<String>>("formatos") } catch (_: Exception) { listOf("7v7") }
                        val techada = try { doc.get<Boolean>("techada") } catch (_: Exception) { false }
                        val precio = try {
                            doc.get<Double>("precio_hora")
                        } catch (_: Exception) {
                            try { doc.get<Long>("precio_hora").toDouble() } catch (_: Exception) { 100.0 }
                        }
                        val rating = try { doc.get<Double?>("rating") ?: 4.6 } catch (_: Exception) { 4.6 }
                        val numResenas = try { doc.get<Int>("num_resenas") } catch (_: Exception) { 0 }
                        val abreMin = try { doc.get<Int>("abre_min") } catch (_: Exception) { 480 }
                        val cierraMin = try { doc.get<Int>("cierra_min") } catch (_: Exception) { 1380 }
                        val telefono = try { doc.get<String?>("telefono") } catch (_: Exception) { null }

                        val ubicacion = try { doc.get<GeoPoint?>("ubicacion") } catch (_: Exception) { null }
                        val lat = ubicacion?.latitude ?: obtenerCoordenadaBaseCiudad(docCiudad).first
                        val lng = ubicacion?.longitude ?: obtenerCoordenadaBaseCiudad(docCiudad).second

                        Cancha(
                            id = id,
                            nombre = nombre,
                            distrito = distrito,
                            superficie = superficie.replaceFirstChar { it.uppercase() },
                            formato = formatos.firstOrNull() ?: "7v7",
                            techada = techada,
                            rating = rating,
                            precioHora = precio,
                            distanciaMetros = 1000,
                            proximoLibre = "Libre hoy",
                            ultimaHora = false,
                            direccion = direccion,
                            ciudad = docCiudad,
                            latitud = lat,
                            longitud = lng,
                            numResenas = numResenas,
                            abreMin = abreMin,
                            cierraMin = cierraMin,
                            telefono = telefono,
                            activa = true,
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                if (canchasDesdeFirestore.isNotEmpty()) {
                    canchasDesdeFirestore
                } else {
                    obtenerCanchasFallback(ciudadBuscada)
                }
            }
            .catch {
                emit(obtenerCanchasFallback(ciudadBuscada))
            }
    }

    override suspend fun refrescar(lat: Double, lng: Double, texto: String?, ciudad: String): Resultado<Unit> {
        return Resultado.Exito(Unit)
    }

    companion object {
        fun obtenerCoordenadaBaseCiudad(ciudad: String): Pair<Double, Double> {
            return when (ciudad.trim().lowercase()) {
                "piura" -> Pair(-5.1945, -80.6328)
                "trujillo" -> Pair(-8.1118, -79.0287)
                "arequipa" -> Pair(-16.4090, -71.5375)
                "chiclayo" -> Pair(-6.7714, -79.8409)
                "cusco" -> Pair(-13.5319, -71.9675)
                else -> Pair(-12.1211, -77.0297) // Lima (Miraflores)
            }
        }

        fun obtenerCanchasFallback(ciudad: String): List<Cancha> {
            return when (ciudad.trim().lowercase()) {
                "piura" -> listOf(
                    Cancha(
                        id = "piura_1",
                        nombre = "Canchas Sintéticas Golazo Piura",
                        distrito = "Piura",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.8,
                        precioHora = 90.0,
                        distanciaMetros = 950,
                        proximoLibre = "Libre 19:00",
                        ultimaHora = false,
                        direccion = "Av. Andrés Avelino Cáceres 140",
                        ciudad = "Piura",
                        latitud = -5.1850,
                        longitud = -80.6350,
                        abreMin = 480,
                        cierraMin = 1380,
                    ),
                    Cancha(
                        id = "piura_2",
                        nombre = "Complejo Deportivo Piura Fútbol",
                        distrito = "Piura",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = false,
                        rating = 4.7,
                        precioHora = 85.0,
                        distanciaMetros = 1400,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Av. Grau 1250",
                        ciudad = "Piura",
                        latitud = -5.1980,
                        longitud = -80.6280,
                        abreMin = 480,
                        cierraMin = 1440,
                    ),
                    Cancha(
                        id = "piura_3",
                        nombre = "Cancha El Monumental",
                        distrito = "Castilla",
                        superficie = "Grass natural",
                        formato = "11v11",
                        techada = false,
                        rating = 4.5,
                        precioHora = 120.0,
                        distanciaMetros = 2100,
                        proximoLibre = "Libre 18:00",
                        ultimaHora = false,
                        direccion = "Av. Progreso 450",
                        ciudad = "Piura",
                        latitud = -5.1920,
                        longitud = -80.6190,
                        abreMin = 480,
                        cierraMin = 1320,
                    ),
                    Cancha(
                        id = "piura_4",
                        nombre = "Complejo Deportivo San Eduardo",
                        distrito = "Piura",
                        superficie = "Sintético",
                        formato = "5v5",
                        techada = true,
                        rating = 4.6,
                        precioHora = 70.0,
                        distanciaMetros = 1800,
                        proximoLibre = "Última hora 21:00",
                        ultimaHora = true,
                        direccion = "Urb. San Eduardo Mz. B",
                        ciudad = "Piura",
                        latitud = -5.1820,
                        longitud = -80.6400,
                        abreMin = 480,
                        cierraMin = 1440,
                    ),
                    Cancha(
                        id = "piura_5",
                        nombre = "Canchas Sintéticas Santa Ana",
                        distrito = "Piura",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = false,
                        rating = 4.4,
                        precioHora = 80.0,
                        distanciaMetros = 2600,
                        proximoLibre = "Libre 21:00",
                        ultimaHora = false,
                        direccion = "Av. Vice 320",
                        ciudad = "Piura",
                        latitud = -5.1810,
                        longitud = -80.6420,
                        abreMin = 480,
                        cierraMin = 1380,
                    ),
                )
                "trujillo" -> listOf(
                    Cancha(
                        id = "truj_1",
                        nombre = "Complejo El Golf Trujillo",
                        distrito = "Víctor Larco",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.8,
                        precioHora = 100.0,
                        distanciaMetros = 1200,
                        proximoLibre = "Libre 19:00",
                        ultimaHora = false,
                        direccion = "Av. El Golf 520",
                        ciudad = "Trujillo",
                        latitud = -8.1320,
                        longitud = -79.0340,
                    ),
                    Cancha(
                        id = "truj_2",
                        nombre = "Canchas Fútbol 7 California",
                        distrito = "California",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = false,
                        rating = 4.6,
                        precioHora = 85.0,
                        distanciaMetros = 1600,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Calle Los Laureles 140",
                        ciudad = "Trujillo",
                        latitud = -8.1240,
                        longitud = -79.0410,
                    ),
                    Cancha(
                        id = "truj_3",
                        nombre = "Complejo Deportivo Mansiche",
                        distrito = "Trujillo Centro",
                        superficie = "Sintético",
                        formato = "5v5",
                        techada = false,
                        rating = 4.5,
                        precioHora = 75.0,
                        distanciaMetros = 2100,
                        proximoLibre = "Libre 21:00",
                        ultimaHora = false,
                        direccion = "Av. Mansiche 800",
                        ciudad = "Trujillo",
                        latitud = -8.1050,
                        longitud = -79.0310,
                    ),
                )
                "arequipa" -> listOf(
                    Cancha(
                        id = "aqp_1",
                        nombre = "Canchas Cayma Fútbol Club",
                        distrito = "Cayma",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.8,
                        precioHora = 95.0,
                        distanciaMetros = 1100,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Av. Ejército 890",
                        ciudad = "Arequipa",
                        latitud = -16.3860,
                        longitud = -71.5440,
                    ),
                    Cancha(
                        id = "aqp_2",
                        nombre = "Grass Sintético Yanahuara",
                        distrito = "Yanahuara",
                        superficie = "Grass natural",
                        formato = "5v5",
                        techada = false,
                        rating = 4.5,
                        precioHora = 85.0,
                        distanciaMetros = 1900,
                        proximoLibre = "Libre 18:00",
                        ultimaHora = false,
                        direccion = "Calle Jerusalén 320",
                        ciudad = "Arequipa",
                        latitud = -16.3910,
                        longitud = -71.5390,
                    ),
                    Cancha(
                        id = "aqp_3",
                        nombre = "Complejo Cerro Colorado",
                        distrito = "Cerro Colorado",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = false,
                        rating = 4.4,
                        precioHora = 80.0,
                        distanciaMetros = 2800,
                        proximoLibre = "Libre 19:00",
                        ultimaHora = false,
                        direccion = "Av. Aviación 1100",
                        ciudad = "Arequipa",
                        latitud = -16.3720,
                        longitud = -71.5620,
                    ),
                )
                "chiclayo" -> listOf(
                    Cancha(
                        id = "cix_1",
                        nombre = "Fútbol Total Santa Victoria",
                        distrito = "Santa Victoria",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.7,
                        precioHora = 85.0,
                        distanciaMetros = 1200,
                        proximoLibre = "Libre 19:00",
                        ultimaHora = false,
                        direccion = "Av. Santa Victoria 410",
                        ciudad = "Chiclayo",
                        latitud = -6.7750,
                        longitud = -79.8450,
                    ),
                    Cancha(
                        id = "cix_2",
                        nombre = "Complejo Deportivo El Trébol",
                        distrito = "Chiclayo",
                        superficie = "Sintético",
                        formato = "5v5",
                        techada = false,
                        rating = 4.5,
                        precioHora = 70.0,
                        distanciaMetros = 1800,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Av. Salaverry 850",
                        ciudad = "Chiclayo",
                        latitud = -6.7820,
                        longitud = -79.8510,
                    ),
                )
                "cusco" -> listOf(
                    Cancha(
                        id = "cus_1",
                        nombre = "Canchas Sintéticas Huancaro",
                        distrito = "Santiago",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.8,
                        precioHora = 90.0,
                        distanciaMetros = 1300,
                        proximoLibre = "Libre 19:00",
                        ultimaHora = false,
                        direccion = "Av. Huancaro 500",
                        ciudad = "Cusco",
                        latitud = -13.5350,
                        longitud = -71.9750,
                    ),
                    Cancha(
                        id = "cus_2",
                        nombre = "Complejo Deportivo Wanchaq",
                        distrito = "Wanchaq",
                        superficie = "Sintético",
                        formato = "5v5",
                        techada = false,
                        rating = 4.6,
                        precioHora = 80.0,
                        distanciaMetros = 1900,
                        proximoLibre = "Libre 21:00",
                        ultimaHora = false,
                        direccion = "Av. de la Cultura 1200",
                        ciudad = "Cusco",
                        latitud = -13.5220,
                        longitud = -71.9540,
                    ),
                )
                else -> listOf(
                    Cancha(
                        id = "lima_1",
                        nombre = "Complejo Deportivo El Golazo",
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
                        id = "lima_2",
                        nombre = "Canchas Fútbol 7 San Isidro",
                        distrito = "San Isidro",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.7,
                        precioHora = 140.0,
                        distanciaMetros = 1600,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Av. Rivera Navarrete 780",
                        ciudad = "Lima",
                        latitud = -12.0980,
                        longitud = -77.0350,
                    ),
                    Cancha(
                        id = "lima_3",
                        nombre = "Complejo Surquillo Pro",
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
                        id = "lima_4",
                        nombre = "DeporCentro Casuarinas",
                        distrito = "Santiago de Surco",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = true,
                        rating = 4.9,
                        precioHora = 135.0,
                        distanciaMetros = 2800,
                        proximoLibre = "Libre 20:00",
                        ultimaHora = false,
                        direccion = "Av. Primavera 1200",
                        ciudad = "Lima",
                        latitud = -12.1120,
                        longitud = -76.9850,
                    ),
                    Cancha(
                        id = "lima_5",
                        nombre = "Cancha El Potrero",
                        distrito = "Barranco",
                        superficie = "Grass natural",
                        formato = "11v11",
                        techada = false,
                        rating = 4.3,
                        precioHora = 110.0,
                        distanciaMetros = 3100,
                        proximoLibre = "Libre 18:00",
                        ultimaHora = false,
                        direccion = "Av. Pedro de Osma 310",
                        ciudad = "Lima",
                        latitud = -12.1480,
                        longitud = -77.0200,
                    ),
                    Cancha(
                        id = "lima_6",
                        nombre = "Club Deportivo San Miguel",
                        distrito = "San Miguel",
                        superficie = "Sintético",
                        formato = "7v7",
                        techada = false,
                        rating = 4.5,
                        precioHora = 100.0,
                        distanciaMetros = 4200,
                        proximoLibre = "Libre 19:30",
                        ultimaHora = false,
                        direccion = "Av. La Marina 1800",
                        ciudad = "Lima",
                        latitud = -12.0770,
                        longitud = -77.0850,
                    ),
                )
            }
        }
    }
}
