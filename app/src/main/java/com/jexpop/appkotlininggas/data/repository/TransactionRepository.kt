package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

class TransactionRepository {

    suspend fun insertTransactions(transactions: List<Transaction>): Result<Unit> {
        return runCatching {
            supabase.from("transaction").insert(transactions)
        }
    }

    suspend fun deleteByMonthBankAndType(
        yearMonth: String,
        bankId: Int,
        paymentType: String
    ): Result<Unit> {
        return runCatching {
            val startDate = "$yearMonth-01"
            val endDate = "$yearMonth-31"
            supabase.from("transaction").delete {
                filter {
                    gte("transaction_date", startDate)
                    lte("transaction_date", endDate)
                    eq("bank_id", bankId)
                    eq("payment_type", paymentType)
                }
            }
        }
    }

    suspend fun getTransactionsByMonth(month: String): Result<List<Transaction>> {
        return runCatching {
            supabase.from("transaction")
                .select {
                    filter {
                        like("transaction_date", "$month%")
                    }
                }
                .decodeList<Transaction>()
        }
    }

    suspend fun getAllTransactions(): Result<List<Transaction>> {
        return runCatching {
            supabase.from("transaction")
                .select()
                .decodeList<Transaction>()
        }
    }
}