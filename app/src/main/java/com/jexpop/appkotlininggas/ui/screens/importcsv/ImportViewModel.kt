package com.jexpop.appkotlininggas.ui.screens.importcsv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.data.repository.BankRepository
import com.jexpop.appkotlininggas.domain.usecase.ImportCsvUseCase
import com.jexpop.appkotlininggas.ui.TransactionsRefreshBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(
    application: Application,
    private val importCsvUseCase: ImportCsvUseCase = ImportCsvUseCase(),
    private val bankRepository: BankRepository = BankRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private val _banks = MutableStateFlow<List<Bank>>(emptyList())
    val banks: StateFlow<List<Bank>> = _banks

    private val _selectedBank = MutableStateFlow<Bank?>(null)
    val selectedBank: StateFlow<Bank?> = _selectedBank

    init {
        loadBanks()
    }

    private fun loadBanks() {
        viewModelScope.launch {
            bankRepository.getActiveBanks()
                .onSuccess { bankList ->
                    _banks.value = bankList
                    if (bankList.size == 1) {
                        _selectedBank.value = bankList.first()
                    }
                }
                .onFailure { error ->
                    _state.value = ImportState.Error(
                        context.getString(R.string.error_loading_banks, error.message)
                    )
                }
        }
    }

    fun selectBank(bank: Bank) {
        _selectedBank.value = bank
    }

    fun importCsv(content: String, context: android.content.Context) {
        val bankId = _selectedBank.value?.id ?: run {
            _state.value = ImportState.Error(
                context.getString(R.string.error_no_bank_selected)
            )
            return
        }
            viewModelScope.launch {
                _state.value = ImportState.Loading
                importCsvUseCase.execute(content, bankId, context)
                .onSuccess { count ->
                    TransactionsRefreshBus.notifyTransactionsChanged()
                    _state.value = ImportState.Success(count)
                }
                .onFailure { error ->
                    val message = when (error.message) {
                        "FORMAT_NOT_RECOGNIZED" -> context.getString(R.string.error_format_not_recognized)
                        "NO_TRANSACTIONS" -> context.getString(R.string.error_no_transactions)
                        "MULTIPLE_MONTHS" -> context.getString(R.string.error_multiple_months)
                        else -> error.message ?: context.getString(R.string.error_unknown)
                    }
                    _state.value = ImportState.Error(message)
                }
        }
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return ImportViewModel(application) as T
            }
        }
    }

}
