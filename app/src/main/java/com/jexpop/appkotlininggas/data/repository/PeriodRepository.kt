package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Period
import com.jexpop.appkotlininggas.data.model.PeriodMonth
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class PeriodRepository {

    suspend fun getOrCreateYear(year: String): Result<Unit> {
        return runCatching {
            android.util.Log.d("PERIOD", "Upsert año: $year")
            supabase.from("period")
                .upsert(Period(year = year)) {
                    onConflict = "year"
                    ignoreDuplicates = true
                }
            android.util.Log.d("PERIOD", "Upsert año completado")
        }
    }

    suspend fun getOrCreateMonth(yearMonth: String): Result<PeriodMonth> {
        return runCatching {
            android.util.Log.d("PERIOD", "Intentando upsert para $yearMonth")
            supabase.from("period_month")
                .upsert(PeriodMonth(month = yearMonth, active = true, current = false)) {
                    onConflict = "month"
                    ignoreDuplicates = true
                }

            android.util.Log.d("PERIOD", "Upsert completado, obteniendo mes")
            supabase.from("period_month")
                .select {
                    filter { eq("month", yearMonth) }
                }
                .decodeSingle<PeriodMonth>()
        }
    }

    suspend fun setCurrentMonth(monthId: Int): Result<Unit> {
        return runCatching {
            android.util.Log.d("PERIOD", "Desmarcando current")
            supabase.from("period_month")
                .update({ set("current", false) }) {
                    filter { eq("current", true) }
                }
            android.util.Log.d("PERIOD", "Marcando current: $monthId")
            supabase.from("period_month")
                .update({ set("current", true) }) {
                    filter { eq("id", monthId) }
                }
            android.util.Log.d("PERIOD", "setCurrentMonth completado")
        }
    }

    suspend fun getCurrentMonth(): Result<String?> {
        return runCatching {
            supabase.from("period_month")
                .select {
                    filter { eq("current", true) }
                }
                .decodeList<PeriodMonth>()
                .firstOrNull()?.month
        }
    }

    suspend fun getAllMonths(): Result<List<String>> {
        return runCatching {
            supabase.from("period_month")
                .select {
                    filter { eq("active", true) }
                    order("month", Order.DESCENDING)
                }
                .decodeList<PeriodMonth>()
                .map { it.month }
        }
    }

}