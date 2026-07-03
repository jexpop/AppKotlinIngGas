package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bank(
    val id: Int? = null,
    val name: String,
    val code: String? = null,
    val description: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)