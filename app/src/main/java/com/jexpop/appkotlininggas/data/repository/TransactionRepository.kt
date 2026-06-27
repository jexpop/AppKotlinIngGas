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
            val year = yearMonth.substring(0, 4)
            val month = yearMonth.substring(4, 6)
            val startDate = "$year-$month-01"
            val endDate = "$year-$month-31"
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

    suspend fun deleteByCreditMonthAndBank(
        creditMonth: String,
        bankId: Int
    ): Result<Unit> {
        return runCatching {
            supabase.from("transaction").delete {
                filter {
                    eq("credit_month", creditMonth)
                    eq("bank_id", bankId)
                    eq("payment_type", "C")
                }
            }
        }
    }

    suspend fun getTransactionsByMonth(month: String): Result<List<Transaction>> {
        return runCatching {
            val year = month.substring(0, 4)
            val m = month.substring(4, 6)
            val startDate = "$year-$m-01"
            val endDate = "$year-$m-31"
            supabase.from("transaction")
                .select {
                    filter {
                        gte("transaction_date", startDate)
                        lte("transaction_date", endDate)
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