package com.jexpop.appkotlininggas.ui.screens.importcsv

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val selectedBank by viewModel.selectedBank.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver
                .openInputStream(it)
                ?.bufferedReader()
                ?.readText() ?: return@let
            viewModel.importCsv(content, context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.import_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Combo de bancos
        if (banks.size > 1) {
            Text(
                stringResource(R.string.import_bank_label),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedBank?.name ?: stringResource(R.string.import_bank_select),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    banks.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.name) },
                            onClick = {
                                viewModel.selectBank(bank)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (banks.size == 1) {
            Text(
                text = stringResource(R.string.import_bank_prefix, banks.first().name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Botón seleccionar fichero
        Button(
            onClick = { launcher.launch("text/*") },
            enabled = state !is ImportState.Loading && selectedBank != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.import_button))
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val current = state) {
            is ImportState.Idle -> {}
            is ImportState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.import_loading))
            }
            is ImportState.Success -> {
                Text(
                    text = stringResource(R.string.import_success, current.count),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text(stringResource(R.string.import_another))
                }
            }
            is ImportState.Error -> {
                Text(
                    text = stringResource(R.string.error_prefix, current.message),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text(stringResource(R.string.import_retry))
                }
            }
        }
    }
}