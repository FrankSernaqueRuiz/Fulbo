package pe.fulbo.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pe.fulbo.app.core.designsystem.component.FulboPrimaryButton
import pe.fulbo.app.core.designsystem.component.FulboTextField
import pe.fulbo.app.core.designsystem.theme.FulboColors
import pe.fulbo.app.core.designsystem.theme.spacing
import pe.fulbo.app.feature.auth.domain.RegistroValidator

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/**
 * Onboarding Paso 1 de 2: Crea tu perfil (Mockup página 1).
 *
 * Registra foto de perfil, nombre, apellido, alias, correo, contraseña, fecha de nacimiento
 * (seleccionando día, mes y año) y aceptación de términos.
 */
@Composable
fun CreaTuPerfilScreen(
    onSiguiente: (nombre: String, apellido: String, alias: String, correo: String, contrasena: String, fechaNacimiento: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellido by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Fecha separada por Día, Mes y Año
    var diaSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
    var mesSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) } // 1 a 12
    var anioSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) }

    var aceptaTerminos by rememberSaveable { mutableStateOf(false) }
    var hasFotoSeleccionada by rememberSaveable { mutableStateOf(false) }

    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorApellido by remember { mutableStateOf<String?>(null) }
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorContrasena by remember { mutableStateOf<String?>(null) }
    var errorFecha by remember { mutableStateOf<String?>(null) }
    var errorTerminos by remember { mutableStateOf<String?>(null) }

    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val iniciales = listOf(nombre.take(1), apellido.take(1))
        .filter { it.isNotBlank() }
        .joinToString("")
        .uppercase()

    val anioActual = RegistroValidator.hoy().year
    val aniosDisponibles = remember { (anioActual - 90..anioActual - 10).reversed().toList() }
    val maxDiasEnMes = remember(diaSeleccionado, mesSeleccionado, anioSeleccionado) {
        calcularDiasEnMes(mesSeleccionado ?: 1, anioSeleccionado ?: 2000)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FulboColors.Background)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = spacing.screen, vertical = spacing.sm),
    ) {
        Spacer(Modifier.height(spacing.xs))

        Text(
            text = "Paso 1 de 2",
            style = MaterialTheme.typography.labelMedium,
            color = FulboColors.Green,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "Crea tu perfil",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FulboColors.TextPrimary,
        )

        Text(
            text = "Para que otros sepan con quién van a jugar.",
            style = MaterialTheme.typography.bodyMedium,
            color = FulboColors.TextSecondary,
        )

        Spacer(Modifier.height(spacing.sm))

        // Selector circular "Subir foto" (más compacto para dejar espacio al teclado)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val bordeColor = if (hasFotoSeleccionada) FulboColors.Green else FulboColors.Outline
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(FulboColors.Surface)
                        .border(1.5.dp, bordeColor, CircleShape)
                        .clickable { hasFotoSeleccionada = !hasFotoSeleccionada },
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasFotoSeleccionada && iniciales.isNotEmpty()) {
                        Text(
                            text = iniciales,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FulboColors.Green,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Subir foto",
                            tint = FulboColors.TextSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Text(
                    text = if (hasFotoSeleccionada) "Foto seleccionada" else "Subir foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasFotoSeleccionada) FulboColors.Green else FulboColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(spacing.sm))

        // Campos de texto para datos personales con navegación fluida por teclado
        FulboTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                errorNombre = null
            },
            label = "Nombre",
            placeholder = "Tu nombre",
            isError = errorNombre != null,
            supportingText = errorNombre,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )
        Spacer(Modifier.height(spacing.sm))

        FulboTextField(
            value = apellido,
            onValueChange = {
                apellido = it
                errorApellido = null
            },
            label = "Apellido",
            placeholder = "Tu apellido",
            isError = errorApellido != null,
            supportingText = errorApellido,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )
        Spacer(Modifier.height(spacing.sm))

        FulboTextField(
            value = alias,
            onValueChange = { alias = it },
            label = "Alias (opcional)",
            placeholder = "Ej: Chino, Lucho, Rayo...",
            supportingText = "Tu alias es el nombre que verán los demás en los partidos.",
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )
        Spacer(Modifier.height(spacing.sm))

        FulboTextField(
            value = correo,
            onValueChange = {
                correo = it
                errorCorreo = null
            },
            label = "Correo electrónico",
            placeholder = "tu@correo.com",
            isError = errorCorreo != null,
            supportingText = errorCorreo,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )
        Spacer(Modifier.height(spacing.sm))

        FulboTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                errorContrasena = null
            },
            label = "Contraseña",
            placeholder = "Mínimo 8 caracteres, letras y números",
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = errorContrasena != null,
            supportingText = errorContrasena,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = FulboColors.TextSecondary,
                    )
                }
            },
        )
        Spacer(Modifier.height(spacing.md))

        // Selector de Fecha de Nacimiento separado por Día, Mes y Año
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Fecha de nacimiento",
                style = MaterialTheme.typography.labelMedium,
                color = if (errorFecha != null) FulboColors.Red else FulboColors.TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Selector Día
                Box(modifier = Modifier.weight(1f)) {
                    SelectorDropdownField(
                        label = diaSeleccionado?.toString() ?: "Día",
                        items = (1..maxDiasEnMes).map { it.toString() },
                        onItemSelected = {
                            diaSeleccionado = it.toInt()
                            errorFecha = null
                        },
                        isError = errorFecha != null,
                    )
                }

                // Selector Mes
                Box(modifier = Modifier.weight(1.5f)) {
                    SelectorDropdownField(
                        label = mesSeleccionado?.let { MESES[it - 1] } ?: "Mes",
                        items = MESES,
                        onItemSelected = { mesNombre ->
                            val nuevoMes = MESES.indexOf(mesNombre) + 1
                            mesSeleccionado = nuevoMes
                            // Si el día actual excede el nuevo mes, ajustarlo
                            val max = calcularDiasEnMes(nuevoMes, anioSeleccionado ?: 2000)
                            if (diaSeleccionado != null && diaSeleccionado!! > max) {
                                diaSeleccionado = max
                            }
                            errorFecha = null
                        },
                        isError = errorFecha != null,
                    )
                }

                // Selector Año
                Box(modifier = Modifier.weight(1.2f)) {
                    SelectorDropdownField(
                        label = anioSeleccionado?.toString() ?: "Año",
                        items = aniosDisponibles.map { it.toString() },
                        onItemSelected = {
                            val nuevoAnio = it.toInt()
                            anioSeleccionado = nuevoAnio
                            val max = calcularDiasEnMes(mesSeleccionado ?: 1, nuevoAnio)
                            if (diaSeleccionado != null && diaSeleccionado!! > max) {
                                diaSeleccionado = max
                            }
                            errorFecha = null
                        },
                        isError = errorFecha != null,
                    )
                }
            }

            Text(
                text = errorFecha ?: "Debes tener al menos 16 años para participar.",
                style = MaterialTheme.typography.bodySmall,
                color = if (errorFecha != null) FulboColors.Red else FulboColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }

        Spacer(Modifier.height(spacing.md))

        // Términos y condiciones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    aceptaTerminos = !aceptaTerminos
                    errorTerminos = null
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = aceptaTerminos,
                onCheckedChange = {
                    aceptaTerminos = it
                    errorTerminos = null
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = FulboColors.Green,
                    uncheckedColor = FulboColors.Outline,
                    checkmarkColor = FulboColors.OnGreen,
                ),
            )
            Text(
                text = "Acepto los Términos y Condiciones y la Política de Privacidad de FULBO.",
                style = MaterialTheme.typography.bodySmall,
                color = if (errorTerminos != null) FulboColors.Red else FulboColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(Modifier.height(spacing.xl))

        FulboPrimaryButton(
            text = "Siguiente",
            onClick = {
                val vNombre = RegistroValidator.validarNombre(nombre, "nombre")
                val vApellido = RegistroValidator.validarNombre(apellido, "apellido")
                val vCorreo = RegistroValidator.validarCorreo(correo)
                val vPass = RegistroValidator.validarPassword(contrasena)

                val fechaParsed = if (diaSeleccionado != null && mesSeleccionado != null && anioSeleccionado != null) {
                    try {
                        LocalDate(anioSeleccionado!!, mesSeleccionado!!, diaSeleccionado!!)
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }

                val vFecha = RegistroValidator.validarEdad(fechaParsed)
                val vTerminos = if (!aceptaTerminos) "Debes aceptar los términos y condiciones" else null

                errorNombre = vNombre
                errorApellido = vApellido
                errorCorreo = vCorreo
                errorContrasena = vPass
                errorFecha = vFecha
                errorTerminos = vTerminos

                if (vNombre == null && vApellido == null && vCorreo == null &&
                    vPass == null && vFecha == null && vTerminos == null && fechaParsed != null
                ) {
                    val diaStr = diaSeleccionado!!.toString().padStart(2, '0')
                    val mesStr = mesSeleccionado!!.toString().padStart(2, '0')
                    val fechaFormateada = "${anioSeleccionado!!}-$mesStr-$diaStr"

                    onSiguiente(
                        nombre.trim(),
                        apellido.trim(),
                        alias.trim().ifBlank { nombre.trim() },
                        correo.trim(),
                        contrasena,
                        fechaFormateada,
                    )
                }
            },
        )
        // Espaciador extra para que al scrollear no quede pegado al teclado
        Spacer(Modifier.height(spacing.xl))
    }
}

@Composable
private fun SelectorDropdownField(
    label: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(FulboColors.Surface)
                .border(
                    width = 1.dp,
                    color = if (isError) FulboColors.Red else FulboColors.Outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (label in listOf("Día", "Mes", "Año")) FulboColors.TextDisabled else FulboColors.TextPrimary,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = FulboColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(FulboColors.SurfaceHigh)
                .height(260.dp),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            color = FulboColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = FulboColors.TextPrimary,
                    ),
                )
            }
        }
    }
}

private fun calcularDiasEnMes(mes: Int, anio: Int): Int = when (mes) {
    2 -> if ((anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}
