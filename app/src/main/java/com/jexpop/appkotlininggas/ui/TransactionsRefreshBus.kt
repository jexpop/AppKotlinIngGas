package com.jexpop.appkotlininggas.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TransactionsRefreshBus {
    private val _refreshTick = MutableStateFlow(0L)
    val refreshTick: StateFlow<Long> = _refreshTick

    fun notifyTransactionsChanged() {
        _refreshTick.value = _refreshTick.value + 1L
    }
}
