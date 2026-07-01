package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RuleType(
    val id: Int,
    val description: String
)