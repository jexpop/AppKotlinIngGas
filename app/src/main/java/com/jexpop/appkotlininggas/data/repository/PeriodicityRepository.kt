package com.jexpop.appkotlininggas.data.repository

import com.jexpop.appkotlininggas.data.model.Periodicity
import com.jexpop.appkotlininggas.supabase
import io.github.jan.supabase.postgrest.from

class PeriodicityRepository {

    suspend fun getAll(): Result<List<Periodicity>> {
        return runCatching {
            supabase.from("periodicity")
                .select()
                .decodeList<Periodicity>()
        }
    }
}