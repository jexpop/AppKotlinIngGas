package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryGroup(
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Int? = null,
    @SerialName("parent_id") val parentId: Int? = null,
    val description: String,
    val periodicity: String? = null,
    @SerialName("sort_order") val sortOrder: String? = null,
    @SerialName("flow_type") val flowType: String? = null
)

@Serializable
data class CategoryGroupInsert(
    @SerialName("parent_id") val parentId: Int? = null,
    val description: String,
    val periodicity: String? = null,
    @SerialName("sort_order") val sortOrder: String? = null,
    @SerialName("flow_type") val flowType: String? = null
)