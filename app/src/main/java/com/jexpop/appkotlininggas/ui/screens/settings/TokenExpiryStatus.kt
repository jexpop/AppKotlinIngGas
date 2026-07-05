package com.jexpop.appkotlininggas.ui.screens.settings

sealed class TokenExpiryStatus {
    object Ok : TokenExpiryStatus()
    object Unknown : TokenExpiryStatus()
    object Expired : TokenExpiryStatus()
    data class Warning(val daysLeft: Int) : TokenExpiryStatus()
}