package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.repository.CategorizationException
import com.jexpop.appkotlininggas.data.repository.CategorizationRule
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class RulesRepository {

    suspend fun getAllRules(): Result<List<CategorizationRule>> {
        return runCatching {
            supabase.from("categorization_rule")
                .select {
                    order("rule_type", Order.ASCENDING)
                }
                .decodeList<CategorizationRule>()
        }
    }

    suspend fun insertRule(rule: CategorizationRule): Result<Unit> {
        return runCatching {
            supabase.from("categorization_rule").insert(rule)
        }
    }

    suspend fun updateRule(rule: CategorizationRule): Result<Unit> {
        return runCatching {
            supabase.from("categorization_rule").update({
                set("rule_type", rule.rule_type)
                set("group_id", rule.group_id)
                set("value1", rule.value1)
                rule.value2?.let { set("value2", it) }
                rule.value3?.let { set("value3", it) }
                rule.value4?.let { set("value4", it) }
            }) {
                filter { eq("id", rule.id!!) }
            }
        }
    }

    suspend fun deleteRule(id: Int): Result<Unit> {
        return runCatching {
            supabase.from("categorization_rule").delete {
                filter { eq("id", id) }
            }
        }
    }

    suspend fun getAllExceptions(): Result<List<CategorizationException>> {
        return runCatching {
            supabase.from("categorization_exception")
                .select {
                    order("month", Order.DESCENDING)
                }
                .decodeList<CategorizationException>()
        }
    }

    suspend fun insertException(exception: CategorizationException): Result<Unit> {
        return runCatching {
            supabase.from("categorization_exception").insert(exception)
        }
    }

    suspend fun updateException(exception: CategorizationException): Result<Unit> {
        return runCatching {
            supabase.from("categorization_exception").update({
                set("month", exception.month)
                set("group_id", exception.group_id)
                set("value1", exception.value1)
                exception.value2?.let { set("value2", it) }
            }) {
                filter { eq("id", exception.id!!) }
            }
        }
    }

    suspend fun deleteException(id: Int): Result<Unit> {
        return runCatching {
            supabase.from("categorization_exception").delete {
                filter { eq("id", id) }
            }
        }
    }
}