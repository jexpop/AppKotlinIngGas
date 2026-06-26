package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Period(
    val year: String,
    val balanced: Boolean = false
)