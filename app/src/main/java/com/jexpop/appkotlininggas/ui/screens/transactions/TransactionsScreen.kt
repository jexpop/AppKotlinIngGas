package com.jexpop.appkotlininggas.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.TransactionView
import com.jexpop.appkotlininggas.ui.components.DateFormatter
import androidx.compose.foundation.clickable

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
    var expandedTransactionKey by remember { mutableStateOf<String?>(null) }
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

    LaunchedEffect(transactions) {
        if (expandedTransactionKey != null && transactions.none { it.expansionKey() == expandedTransactionKey }) {
            expandedTransactionKey = null
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
                    items(
                        items = transactions,
                        key = { it.expansionKey() }
                    ) { transaction ->
                        val transactionKey = transaction.expansionKey()
                        TransactionItem(
                            transaction = transaction,
                            expanded = transactionKey == expandedTransactionKey,
                            onClick = {
                                expandedTransactionKey = if (transactionKey == expandedTransactionKey) {
                                    null
                                } else {
                                    transactionKey
                                }
                            }
                        )
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
    var showMonthPicker by remember { mutableStateOf(false) }
    var showBankPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filtro por mes - usando OutlinedButton + AlertDialog
        Text(
            text = stringResource(R.string.transactions_month),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { showMonthPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedMonth?.let { DateFormatter.formatMonth(it) }
                    ?: stringResource(R.string.transactions_all_months),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Filtro por banco (solo si hay más de uno) - usando OutlinedButton + AlertDialog
        if (banks.size > 1) {
            Text(
                text = stringResource(R.string.nav_banks),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showBankPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedBank?.name ?: stringResource(R.string.transactions_all_banks),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

        // Dialog selector de mes
        if (showMonthPicker) {
            AlertDialog(
                onDismissRequest = { showMonthPicker = false },
                title = { Text(stringResource(R.string.transactions_select_month)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onMonthSelected(null)
                                showMonthPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.transactions_all_months))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        months.forEach { monthItem ->
                            TextButton(
                                onClick = {
                                    onMonthSelected(monthItem)
                                    showMonthPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(DateFormatter.formatMonth(monthItem))
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Dialog selector de banco
        if (showBankPicker) {
            AlertDialog(
                onDismissRequest = { showBankPicker = false },
                title = { Text(stringResource(R.string.transactions_select_bank)) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onBankSelected(null)
                                showBankPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.transactions_all_banks))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        banks.forEach { bank ->
                            TextButton(
                                onClick = {
                                    onBankSelected(bank)
                                    showBankPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(bank.name)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionView,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val isIncome = transaction.flowType == "H"
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountText = if (isIncome) "+%.2f€".format(transaction.amount)
    else "-%.2f€".format(transaction.amount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.concept,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
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
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider()
                    TransactionDetailLine(
                        label = stringResource(R.string.transactions_detail_bank),
                        value = transaction.bankName ?: "-"
                    )
                    TransactionDetailLine(
                        label = stringResource(R.string.transactions_detail_category),
                        value = transaction.groupDescription
                            ?: stringResource(R.string.transactions_detail_uncategorized)
                    )
                    transaction.balance?.let {
                        TransactionDetailLine(
                            label = stringResource(R.string.transactions_detail_balance),
                            value = "%.2f€".format(it)
                        )
                    }
                    transaction.creditMonth?.let {
                        TransactionDetailLine(
                            label = stringResource(R.string.transactions_detail_credit_month),
                            value = DateFormatter.formatMonth(it)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun TransactionView.expansionKey(): String {
    return id?.toString() ?: "$transactionDate-$concept-$amount-$paymentType"
}
