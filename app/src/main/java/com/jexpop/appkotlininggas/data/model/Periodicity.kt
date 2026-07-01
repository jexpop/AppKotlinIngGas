package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Periodicity(
    val id: String,
    val description: String
)