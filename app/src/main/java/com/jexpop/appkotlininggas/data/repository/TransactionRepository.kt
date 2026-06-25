package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from

class TransactionRepository {

    suspend fun insertTransactions(transactions: List<Transaction>): Result<Unit> {
        return runCatching {
            supabase.from("transaction").insert(transactions)
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