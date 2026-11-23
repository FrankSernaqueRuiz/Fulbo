package pe.fulbo.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.fulbo.app.core.designsystem.component.FulboPrimaryButton
import pe.fulbo.app.core.designsystem.component.FulboTextField
import pe.fulbo.app.core.designsystem.theme.FulboColors
import pe.fulbo.app.core.designsystem.theme.spacing
import pe.fulbo.app.core.util.Resultado
import pe.fulbo.app.feature.auth.domain.AuthRepository
import pe.fulbo.app.feature.auth.domain.RegistroValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onVolver: () -> Unit,
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = koinInject(),
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorPassword by remember { mutableStateOf<String?>(null) }
    var errorGeneral by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = FulboColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Iniciar sesión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FulboColors.Background,
                    titleContentColor = FulboColors.TextPrimary,
                    navigationIconContentColor = FulboColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.screen)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Spacer(Modifier.height(spacing.sm))

            Text(
                text = "Bienvenido de nuevo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = FulboColors.TextPrimary,
            )
            Text(
                text = "Ingresa con tu correo registrado para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = FulboColors.TextSecondary,
            )

            Spacer(Modifier.height(spacing.sm))

            FulboTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    errorCorreo = null
                    errorGeneral = null
                },
                label = "Correo electrónico",
                placeholder = "ejemplo@correo.com",
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

            FulboTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorPassword = null
                    errorGeneral = null
                },
                label = "Contraseña",
                placeholder = "••••••••",
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = errorPassword != null,
                supportingText = errorPassword,
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

            if (errorGeneral != null) {
                Text(
                    text = errorGeneral ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FulboColors.Red,
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(spacing.lg))

            FulboPrimaryButton(
                text = "Entrar",
                isLoading = isLoading,
                onClick = {
                    val valCorreo = RegistroValidator.validarCorreo(correo)
                    val valPass = if (password.isBlank()) "Ingresa tu contraseña" else null
                    errorCorreo = valCorreo
                    errorPassword = valPass

                    if (valCorreo == null && valPass == null) {
                        isLoading = true
                        errorGeneral = null
                        scope.launch {
                            when (authRepository.iniciarSesion(correo, password)) {
                                is Resultado.Exito -> {
                                    isLoading = false
                                    onLoginExitoso()
                                }
                                is Resultado.Fallo -> {
                                    isLoading = false
                                    errorGeneral = "Correo o contraseña incorrectos. Verifica tus datos."
                                }
                            }
                        }
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¿Aún no tienes cuenta?", color = FulboColors.TextSecondary)
                TextButton(onClick = onIrARegistro) {
                    Text("Regístrate aquí", color = FulboColors.Green, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(spacing.md))
        }
    }
}
