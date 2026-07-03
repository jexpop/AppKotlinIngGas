package com.jexpop.appkotlininggas.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isEncryptionConfigured by viewModel.isEncryptionConfigured.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordWarning by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is SettingsState.Success) {
            viewModel.resetState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Usuario
        Text(
            text = stringResource(R.string.settings_user),
            style = MaterialTheme.typography.titleMedium
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = userEmail ?: "—",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Solo el admin puede configurar el cifrado
        if (viewModel.isAdmin()) {
            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_encryption),
                style = MaterialTheme.typography.titleMedium
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_encryption_password),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                if (isEncryptionConfigured) R.string.settings_encryption_configured
                                else R.string.settings_encryption_not_configured
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEncryptionConfigured)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = {
                            if (isEncryptionConfigured) {
                                showChangePasswordWarning = true
                            } else {
                                showPasswordDialog = true
                            }
                        }
                    ) {
                        Text(if (isEncryptionConfigured) "Cambiar" else "Configurar")
                    }
                }
            }

            if (state is SettingsState.Error) {
                Text(
                    text = (state as SettingsState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (state is SettingsState.Success) {
                Text(
                    text = stringResource(R.string.settings_encryption_password_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider()

        // Cerrar sesión
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_logout))
        }

        // Versión
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${stringResource(R.string.settings_app_version)} 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Diálogo contraseña
    if (showPasswordDialog) {
        PasswordDialog(
            onConfirm = { password, confirm ->
                if (viewModel.saveEncryptionPassword(password, confirm)) {
                    showPasswordDialog = false
                }
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    // Aviso cambio contraseña
    if (showChangePasswordWarning) {
        AlertDialog(
            onDismissRequest = { showChangePasswordWarning = false },
            title = { Text("⚠️ Cambiar contraseña") },
            text = {
                Text("Si cambias la contraseña, los ficheros anteriores seguirán cifrados con la contraseña antigua y no podrás descifrarlos con la nueva.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showChangePasswordWarning = false
                    showPasswordDialog = true
                }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordWarning = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Diálogo logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current

                // En el diálogo de logout:
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut(context, onLogout)
                }) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun PasswordDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_encryption_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_encryption_password_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.settings_encryption_password_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password, confirmPassword) },
                enabled = password.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}