package com.jexpop.appkotlininggas.ui.screens.importcsv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.data.repository.BankRepository
import com.jexpop.appkotlininggas.domain.usecase.ImportCsvUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(
    private val importCsvUseCase: ImportCsvUseCase = ImportCsvUseCase(),
    private val bankRepository: BankRepository = BankRepository()
) : ViewModel() {

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
                    _state.value = ImportState.Error("Error cargando bancos: ${error.message}")
                }
        }
    }

    fun selectBank(bank: Bank) {
        _selectedBank.value = bank
    }

    fun importCsv(content: String) {
        val bankId = _selectedBank.value?.id ?: run {
            _state.value = ImportState.Error("Selecciona un banco")
            return
        }
        viewModelScope.launch {
            _state.value = ImportState.Loading
            importCsvUseCase.execute(content, bankId)
                .onSuccess { count ->
                    _state.value = ImportState.Success(count)
                }
                .onFailure { error ->
                    _state.value = ImportState.Error(error.message ?: "Error desconocido")
                }
        }
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }
}