package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Bank
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from

class BankRepository {

    suspend fun getActiveBanks(): Result<List<Bank>> {
        return runCatching {
            supabase.from("bank")
                .select {
                    filter {
                        eq("active", true)
                    }
                }
                .decodeList<Bank>()
        }
    }
}