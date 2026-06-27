package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from

class BankRepository {

    suspend fun getActiveBanks(): Result<List<Bank>> {
        return runCatching {
            supabase.from("bank")
                .select {
                    filter { eq("active", true) }
                }
                .decodeList<Bank>()
        }
    }

    suspend fun getAllBanks(): Result<List<Bank>> {
        return runCatching {
            supabase.from("bank")
                .select()
                .decodeList<Bank>()
        }
    }

    suspend fun insertBank(bank: Bank): Result<Unit> {
        return runCatching {
            supabase.from("bank").insert(bank)
        }
    }

    suspend fun updateBank(bank: Bank): Result<Unit> {
        return runCatching {
            supabase.from("bank").update({
                set("active", bank.active)
                set("name", bank.name)
                bank.description?.let { set("description", it) }
            }) {
                filter { eq("id", bank.id!!) }
            }
        }
    }
}