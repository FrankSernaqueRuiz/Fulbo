package pe.fulbo.app.feature.canchas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.fulbo.app.core.util.AppError
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.feature.auth.domain.AuthRepository
import pe.fulbo.app.feature.canchas.data.CanchasRepository
import pe.fulbo.app.feature.canchas.data.FirestoreCanchasRepository
import pe.fulbo.app.feature.canchas.domain.Cancha

enum class FiltroCancha(val etiqueta: String) {
    ABIERTO_AHORA("Abierto ahora"),
    CERCA_DE_MI("Cerca de mí"),
    FUTBOL_7("Fútbol 7"),
    SINTETICO("Sintético"),
    TECHADA("Techada"),
}

/** Todo lo que la pantalla necesita para dibujarse, en un solo objeto inmutable. */
data class CanchasUiState(
    val cargando: Boolean = true,
    val ciudad: String = "Lima",
    val canchas: List<Cancha> = emptyList(),
    val canchaSeleccionada: Cancha? = null,
    val busqueda: String = "",
    val filtros: Set<FiltroCancha> = setOf(FiltroCancha.ABIERTO_AHORA),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val error: AppError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CanchasViewModel(
    private val repo: CanchasRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private var ciudadSeleccionadaManualmente = false
    private val local = MutableStateFlow(CanchasUiState())

    init {
        viewModelScope.launch {
            authRepo.observarMiPerfil().collect { perfil ->
                if (perfil != null && perfil.ciudad.isNotBlank() && !ciudadSeleccionadaManualmente) {
                    onCiudadChange(perfil.ciudad)
                }
            }
        }
    }

    val uiState: StateFlow<CanchasUiState> = local.flatMapLatest { state ->
        repo.observarCanchas(state.ciudad).map { canchasBrutas ->
            val baseCoords = FirestoreCanchasRepository.obtenerCoordenadaBaseCiudad(state.ciudad)
            val uLat = state.userLat ?: baseCoords.first
            val uLng = state.userLng ?: baseCoords.second

            val conDistancia = canchasBrutas.map { it.conUbicacionUsuario(uLat, uLng) }
            val filtradas = conDistancia.filter {
                cumpleFiltros(it, state.filtros, state.busqueda)
            }.sortedWith(
                if (FiltroCancha.CERCA_DE_MI in state.filtros) compareBy { it.distanciaMetros }
                else compareByDescending { it.rating }
            )

            state.copy(
                cargando = false,
                canchas = filtradas,
                canchaSeleccionada = state.canchaSeleccionada?.let { sel ->
                    filtradas.find { it.id == sel.id }
                } ?: filtradas.firstOrNull(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CanchasUiState())

    fun onCiudadChange(ciudad: String) {
        ciudadSeleccionadaManualmente = true
        local.update {
            val baseCoords = FirestoreCanchasRepository.obtenerCoordenadaBaseCiudad(ciudad)
            it.copy(
                ciudad = ciudad,
                userLat = baseCoords.first,
                userLng = baseCoords.second,
                canchaSeleccionada = null,
                cargando = true,
            )
        }
    }

    fun onUbicacionChange(lat: Double, lng: Double) {
        local.update { it.copy(userLat = lat, userLng = lng) }
    }

    fun onBusquedaChange(texto: String) {
        local.update { it.copy(busqueda = texto) }
    }

    fun onCanchaSeleccionada(cancha: Cancha) {
        local.update { it.copy(canchaSeleccionada = cancha) }
    }

    fun onFiltroClick(filtro: FiltroCancha) = local.update {
        it.copy(filtros = if (filtro in it.filtros) it.filtros - filtro else it.filtros + filtro)
    }

    fun refrescar() = viewModelScope.launch {
        local.update { it.copy(cargando = true, error = null) }
        val s = local.value
        val coords = FirestoreCanchasRepository.obtenerCoordenadaBaseCiudad(s.ciudad)
        val result = repo.refrescar(
            lat = s.userLat ?: coords.first,
            lng = s.userLng ?: coords.second,
            texto = s.busqueda,
            ciudad = s.ciudad,
        )
        local.update {
            it.copy(cargando = false, error = (result as? Resultado.Fallo)?.error)
        }
    }

    private fun cumpleFiltros(c: Cancha, filtros: Set<FiltroCancha>, busqueda: String): Boolean {
        if (busqueda.isNotBlank()) {
            val coincide = c.nombre.contains(busqueda, ignoreCase = true) ||
                    c.distrito.contains(busqueda, ignoreCase = true) ||
                    c.direccion.contains(busqueda, ignoreCase = true)
            if (!coincide) return false
        }

        return filtros.all { f ->
            when (f) {
                FiltroCancha.ABIERTO_AHORA -> c.activa
                FiltroCancha.CERCA_DE_MI -> c.distanciaMetros <= 5000 // Menos de 5 km
                FiltroCancha.FUTBOL_7 -> c.formato == "7v7"
                FiltroCancha.SINTETICO -> c.superficie.equals("Sintético", ignoreCase = true)
                FiltroCancha.TECHADA -> c.techada
            }
        }
    }
}
