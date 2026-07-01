package com.jexpop.appkotlininggas.ui.screens.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.CategoryGroup
import com.jexpop.appkotlininggas.data.repository.CategorizationException
import com.jexpop.appkotlininggas.data.repository.CategorizationRule
import com.jexpop.appkotlininggas.data.repository.CategoryRepository
import com.jexpop.appkotlininggas.data.repository.RulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.jexpop.appkotlininggas.data.model.Periodicity
import com.jexpop.appkotlininggas.data.repository.PeriodicityRepository
import com.jexpop.appkotlininggas.data.model.RuleType

sealed class CategoriesState {
    object Idle : CategoriesState()
    object Loading : CategoriesState()
    object Success : CategoriesState()
    data class Error(val message: String) : CategoriesState()
}

class CategoriesViewModel(
    application: Application,
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val rulesRepository: RulesRepository = RulesRepository()
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _state = MutableStateFlow<CategoriesState>(CategoriesState.Idle)
    val state: StateFlow<CategoriesState> = _state

    private val _groups = MutableStateFlow<List<CategoryGroup>>(emptyList())
    val groups: StateFlow<List<CategoryGroup>> = _groups

    private val _rules = MutableStateFlow<List<CategorizationRule>>(emptyList())
    val rules: StateFlow<List<CategorizationRule>> = _rules

    private val _exceptions = MutableStateFlow<List<CategorizationException>>(emptyList())
    val exceptions: StateFlow<List<CategorizationException>> = _exceptions

    private val periodicityRepository: PeriodicityRepository = PeriodicityRepository()

    private val _periodicities = MutableStateFlow<List<Periodicity>>(emptyList())
    val periodicities: StateFlow<List<Periodicity>> = _periodicities

    private val _ruleTypes = MutableStateFlow<List<RuleType>>(emptyList())
    val ruleTypes: StateFlow<List<RuleType>> = _ruleTypes

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.value = CategoriesState.Loading
            categoryRepository.getAllGroups()
                .onSuccess { _groups.value = it }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
            rulesRepository.getAllRules()
                .onSuccess { _rules.value = it }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
            rulesRepository.getAllExceptions()
                .onSuccess { _exceptions.value = it }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
            periodicityRepository.getAll()
                .onSuccess { _periodicities.value = it }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
            rulesRepository.getAllRuleTypes()
                .onSuccess { _ruleTypes.value = it }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
            _state.value = CategoriesState.Idle
        }
    }

    // Grupos
    fun addGroup(group: CategoryGroup) {
        viewModelScope.launch {
            android.util.Log.d("ADD_GROUP", "Intentando insertar: $group")
            categoryRepository.insertGroup(group)
                .onSuccess {
                    android.util.Log.d("ADD_GROUP", "Insertado correctamente")
                    val allGroups = categoryRepository.getAllGroups().getOrNull() ?: emptyList()
                    categoryRepository.recalculateSortOrder(allGroups).getOrNull()
                    loadAll()
                }
                .onFailure {
                    android.util.Log.e("ADD_GROUP", "Error: ${it.message}")
                    _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown))
                }
        }
    }

    fun updateGroup(group: CategoryGroup) {
        viewModelScope.launch {
            categoryRepository.updateGroup(group)
                .onSuccess {
                    val allGroups = categoryRepository.getAllGroups().getOrNull() ?: emptyList()
                    categoryRepository.recalculateSortOrder(allGroups).getOrNull()
                    loadAll()
                }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    fun deleteGroup(id: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            categoryRepository.hasTransactions(id)
                .onSuccess { hasTransactions ->
                    if (hasTransactions) {
                        onError(context.getString(R.string.categories_delete_error))
                    } else {
                        categoryRepository.deleteGroup(id)
                            .onSuccess { loadAll() }
                            .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
                    }
                }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    // Reglas automáticas
    fun addRule(rule: CategorizationRule) {
        viewModelScope.launch {
            rulesRepository.insertRule(rule)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    fun updateRule(rule: CategorizationRule) {
        viewModelScope.launch {
            rulesRepository.updateRule(rule)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            rulesRepository.deleteRule(id)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    // Excepciones manuales
    fun addException(exception: CategorizationException) {
        viewModelScope.launch {
            rulesRepository.insertException(exception)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    fun updateException(exception: CategorizationException) {
        viewModelScope.launch {
            rulesRepository.updateException(exception)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    fun deleteException(id: Int) {
        viewModelScope.launch {
            rulesRepository.deleteException(id)
                .onSuccess { loadAll() }
                .onFailure { _state.value = CategoriesState.Error(it.message ?: context.getString(R.string.error_unknown)) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return CategoriesViewModel(application) as T
            }
        }
    }
}