package com.jexpop.appkotlininggas.ui.components

object DateFormatter {

    private val monthNames = mapOf(
        "01" to "Enero", "02" to "Febrero", "03" to "Marzo",
        "04" to "Abril", "05" to "Mayo", "06" to "Junio",
        "07" to "Julio", "08" to "Agosto", "09" to "Septiembre",
        "10" to "Octubre", "11" to "Noviembre", "12" to "Diciembre"
    )

    // "2026-01-31" → "31 Ene 2026"
    fun formatDate(date: String): String {
        return try {
            val parts = date.split("-")
            val day = parts[2]
            val month = parts[1]
            val year = parts[0]
            val monthAbbr = monthNames[month]?.take(3) ?: month
            "$day $monthAbbr $year"
        } catch (e: Exception) {
            date
        }
    }

    // "202601" → "Enero 2026"
    fun formatMonth(month: String): String {
        return try {
            val year = month.substring(0, 4)
            val m = month.substring(4, 6)
            "${monthNames[m]} $year"
        } catch (e: Exception) {
            month
        }
    }
}