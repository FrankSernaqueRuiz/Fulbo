package pe.fulbo.app.feature.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.fulbo.app.core.designsystem.component.FulboCard
import pe.fulbo.app.core.designsystem.component.RankAvatar
import pe.fulbo.app.core.designsystem.component.StarRatingBar
import pe.fulbo.app.core.designsystem.theme.FulboColors
import pe.fulbo.app.core.designsystem.theme.spacing
import pe.fulbo.app.core.model.RangoEquipo
import pe.fulbo.app.core.model.ResenaJugador

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.fulbo.app.core.designsystem.component.FulboSecondaryButton
import pe.fulbo.app.feature.auth.domain.AuthRepository

import androidx.compose.runtime.collectAsState
import pe.fulbo.app.core.model.PerfilUsuario

private const val PUNTAJE_MAXIMO_CONDUCTA: Float = 5.0f

/**
 * Pantalla de perfil de jugador con reputación, desglose de conducta y reseñas (Mockup página 4).
 *
 * @param onCerrarSesion Callback cuando el usuario cierra sesión o elimina su cuenta.
 * @param modifier Modificador de Compose.
 */
@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit = {},
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = koinInject(),
) {
    val spacing = MaterialTheme.spacing
    val scope = rememberCoroutineScope()
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val perfilReal by authRepository.observarMiPerfil().collectAsState(initial = null)
    val resenasReales by authRepository.observarMisResenas().collectAsState(initial = emptyList())

    val nombreMostrar = perfilReal?.let { "${it.nombre} ${it.apellido}".trim() }?.ifBlank { null } ?: "Mi Perfil"
    val aliasMostrar = perfilReal?.alias?.ifBlank { null } ?: "Jugador"
    val posicionMostrar = perfilReal?.posicion?.replaceFirstChar { it.uppercase() } ?: "Medio"
    val nivelMostrar = perfilReal?.nivel?.replaceFirstChar { it.uppercase() } ?: "Intermedio"
    val inicialesMostrar = perfilReal?.iniciales ?: "FP"
    val rangoMostrar = if (perfilReal?.plan == "premium") RangoEquipo.ORO else RangoEquipo.PLATA

    val partidosJugadosMostrar = perfilReal?.partidosJugados ?: 0
    val faltasMostrar = perfilReal?.faltas ?: 0
    val ratingMostrar = perfilReal?.repGlobal
    val resenasMostrar = perfilReal?.numCalificaciones ?: 0
    val puntPuntualidad = perfilReal?.repPuntualidad
    val puntAsistencia = perfilReal?.repAsistencia
    val puntPago = perfilReal?.repPago
    val puntTrato = perfilReal?.repTrato

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) mostrarDialogoEliminar = false },
            title = {
                Text(
                    text = "Eliminar cuenta permanentemente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FulboColors.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer. Se borrarán todos tus datos personales, reputación e historial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FulboColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            authRepository.eliminarCuenta()
                            isProcessing = false
                            mostrarDialogoEliminar = false
                            onCerrarSesion()
                        }
                    },
                    enabled = !isProcessing,
                ) {
                    Text("Eliminar definitivamente", color = FulboColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoEliminar = false },
                    enabled = !isProcessing,
                ) {
                    Text("Cancelar", color = FulboColors.TextSecondary)
                }
            },
            containerColor = FulboColors.Surface,
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FulboColors.Background),
        contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            PerfilCabecera(
                nombre = nombreMostrar,
                alias = aliasMostrar,
                posicion = posicionMostrar,
                nivel = nivelMostrar,
                iniciales = inicialesMostrar,
                rango = rangoMostrar,
            )
        }

        item {
            PerfilReputacionCard(
                rating = ratingMostrar,
                totalResenas = resenasMostrar,
                puntajePuntualidad = puntPuntualidad,
                puntajeAsistencia = puntAsistencia,
                puntajePago = puntPago,
                puntajeTrato = puntTrato,
            )
        }

        item {
            PerfilEstadisticasRow(
                partidosJugados = partidosJugadosMostrar,
                semanasRacha = 1,
                faltas = faltasMostrar,
            )
        }

        item {
            PerfilResenasSection(resenas = resenasReales)
        }

        item {
            Spacer(Modifier.height(spacing.sm))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                FulboSecondaryButton(
                    text = "Cerrar sesión",
                    isEnabled = !isProcessing,
                    onClick = {
                        scope.launch {
                            authRepository.cerrarSesion()
                            onCerrarSesion()
                        }
                    },
                )

                TextButton(
                    onClick = { mostrarDialogoEliminar = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                ) {
                    Text(
                        text = "Eliminar mi cuenta",
                        color = FulboColors.Red,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.height(spacing.lg))
            }
        }
    }
}

@Composable
private fun PerfilCabecera(
    nombre: String,
    alias: String,
    posicion: String,
    nivel: String,
    iniciales: String,
    rango: RangoEquipo,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.sm))
        RankAvatar(
            iniciales = iniciales,
            rango = rango,
            size = 84.dp,
        )
        Spacer(Modifier.height(spacing.sm))

        Text(
            text = nombre,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FulboColors.TextPrimary,
        )

        Text(
            text = "“$alias” · $posicion · $nivel",
            style = MaterialTheme.typography.bodyLarge,
            color = FulboColors.TextSecondary,
        )
    }
}

@Composable
private fun PerfilReputacionCard(
    rating: Double?,
    totalResenas: Int,
    puntajePuntualidad: Float?,
    puntajeAsistencia: Float?,
    puntajePago: Float?,
    puntajeTrato: Float?,
    modifier: Modifier = Modifier,
) {
    FulboCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                if (rating != null && totalResenas > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = rating.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = FulboColors.TextPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "de 5",
                            style = MaterialTheme.typography.titleMedium,
                            color = FulboColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(
                        text = "$totalResenas reseñas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FulboColors.TextSecondary,
                    )
                } else {
                    Text(
                        text = "Nuevo jugador",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FulboColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sin calificaciones aún",
                        style = MaterialTheme.typography.bodySmall,
                        color = FulboColors.TextSecondary,
                    )
                }
            }

            if (rating != null && totalResenas > 0) {
                StarRatingBar(rating = rating, starSize = 22.dp)
            }
        }

        Spacer(Modifier.height(18.dp))

        MetricaDesgloseFila(titulo = "Puntualidad", puntaje = puntajePuntualidad)
        Spacer(Modifier.height(10.dp))
        MetricaDesgloseFila(titulo = "Asiste", puntaje = puntajeAsistencia)
        Spacer(Modifier.height(10.dp))
        MetricaDesgloseFila(titulo = "Paga su parte", puntaje = puntajePago)
        Spacer(Modifier.height(10.dp))
        MetricaDesgloseFila(titulo = "Buen trato", puntaje = puntajeTrato)
    }
}

@Composable
private fun MetricaDesgloseFila(
    titulo: String,
    puntaje: Float?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = FulboColors.TextPrimary,
            modifier = Modifier.width(100.dp),
        )

        LinearProgressIndicator(
            progress = { if (puntaje != null) (puntaje / PUNTAJE_MAXIMO_CONDUCTA) else 0f },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
            color = FulboColors.Green,
            trackColor = FulboColors.SurfaceHigh,
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = puntaje?.toString() ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (puntaje != null) FulboColors.TextPrimary else FulboColors.TextDisabled,
        )
    }
}

@Composable
private fun PerfilEstadisticasRow(
    partidosJugados: Int,
    semanasRacha: Int,
    faltas: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        StatCard(
            valor = partidosJugados.toString(),
            etiqueta = "Partidos",
            icono = Icons.Outlined.SportsSoccer,
            color = FulboColors.Green,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            valor = "$semanasRacha sem",
            etiqueta = "Racha",
            icono = Icons.Outlined.LocalFireDepartment,
            color = FulboColors.Orange,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            valor = faltas.toString(),
            etiqueta = "Faltas",
            icono = Icons.Outlined.EventBusy,
            color = FulboColors.Red,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = FulboColors.Surface,
        border = BorderStroke(1.dp, FulboColors.Outline),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FulboColors.TextPrimary,
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = FulboColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun PerfilResenasSection(
    resenas: List<ResenaJugador>,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Reseñas recientes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = FulboColors.TextPrimary,
            modifier = Modifier.padding(vertical = spacing.xs),
        )

        Spacer(Modifier.height(spacing.xs))

        if (resenas.isEmpty()) {
            FulboCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.md, horizontal = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Aún no tienes reseñas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FulboColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Juega partidos con otros futbolistas para recibir calificaciones y comentarios de tus compañeros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FulboColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            resenas.forEach { resena ->
                FulboCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        text = "“${resena.texto}”",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = FulboColors.TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "— ${resena.autor}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FulboColors.Green,
                    )
                }
            }
        }
    }
}
