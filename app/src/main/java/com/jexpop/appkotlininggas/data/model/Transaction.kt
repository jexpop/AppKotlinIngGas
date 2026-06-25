package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Long? = null,
    @SerialName("transaction_date") val transactionDate: String,
    val concept: String,
    @SerialName("flow_type") val flowType: String,
    val amount: Double,
    val balance: Double? = null,
    @SerialName("payment_type") val paymentType: String,
    @SerialName("credit_month") val creditMonth: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("period_month_id") val periodMonthId: Int? = null,
    @SerialName("sequence_number") val sequenceNumber: Int? = null,
    @SerialName("bank_id") val bankId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)