package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.TransactionView
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class TransactionViewRepository {

    suspend fun getByMonth(month: String): Result<List<TransactionView>> {
        return runCatching {
            supabase.from("transaction_view")
                .select {
                    filter { eq("month", month) }
                    order("transaction_date", Order.DESCENDING)
                }
                .decodeList<TransactionView>()
        }
    }

    suspend fun getByBank(bankId: Int): Result<List<TransactionView>> {
        return runCatching {
            supabase.from("transaction_view")
                .select {
                    filter { eq("bank_id", bankId) }
                    order("transaction_date", Order.DESCENDING)
                }
                .decodeList<TransactionView>()
        }
    }

    suspend fun getByDateRange(
        startDate: String,
        endDate: String
    ): Result<List<TransactionView>> {
        return runCatching {
            supabase.from("transaction_view")
                .select {
                    filter {
                        gte("transaction_date", startDate)
                        lte("transaction_date", endDate)
                    }
                    order("transaction_date", Order.DESCENDING)
                }
                .decodeList<TransactionView>()
        }
    }

    suspend fun getByFilters(
        month: String? = null,
        bankId: Int? = null,
        paymentType: String? = null,
        groupId: Int? = null,
        onlyUncategorized: Boolean = false,
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<TransactionView>> {
        return runCatching {
            supabase.from("transaction_view")
                .select {
                    filter {
                        month?.let { eq("month", it) }
                        bankId?.let { eq("bank_id", it) }
                        paymentType?.let { eq("payment_type", it) }
                        groupId?.let { eq("group_id", it) }
                        startDate?.let { gte("transaction_date", it) }
                        endDate?.let { lte("transaction_date", it) }
                    }
                    order("transaction_date", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<TransactionView>()
                .let { transactions ->
                    if (onlyUncategorized) {
                        transactions.filter { it.groupId == null }
                    } else {
                        transactions
                    }
                }
        }
    }

}