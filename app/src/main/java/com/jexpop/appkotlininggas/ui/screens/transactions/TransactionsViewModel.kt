package com.jexpop.appkotlininggas.ui.screens.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.data.model.TransactionView
import com.jexpop.appkotlininggas.data.repository.BankRepository
import com.jexpop.appkotlininggas.data.repository.PeriodRepository
import com.jexpop.appkotlininggas.data.repository.TransactionViewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TransactionsState {
    object Idle : TransactionsState()
    object Loading : TransactionsState()
    object Success : TransactionsState()
    data class Error(val message: String) : TransactionsState()
}

class TransactionsViewModel(
    application: Application,
    private val repository: TransactionViewRepository = TransactionViewRepository(),
    private val bankRepository: BankRepository = BankRepository(),
    private val periodRepository: PeriodRepository = PeriodRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _transactions = MutableStateFlow<List<TransactionView>>(emptyList())
    val transactions: StateFlow<List<TransactionView>> = _transactions

    private val _state = MutableStateFlow<TransactionsState>(TransactionsState.Idle)
    val state: StateFlow<TransactionsState> = _state

    private val _banks = MutableStateFlow<List<Bank>>(emptyList())
    val banks: StateFlow<List<Bank>> = _banks

    private val _months = MutableStateFlow<List<String>>(emptyList())
    val months: StateFlow<List<String>> = _months

    // Filtros activos
    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth

    private val _selectedBank = MutableStateFlow<Bank?>(null)
    val selectedBank: StateFlow<Bank?> = _selectedBank

    private val _selectedPaymentType = MutableStateFlow<String?>(null)
    val selectedPaymentType: StateFlow<String?> = _selectedPaymentType

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate

    // Total real (servidor) que cumple el filtro activo, vía Count.EXACT.
    // Independiente de la paginación: no cambia al hacer scroll/loadMore.
    private val _totalCount = MutableStateFlow<Long?>(null)
    val totalCount: StateFlow<Long?> = _totalCount

    // Paginación
    private var currentOffset = 0
    private val pageSize = 50
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            bankRepository.getAllBanks()
                .onSuccess { _banks.value = it }

            // Cargar meses disponibles
            loadMonths()

            // Cargar mes actual por defecto
            val currentMonth = getCurrentMonth()
            _selectedMonth.value = currentMonth
            loadTransactions(reset = true)
        }
    }

    private suspend fun getCurrentMonth(): String? {
        return periodRepository.getCurrentMonth().getOrNull()
    }

    /**
     * Recarga la lista de meses disponibles para el combo de filtros.
     * Pública para poder invocarla tras operaciones que puedan crear un mes
     * nuevo en la base de datos (p. ej. una importación de CSV), sin depender
     * de reiniciar la app (que era lo único que antes disparaba init{}).
     */
    suspend fun loadMonths() {
        periodRepository.getAllMonths()
            .onSuccess { _months.value = it }
    }

    /**
     * Refresco a llamar tras una importación correcta. A diferencia de
     * loadTransactions(reset = true) por sí solo, también recarga el combo
     * de meses por si la importación ha creado un mes nuevo en la BD que
     * antes no existía, y actualiza el mes seleccionado al nuevo mes
     * "current" (ImportCsvUseCase llama a periodRepository.setCurrentMonth()
     * durante la importación, pero antes solo se leía una vez en el init{}).
     */
    fun refreshAfterImport() {
        viewModelScope.launch {
            loadMonths()
            _selectedMonth.value = getCurrentMonth()
            loadTransactions(reset = true)
        }
    }

    fun loadTransactions(reset: Boolean = false) {
        if (reset) {
            currentOffset = 0
            _hasMore.value = true
            _transactions.value = emptyList()
            _totalCount.value = null
        }

        if (!_hasMore.value) return

        viewModelScope.launch {
            _state.value = TransactionsState.Loading
            repository.getByFilters(
                month = _selectedMonth.value,
                bankId = _selectedBank.value?.id,
                paymentType = _selectedPaymentType.value,
                onlyUncategorized = _selectedCategoryFilter.value == "UNCATEGORIZED",
                startDate = _startDate.value,
                endDate = _endDate.value,
                limit = pageSize,
                offset = currentOffset
            ).onSuccess { page ->
                if (reset) {
                    _transactions.value = page.items
                } else {
                    _transactions.value = _transactions.value + page.items
                }
                _totalCount.value = page.totalCount
                _hasMore.value = page.items.size == pageSize
                currentOffset += page.items.size
                _state.value = TransactionsState.Success
            }.onFailure {
                _state.value = TransactionsState.Error(
                    it.message ?: context.getString(R.string.error_unknown)
                )
            }
        }
    }

    fun selectMonth(month: String?) {
        _selectedMonth.value = month
        _startDate.value = null
        _endDate.value = null
        loadTransactions(reset = true)
    }

    fun selectBank(bank: Bank?) {
        _selectedBank.value = bank
        loadTransactions(reset = true)
    }

    fun selectPaymentType(type: String?) {
        _selectedPaymentType.value = type
        loadTransactions(reset = true)
    }

    fun selectCategoryFilter(filter: String?) {
        _selectedCategoryFilter.value = filter
        loadTransactions(reset = true)
    }

    fun setDateRange(start: String?, end: String?) {
        _startDate.value = start
        _endDate.value = end
        _selectedMonth.value = null
        loadTransactions(reset = true)
    }

    fun loadMore() {
        loadTransactions(reset = false)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return TransactionsViewModel(application) as T
            }
        }
    }
}