package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PeriodMonth(
    val id: Int? = null,
    val month: String,
    val active: Boolean = false,
    val current: Boolean = false
)