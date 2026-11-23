package pe.fulbo.app.feature.partidos.data

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import pe.fulbo.app.core.firebase.Colecciones
import pe.fulbo.app.core.firebase.Funciones
import pe.fulbo.app.core.model.DeporteCasual
import pe.fulbo.app.core.model.PartidoCasualItem
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.core.util.llamadaSegura
import pe.fulbo.app.feature.partidos.domain.PartidosRepository

class FirestorePartidosRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : PartidosRepository {

    override fun observarPartidos(ciudad: String): Flow<List<PartidoCasualItem>> {
        return firestore.collection(Colecciones.PARTIDOS)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val canchaNombre = doc.get<String>("cancha_nombre")
                        val formato = doc.get<String>("formato")
                        val nivel = doc.get<String>("nivel_buscado")
                        val costo = doc.get<Double>("costo_persona")
                        val cuposLibres = doc.get<Int>("cupos_libres")
                        val estado = doc.get<String>("estado")

                        if (estado != "publicado" && estado != "completo") return@mapNotNull null

                        val formatoEtiqueta = when (formato) {
                            "4v4" -> "4 vs 4"
                            "5v5" -> "5 vs 5"
                            "11v11" -> "11 vs 11"
                            else -> "7 vs 7"
                        }
                        val nivelEtiqueta = when (nivel) {
                            "recien" -> "Recién empiezo"
                            "casual" -> "Casual"
                            "competitivo" -> "Competitivo"
                            else -> "Intermedio"
                        }

                        PartidoCasualItem(
                            id = id,
                            deporte = DeporteCasual.FUTBOL,
                            cancha = canchaNombre,
                            fechaHora = "Hoy 8:00 pm",
                            formato = formatoEtiqueta,
                            nivel = nivelEtiqueta,
                            costoPersona = costo.toInt(),
                            cuposFaltantes = cuposLibres,
                            organizadorNombre = "Organizador",
                            organizadorRating = 4.8,
                            organizadorIniciales = "OR",
                            estadoTexto = if (cuposLibres == 1) "Falta 1 jugador" else "Faltan $cuposLibres jugadores",
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .catch {
                // Fallback con datos por defecto si aún no hay colección en el emulador/base
                emit(emptyList())
            }
    }

    override suspend fun publicarPartido(
        canchaNombre: String,
        deporte: DeporteCasual,
        formato: String,
        cuposFaltantes: Int,
        nivelBuscado: String,
        costoPersona: Double,
        ciudad: String,
    ): Resultado<String> = llamadaSegura {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Debes iniciar sesión para publicar un partido")
        val cuposTotales = cuposFaltantes + 1
        val formatoCanonico = when {
            formato.contains("4") -> "4v4"
            formato.contains("5") -> "5v5"
            formato.contains("11") -> "11v11"
            else -> "7v7"
        }
        val nivelCanonico = when {
            nivelBuscado.lowercase().contains("recien") -> "recien"
            nivelBuscado.lowercase().contains("casual") -> "casual"
            nivelBuscado.lowercase().contains("competitivo") -> "competitivo"
            else -> "intermedio"
        }

        val segundosFuturos = (kotlin.time.Clock.System.now().toEpochMilliseconds() + 86400000) / 1000
        val fechaHoraTimestamp = Timestamp(segundosFuturos, 0)

        val docRef = firestore.collection(Colecciones.PARTIDOS).document
        docRef.set(
            mapOf(
                "organizador_uid" to uid,
                "cancha_id" to "cancha_${docRef.id.take(8)}",
                "cancha_nombre" to canchaNombre,
                "equipo_id" to null,
                "fecha_hora" to fechaHoraTimestamp,
                "formato" to formatoCanonico,
                "cupos_totales" to cuposTotales,
                "cupos_libres" to cuposFaltantes,
                "nivel_buscado" to nivelCanonico,
                "rep_minima" to null,
                "costo_persona" to costoPersona,
                "cancha_confirmada" to true,
                "ubicacion" to GeoPoint(-12.046374, -77.042793),
                "ciudad" to ciudad,
                "descripcion" to "Partido publicado en $canchaNombre",
                "estado" to "publicado",
                "creado_en" to FieldValue.serverTimestamp,
            )
        )
        docRef.id
    }

    override suspend fun solicitarInscripcion(
        partidoId: String,
        listaEspera: Boolean,
    ): Resultado<String> = llamadaSegura {
        val callable = functions.httpsCallable(Funciones.SOLICITAR_INSCRIPCION)
        callable.invoke(
            mapOf(
                "partidoId" to partidoId,
                "listaEspera" to listaEspera,
            )
        )
        "Solicitud procesada con éxito"
    }
}
