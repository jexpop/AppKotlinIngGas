package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class AppParam(
    val id: Int? = null,
    val cond1: String,
    val cond2: String,
    val value: String? = null
)

class AppParamRepository {

    suspend fun getValue(cond1: String, cond2: String): Result<String?> {
        return runCatching {
            supabase.from("app_param")
                .select {
                    filter {
                        eq("cond1", cond1)
                        eq("cond2", cond2)
                    }
                }
                .decodeList<AppParam>()
                .firstOrNull()?.value
        }
    }

    suspend fun setValue(cond1: String, cond2: String, value: String): Result<Unit> {
        return runCatching {
            supabase.from("app_param").update({
                set("value", value)
            }) {
                filter {
                    eq("cond1", cond1)
                    eq("cond2", cond2)
                }
            }
        }
    }
}