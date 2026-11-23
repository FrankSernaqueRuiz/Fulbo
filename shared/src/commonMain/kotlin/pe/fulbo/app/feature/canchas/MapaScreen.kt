package pe.fulbo.app.feature.canchas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.fulbo.app.core.designsystem.component.FulboChip
import pe.fulbo.app.core.designsystem.component.FulboPrimaryButton
import pe.fulbo.app.core.designsystem.component.FulboSecondaryButton
import pe.fulbo.app.core.designsystem.component.StatusPill
import pe.fulbo.app.core.designsystem.theme.FulboColors
import pe.fulbo.app.core.designsystem.theme.spacing
import pe.fulbo.app.feature.canchas.data.FirestoreCanchasRepository
import pe.fulbo.app.feature.canchas.domain.Cancha

private val COLOR_FONDO_MAPA: Color = Color(0xFF0F151D)
private val COLOR_PARQUE_MAPA: Color = Color(0xFF14291E)
private val COLOR_RIO_MAPA: Color = Color(0xFF122234)
private val COLOR_CALLE_MAPA: Color = Color(0xFF1D2633)
private val COLOR_AVENIDA_MAPA: Color = Color(0xFF263344)

private val CIUDADES_DISPONIBLES: List<String> = listOf(
    "Piura",
    "Lima",
    "Trujillo",
    "Arequipa",
    "Chiclayo",
    "Cusco",
)

/**
 * Pantalla de búsqueda de canchas con mapa interactivo y tarjeta inferior flotante (Mockup página 5).
 * Ahora conectada con datos reales de Firestore y selector de ciudad.
 *
 * @param onCrearPartidoAqui Acción invocada para abrir la publicación prellenada con la cancha elegida.
 * @param modifier Modificador de Compose.
 */
@Composable
fun MapaScreen(
    onCrearPartidoAqui: (nombreCancha: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CanchasViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isDialogoCiudadesVisible by remember { mutableStateOf(false) }

    val baseCoords = FirestoreCanchasRepository.obtenerCoordenadaBaseCiudad(state.ciudad)

    if (isDialogoCiudadesVisible) {
        SelectorCiudadMapaDialog(
            ciudades = CIUDADES_DISPONIBLES,
            ciudadSeleccionada = state.ciudad,
            onCiudadSeleccionada = { ciudad ->
                viewModel.onCiudadChange(ciudad)
                isDialogoCiudadesVisible = false
            },
            onDismiss = { isDialogoCiudadesVisible = false },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FulboColors.Background),
    ) {
        MapaCanvas(modifier = Modifier.fillMaxSize())

        val minLat = state.canchas.minOfOrNull { it.latitud } ?: baseCoords.first
        val maxLat = state.canchas.maxOfOrNull { it.latitud } ?: baseCoords.first
        val minLng = state.canchas.minOfOrNull { it.longitud } ?: baseCoords.second
        val maxLng = state.canchas.maxOfOrNull { it.longitud } ?: baseCoords.second

        val latRange = (maxLat - minLat).coerceAtLeast(0.005)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.005)

        // Marcadores dinámicos: se distribuyen en el lienzo según sus coordenadas reales
        state.canchas.forEach { cancha ->
            val isCanchaSeleccionada = cancha.id == state.canchaSeleccionada?.id

            val normY = if (state.canchas.size <= 1) 0.5f else {
                (((maxLat - cancha.latitud) / latRange).toFloat()).coerceIn(0.08f, 0.92f)
            }
            val normX = if (state.canchas.size <= 1) 0.5f else {
                (((cancha.longitud - minLng) / lngRange).toFloat()).coerceIn(0.08f, 0.92f)
            }

            // Visible range: top is below search row (175dp), bottom is above card (card is 230dp high)
            val yOffset = 180.dp + (250.dp * normY)
            val xOffset = 28.dp + (270.dp * normX)

            CanchaPinMarcador(
                cancha = cancha,
                xDp = xOffset,
                yDp = yOffset,
                isSeleccionada = isCanchaSeleccionada,
                onClick = { viewModel.onCanchaSeleccionada(cancha) },
            )
        }

        MapaBuscadorRow(
            busqueda = state.busqueda,
            onBusquedaChange = viewModel::onBusquedaChange,
            ciudad = state.ciudad,
            onAbrirCiudades = { isDialogoCiudadesVisible = true },
            filtros = state.filtros,
            onToggleFiltro = viewModel::onFiltroClick,
        )

        state.canchaSeleccionada?.let { cancha ->
            CanchaDetalleCard(
                cancha = cancha,
                onCrearPartido = { onCrearPartidoAqui(cancha.nombre) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun MapaCanvas(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val ancho = size.width
        val alto = size.height

        drawRect(COLOR_FONDO_MAPA)

        // Áreas verdes / parques
        drawCircle(COLOR_PARQUE_MAPA, radius = ancho * 0.35f, center = Offset(ancho * 0.2f, alto * 0.4f))
        drawCircle(COLOR_PARQUE_MAPA, radius = ancho * 0.25f, center = Offset(ancho * 0.8f, alto * 0.7f))

        // Canal / río
        drawLine(
            color = COLOR_RIO_MAPA,
            start = Offset(0f, alto * 0.25f),
            end = Offset(ancho, alto * 0.32f),
            strokeWidth = 24.dp.toPx(),
        )

        // Cuadrícula de avenidas y calles
        for (indiceHorizontal in 1..8) {
            val isAvenida = indiceHorizontal % 3 == 0
            drawLine(
                color = if (isAvenida) COLOR_AVENIDA_MAPA else COLOR_CALLE_MAPA,
                start = Offset(0f, alto * (indiceHorizontal * 0.11f)),
                end = Offset(ancho, alto * (indiceHorizontal * 0.11f) + (indiceHorizontal * 5)),
                strokeWidth = if (isAvenida) 5.dp.toPx() else 2.5.dp.toPx(),
            )
        }

        for (indiceVertical in 1..6) {
            val isAvenida = indiceVertical % 2 == 0
            drawLine(
                color = if (isAvenida) COLOR_AVENIDA_MAPA else COLOR_CALLE_MAPA,
                start = Offset(ancho * (indiceVertical * 0.16f), 0f),
                end = Offset(ancho * (indiceVertical * 0.16f) + 20, alto),
                strokeWidth = if (isAvenida) 5.dp.toPx() else 2.5.dp.toPx(),
            )
        }
    }
}

@Composable
private fun CanchaPinMarcador(
    cancha: Cancha,
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    isSeleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = xDp, y = yDp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isSeleccionada) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(FulboColors.Green.copy(alpha = 0.25f), CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(6.dp, CircleShape)
                    .background(if (isSeleccionada) FulboColors.Green else FulboColors.Surface, CircleShape)
                    .border(2.dp, if (isSeleccionada) FulboColors.OnGreen else FulboColors.Outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = cancha.nombre,
                    tint = if (isSeleccionada) FulboColors.OnGreen else FulboColors.Green,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun MapaBuscadorRow(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    ciudad: String,
    onAbrirCiudades: () -> Unit,
    filtros: Set<FiltroCancha>,
    onToggleFiltro: (FiltroCancha) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screen, vertical = spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusquedaChange,
                placeholder = { Text("Buscar canchas en $ciudad", color = FulboColors.TextDisabled) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = FulboColors.TextSecondary)
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FulboColors.Surface,
                    unfocusedContainerColor = FulboColors.Surface,
                    focusedBorderColor = FulboColors.Green,
                    unfocusedBorderColor = FulboColors.Outline,
                    focusedTextColor = FulboColors.TextPrimary,
                    unfocusedTextColor = FulboColors.TextPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .shadow(8.dp, CircleShape),
            )

            // Botón selector de ciudad
            Surface(
                shape = CircleShape,
                color = FulboColors.Surface,
                border = BorderStroke(1.dp, FulboColors.Green),
                modifier = Modifier
                    .height(52.dp)
                    .clickable(onClick = onAbrirCiudades)
                    .shadow(8.dp, CircleShape),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Ciudad",
                        tint = FulboColors.Green,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = ciudad,
                        color = FulboColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = FulboColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            FulboChip(
                text = "Abiertas ahora",
                isSelected = FiltroCancha.ABIERTO_AHORA in filtros,
                onClick = { onToggleFiltro(FiltroCancha.ABIERTO_AHORA) },
            )
            FulboChip(
                text = "Cerca de mí",
                isSelected = FiltroCancha.CERCA_DE_MI in filtros,
                onClick = { onToggleFiltro(FiltroCancha.CERCA_DE_MI) },
            )
            FulboChip(
                text = "Fútbol 7",
                isSelected = FiltroCancha.FUTBOL_7 in filtros,
                onClick = { onToggleFiltro(FiltroCancha.FUTBOL_7) },
            )
            FulboChip(
                text = "Sintético",
                isSelected = FiltroCancha.SINTETICO in filtros,
                onClick = { onToggleFiltro(FiltroCancha.SINTETICO) },
            )
            FulboChip(
                text = "Techada",
                isSelected = FiltroCancha.TECHADA in filtros,
                onClick = { onToggleFiltro(FiltroCancha.TECHADA) },
            )
        }
    }
}

@Composable
private fun CanchaDetalleCard(
    cancha: Cancha,
    onCrearPartido: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val uriHandler = LocalUriHandler.current

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = FulboColors.Surface,
        border = BorderStroke(1.dp, FulboColors.Outline),
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.screen),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(FulboColors.Outline),
            )

            Spacer(Modifier.height(spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cancha.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FulboColors.TextPrimary,
                    )
                    Text(
                        text = cancha.direccion.ifBlank { "${cancha.distrito}, ${cancha.ciudad}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = FulboColors.TextSecondary,
                    )
                }
                StatusPill(
                    text = if (cancha.activa) "Abierto" else "Cerrado",
                    color = if (cancha.activa) FulboColors.Green else FulboColors.Red,
                    isFilled = true,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${cancha.horarioTexto} · ${cancha.distanciaTexto}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FulboColors.TextSecondary,
                )
                Text(
                    text = "·",
                    color = FulboColors.TextDisabled,
                )
                Text(
                    text = "S/ ${cancha.precioHora.toInt()}/h",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = FulboColors.Green,
                )
                Text(
                    text = "· ★ ${cancha.rating}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FulboColors.Orange,
                )
            }

            Spacer(Modifier.height(spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                FulboSecondaryButton(
                    text = "Cómo llegar",
                    onClick = {
                        val query = "${cancha.nombre}, ${cancha.distrito}, ${cancha.ciudad}, Peru".replace(" ", "+")
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$query")
                    },
                    modifier = Modifier.weight(1f),
                )

                FulboPrimaryButton(
                    text = "Crear partido aquí",
                    onClick = onCrearPartido,
                    modifier = Modifier.weight(1.3f),
                )
            }
        }
    }
}

@Composable
private fun SelectorCiudadMapaDialog(
    ciudades: List<String>,
    ciudadSeleccionada: String,
    onCiudadSeleccionada: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige tu ciudad", color = FulboColors.TextPrimary) },
        containerColor = FulboColors.Surface,
        text = {
            Column {
                ciudades.forEach { ciudad ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCiudadSeleccionada(ciudad) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = ciudad == ciudadSeleccionada,
                            onClick = { onCiudadSeleccionada(ciudad) },
                            colors = RadioButtonDefaults.colors(selectedColor = FulboColors.Green),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(ciudad, style = MaterialTheme.typography.bodyLarge, color = FulboColors.TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = FulboColors.Green)
            }
        },
    )
}
