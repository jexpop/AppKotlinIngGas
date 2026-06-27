package com.jexpop.appkotlininggas.ui.screens.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.TransactionView
import com.jexpop.appkotlininggas.ui.components.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory)
) {
    val transactions by viewModel.transactions.collectAsState()
    val state by viewModel.state.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val months by viewModel.months.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedBank by viewModel.selectedBank.collectAsState()
    val selectedPaymentType by viewModel.selectedPaymentType.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Cargar más al llegar al final
    val reachedEnd = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= transactions.size - 5
        }
    }

    LaunchedEffect(reachedEnd.value) {
        if (reachedEnd.value && hasMore && state !is TransactionsState.Loading) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_transactions)) },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.transactions_filters))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding)) {

            // Panel de filtros
            if (showFilters) {
                FiltersPanel(
                    months = months,
                    banks = banks,
                    selectedMonth = selectedMonth,
                    selectedBank = selectedBank,
                    selectedPaymentType = selectedPaymentType,
                    onMonthSelected = { viewModel.selectMonth(it) },
                    onBankSelected = { viewModel.selectBank(it) },
                    onPaymentTypeSelected = { viewModel.selectPaymentType(it) }
                )
                HorizontalDivider()
            }

            // Resumen del filtro activo
            if (selectedMonth != null || selectedBank != null || selectedPaymentType != null) {
                TransactionsSummary(transactions = transactions)
                HorizontalDivider()
            }

            // Lista de transacciones
            if (transactions.isEmpty() && state !is TransactionsState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.transactions_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionItem(transaction = transaction)
                    }
                    if (state is TransactionsState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsSummary(transactions: List<TransactionView>) {
    val totalIncome = transactions.filter { it.flowType == "H" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.flowType == "D" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.transactions_income),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+%.2f€".format(totalIncome),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.transactions_expense),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "-%.2f€".format(totalExpense),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.transactions_balance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.2f€".format(balance),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersPanel(
    months: List<String>,
    banks: List<com.jexpop.appkotlininggas.data.model.Bank>,
    selectedMonth: String?,
    selectedBank: com.jexpop.appkotlininggas.data.model.Bank?,
    selectedPaymentType: String?,
    onMonthSelected: (String?) -> Unit,
    onBankSelected: (com.jexpop.appkotlininggas.data.model.Bank?) -> Unit,
    onPaymentTypeSelected: (String?) -> Unit
) {
    var monthExpanded by remember { mutableStateOf(false) }
    var bankExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filtro por mes
        ExposedDropdownMenuBox(
            expanded = monthExpanded,
            onExpandedChange = { monthExpanded = !monthExpanded }
        ) {
            OutlinedTextField(
                value = selectedMonth?.let { DateFormatter.formatMonth(it) }
                    ?: stringResource(R.string.transactions_all_months),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.transactions_month)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.transactions_all_months)) },
                    onClick = {
                        onMonthSelected(null)
                        monthExpanded = false
                    }
                )
                months.forEach { monthItem ->
                    DropdownMenuItem(
                        text = { Text(DateFormatter.formatMonth(monthItem)) },
                        onClick = {
                            onMonthSelected(monthItem)
                            monthExpanded = false
                        }
                    )
                }
            }
        }

        // Filtro por banco (solo si hay más de uno)
        if (banks.size > 1) {
            ExposedDropdownMenuBox(
                expanded = bankExpanded,
                onExpandedChange = { bankExpanded = !bankExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBank?.name ?: stringResource(R.string.transactions_all_banks),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.nav_banks)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = bankExpanded,
                    onDismissRequest = { bankExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.transactions_all_banks)) },
                        onClick = {
                            onBankSelected(null)
                            bankExpanded = false
                        }
                    )
                    banks.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.name) },
                            onClick = {
                                onBankSelected(bank)
                                bankExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Filtro por tipo de pago
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedPaymentType == null,
                onClick = { onPaymentTypeSelected(null) },
                label = { Text(stringResource(R.string.transactions_all)) }
            )
            FilterChip(
                selected = selectedPaymentType == "D",
                onClick = { onPaymentTypeSelected("D") },
                label = { Text(stringResource(R.string.transactions_account)) }
            )
            FilterChip(
                selected = selectedPaymentType == "C",
                onClick = { onPaymentTypeSelected("C") },
                label = { Text(stringResource(R.string.transactions_credit)) }
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionView) {
    val isIncome = transaction.flowType == "H"
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountText = if (isIncome) "+%.2f€".format(transaction.amount)
    else "-%.2f€".format(transaction.amount)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.concept,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = DateFormatter.formatDate(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (transaction.paymentType == "C") "💳" else "🏦",
                        style = MaterialTheme.typography.bodySmall
                    )
                    transaction.groupDescription?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}