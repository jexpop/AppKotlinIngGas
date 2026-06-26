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
    val value1: String? = null,
    val value2: String? = null
)

@Serializable
data class CategorizationRule(
    val id: Int? = null,
    val rule_type: Int,
    val group_id: Int,
    val value1: String? = null,
    val value2: String? = null,
    val value3: String? = null,
    val value4: String? = null
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