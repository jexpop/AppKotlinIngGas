package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class CategorizationException(
    val id: Int? = null,
    val month: String,
    val group_id: Int,
    val value1: String? = null
)

@Serializable
data class CategorizationRule(
    val id: Int? = null,
    val rule_type: Int,
    val group_id: Int,
    val value1: String? = null,
    val value2: String? = null,
    val value3: String? = null,
    val value4: String? = null,
    // Rango de posiciones (numeración de usuario, 1-based, inclusive) usado por los
    // tipos de regla 4 y 7. NULL = usa el rango por defecto 18-30 en CategorizationUseCase.
    val range_start: Int? = null,
    val range_end: Int? = null,
    // Signo esperado del importe para los tipos 5, 6 y 7 (que comparan contra value2/value3/value4).
    // false (por defecto) = gasto (amount < 0). true = ingreso (amount > 0).
    // El límite/rango se compara siempre contra el valor absoluto del importe.
    val is_income: Boolean = false
)

class CategorizationRepository {

    suspend fun getExceptions(month: String): Result<List<CategorizationException>> {
        return runCatching {
            supabase.from("categorization_exception")
                .select {
                    filter { eq("month", month) }
                }
                .decodeList<CategorizationException>()
        }
    }

    suspend fun getRules(): Result<List<CategorizationRule>> {
        return runCatching {
            supabase.from("categorization_rule")
                .select()
                .decodeList<CategorizationRule>()
        }
    }
}