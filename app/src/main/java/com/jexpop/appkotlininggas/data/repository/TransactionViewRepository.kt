package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.TransactionView
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order

/**
 * Página de resultados de transacciones, incluyendo el total real que cumple
 * el filtro en base de datos (independiente de cuántos se hayan cargado/paginado).
 */
data class TransactionsPage(
    val items: List<TransactionView>,
    val totalCount: Long?
)

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

    /**
     * Devuelve la página de transacciones solicitada junto con el total real
     * (totalCount) que cumple el filtro en base de datos, vía Count.EXACT.
     * totalCount no depende de la paginación: es el mismo valor para cualquier
     * offset/limit dado un mismo conjunto de filtros.
     */
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
    ): Result<TransactionsPage> {
        return runCatching {
            val result = supabase.from("transaction_view")
                .select {
                    count(Count.EXACT)
                    filter {
                        month?.let { eq("month", it) }
                        bankId?.let { eq("bank_id", it) }
                        paymentType?.let { eq("payment_type", it) }
                        groupId?.let { eq("group_id", it) }
                        startDate?.let { gte("transaction_date", it) }
                        endDate?.let { lte("transaction_date", it) }
                        // Filtro aplicado en SERVIDOR (antes del range()) para no perder
                        // resultados fuera de la página actual. "exact" con valor null
                        // genera group_id=is.null en la query PostgREST.
                        if (onlyUncategorized) {
                            exact("group_id", null)
                        }
                    }
                    order("transaction_date", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }

            TransactionsPage(
                items = result.decodeList<TransactionView>(),
                totalCount = result.countOrNull()
            )
        }
    }

}