package com.jexpop.appkotlininggas.ui.screens.banks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.data.repository.BankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BanksState {
    object Idle : BanksState()
    object Loading : BanksState()
    object Success : BanksState()
    data class Error(val message: String) : BanksState()
}

class BanksViewModel(
    application: Application,
    private val bankRepository: BankRepository = BankRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _banks = MutableStateFlow<List<Bank>>(emptyList())
    val banks: StateFlow<List<Bank>> = _banks

    private val _state = MutableStateFlow<BanksState>(BanksState.Idle)
    val state: StateFlow<BanksState> = _state

    init {
        loadBanks()
    }

    private fun loadBanks() {
        viewModelScope.launch {
            _state.value = BanksState.Loading
            bankRepository.getAllBanks()
                .onSuccess {
                    _banks.value = it
                    _state.value = BanksState.Idle
                }
                .onFailure {
                    _state.value = BanksState.Error(
                        it.message ?: context.getString(R.string.error_unknown)
                    )
                }
        }
    }

    fun toggleActive(bank: Bank) {
        viewModelScope.launch {
            bankRepository.updateBank(bank.copy(active = !bank.active))
                .onSuccess { loadBanks() }
                .onFailure {
                    _state.value = BanksState.Error(
                        it.message ?: context.getString(R.string.error_unknown)
                    )
                }
        }
    }

    fun addBank(name: String, description: String) {
        viewModelScope.launch {
            bankRepository.insertBank(Bank(name = name, description = description))
                .onSuccess { loadBanks() }
                .onFailure {
                    _state.value = BanksState.Error(
                        it.message ?: context.getString(R.string.error_unknown)
                    )
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return BanksViewModel(application) as T
            }
        }
    }
}