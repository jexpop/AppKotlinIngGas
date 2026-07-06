package com.jexpop.appkotlininggas.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R
import androidx.activity.result.contract.ActivityResultContracts
import com.jexpop.appkotlininggas.data.DriveAuthManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager


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

    val isDriveConnected by viewModel.isDriveConnected.collectAsState()
    val context = LocalContext.current
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.checkDriveConnection(context)
        }
    }

    //Github
    val tokenExpiry by viewModel.tokenExpiry.collectAsState()
    val tokenExpiryWarning by viewModel.tokenExpiryWarning.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val githubRepoBackup by viewModel.githubRepoBackup.collectAsState()
    val githubRepoPublic by viewModel.githubRepoPublic.collectAsState()
    val githubUsername by viewModel.githubUsername.collectAsState()
    var showGithubDialog by remember { mutableStateOf(false) }
    var showRenewalSteps by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.checkDriveConnection(context)
    }

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

            // Mostrar salt actual (solo admin, solo si hay password configurada)
            if (isEncryptionConfigured) {
                val saltBase64 = remember { mutableStateOf<String?>(null) }
                val showSalt = remember { mutableStateOf(false) }

                LaunchedEffect(showSalt.value) {
                    if (showSalt.value && saltBase64.value == null) {
                        saltBase64.value = viewModel.getEncryptionSaltBase64()
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Salt de cifrado (Base64)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Necesario para descifrar backups en scripts externos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showSalt.value = !showSalt.value }
                            ) {
                                Text(if (showSalt.value) "Ocultar" else "Mostrar")
                            }
                        }

                        if (showSalt.value && saltBase64.value != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = saltBase64.value!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.weight(1f),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Encryption Salt", saltBase64.value!!))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.settings_drive),
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
                        text = stringResource(R.string.settings_drive),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            if (isDriveConnected) R.string.settings_drive_connected
                            else R.string.settings_drive_not_connected
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDriveConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    onClick = {
                        if (isDriveConnected) {
                            viewModel.disconnectDrive(context)
                        } else {
                            driveSignInLauncher.launch(DriveAuthManager.getSignInIntent(context))
                        }
                    }
                ) {
                    Text(
                        if (isDriveConnected) stringResource(R.string.settings_drive_disconnect)
                        else stringResource(R.string.settings_drive_connect)
                    )
                }
            }
        }

        // Github
        HorizontalDivider()

        Text(
            text = stringResource(R.string.settings_github),
            style = MaterialTheme.typography.titleMedium
        )

        // Estado del token
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_github_token),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (val status = tokenExpiryWarning) {
                                is TokenExpiryStatus.Ok -> stringResource(R.string.settings_github_token_ok)
                                is TokenExpiryStatus.Expired -> stringResource(R.string.settings_github_token_expired)
                                is TokenExpiryStatus.Warning -> stringResource(R.string.settings_github_token_warning, status.daysLeft)
                                is TokenExpiryStatus.Unknown -> stringResource(R.string.settings_github_token_unknown)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (tokenExpiryWarning) {
                                is TokenExpiryStatus.Ok -> MaterialTheme.colorScheme.primary
                                is TokenExpiryStatus.Expired -> MaterialTheme.colorScheme.error
                                is TokenExpiryStatus.Warning -> MaterialTheme.colorScheme.tertiary
                                is TokenExpiryStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    TextButton(onClick = { showGithubDialog = true }) {
                        Text("Editar")
                    }
                }

                // Instrucciones de renovación
                TextButton(
                    onClick = { showRenewalSteps = !showRenewalSteps },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.settings_github_renewal_title),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (showRenewalSteps) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_github_renewal_steps),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

    // Diálogo edición de Github
    if (showGithubDialog) {
        GithubDialog(
            token = githubToken ?: "",
            expiry = tokenExpiry ?: "",
            repoBackup = githubRepoBackup ?: "",
            repoPublic = githubRepoPublic ?: "",
            username = githubUsername ?: "",
            onConfirm = { token, expiry, repoBackup, repoPublic, username ->
                if (token.isNotBlank()) viewModel.updateGithubToken(token)
                if (expiry.isNotBlank()) viewModel.updateTokenExpiry(expiry)
                if (repoBackup.isNotBlank()) viewModel.updateGithubRepoBackup(repoBackup)
                if (repoPublic.isNotBlank()) viewModel.updateGithubRepoPublic(repoPublic)
                if (username.isNotBlank()) viewModel.updateGithubUsername(username)
                showGithubDialog = false
            },
            onDismiss = { showGithubDialog = false }
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


// Github
@Composable
fun GithubDialog(
    token: String,
    expiry: String,
    repoBackup: String,
    repoPublic: String,
    username: String,
    onConfirm: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var newToken by remember { mutableStateOf(token) }
    var newExpiry by remember { mutableStateOf(expiry) }
    var newRepoBackup by remember { mutableStateOf(repoBackup) }
    var newRepoPublic by remember { mutableStateOf(repoPublic) }
    var newUsername by remember { mutableStateOf(username) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_github)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text(stringResource(R.string.settings_github_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newRepoPublic,
                    onValueChange = { newRepoPublic = it },
                    label = { Text(stringResource(R.string.settings_github_repo_public)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newRepoBackup,
                    onValueChange = { newRepoBackup = it },
                    label = { Text(stringResource(R.string.settings_github_repo_backup)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text(stringResource(R.string.settings_github_token)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newExpiry,
                    onValueChange = { newExpiry = it },
                    label = { Text(stringResource(R.string.settings_github_token_expiry)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(newToken, newExpiry, newRepoBackup, newRepoPublic, newUsername)
                }
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