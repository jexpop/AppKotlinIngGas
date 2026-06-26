package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Period
import com.jexpop.appkotlininggas.data.model.PeriodMonth
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from

class PeriodRepository {

    suspend fun getOrCreateYear(year: String): Result<Unit> {
        return runCatching {
            android.util.Log.d("PERIOD", "Buscando año: $year")
            val existing = supabase.from("period")
                .select {
                    filter { eq("year", year) }
                }
                .decodeList<Period>()

            android.util.Log.d("PERIOD", "Años encontrados: ${existing.size}")
            if (existing.isEmpty()) {
                android.util.Log.d("PERIOD", "Insertando año: $year")
                supabase.from("period").insert(Period(year = year))
                android.util.Log.d("PERIOD", "Año insertado")
            }
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
}