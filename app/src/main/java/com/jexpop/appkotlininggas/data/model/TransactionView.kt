package com.jexpop.appkotlininggas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionView(
    val id: Long? = null,
    @SerialName("transaction_date") val transactionDate: String,
    val concept: String,
    val amount: Double,
    @SerialName("flow_type") val flowType: String,
    @SerialName("payment_type") val paymentType: String,
    val balance: Double? = null,
    @SerialName("credit_month") val creditMonth: String? = null,
    @SerialName("bank_id") val bankId: Int? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_description") val groupDescription: String? = null,
    @SerialName("group_flow_type") val groupFlowType: String? = null,
    @SerialName("period_month_id") val periodMonthId: Int? = null,
    val month: String? = null
)