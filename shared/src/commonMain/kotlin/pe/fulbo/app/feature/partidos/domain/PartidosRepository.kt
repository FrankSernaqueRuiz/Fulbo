package pe.fulbo.app.feature.partidos.domain

import kotlinx.coroutines.flow.Flow
import pe.fulbo.app.core.model.DeporteCasual
import pe.fulbo.app.core.model.PartidoCasualItem
import pe.fulbo.app.core.util.Resultado

interface PartidosRepository {
    fun observarPartidos(ciudad: String): Flow<List<PartidoCasualItem>>
    suspend fun publicarPartido(
        canchaNombre: String,
        deporte: DeporteCasual,
        formato: String,
        cuposFaltantes: Int,
        nivelBuscado: String,
        costoPersona: Double,
        ciudad: String = "Lima",
    ): Resultado<String>
    suspend fun solicitarInscripcion(partidoId: String, listaEspera: Boolean = false): Resultado<String>
}
