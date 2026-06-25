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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.data.model.CsvType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val selectedBank by viewModel.selectedBank.collectAsState()
    var selectedCsvType by remember { mutableStateOf(CsvType.ACCOUNT) }
    var expanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver
                .openInputStream(it)
                ?.bufferedReader()
                ?.readText() ?: return@let
            viewModel.importCsv(content, selectedCsvType)
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
            text = "Importar CSV",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Combo de bancos
        if (banks.size > 1) {
            Text("Banco:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedBank?.name ?: "Selecciona un banco",
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
            // Si solo hay un banco se muestra como texto informativo
            Text(
                text = "Banco: ${banks.first().name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Selector de tipo de CSV
        Text("Tipo de fichero:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCsvType == CsvType.ACCOUNT,
                onClick = { selectedCsvType = CsvType.ACCOUNT },
                label = { Text("Cuenta corriente") }
            )
            FilterChip(
                selected = selectedCsvType == CsvType.CREDIT_CARD,
                onClick = { selectedCsvType = CsvType.CREDIT_CARD },
                label = { Text("Tarjeta crédito") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón seleccionar fichero
        Button(
            onClick = { launcher.launch("text/*") },
            enabled = state !is ImportState.Loading && selectedBank != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar fichero CSV")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Estado
        when (val current = state) {
            is ImportState.Idle -> {}
            is ImportState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Importando...")
            }
            is ImportState.Success -> {
                Text(
                    text = "✓ ${current.count} transacciones importadas",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Importar otro fichero")
                }
            }
            is ImportState.Error -> {
                Text(
                    text = "Error: ${current.message}",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Reintentar")
                }
            }
        }
    }
}