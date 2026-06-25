package com.jexpop.appkotlininggas.data.parser

import com.jexpop.appkotlininggas.data.model.CsvType
import com.jexpop.appkotlininggas.data.model.Transaction

object CsvParser {

    fun parse(content: String, csvType: CsvType, bankId: Int): List<Transaction> {
        return when (csvType) {
            CsvType.ACCOUNT -> parseAccount(content, bankId)
            CsvType.CREDIT_CARD -> parseCreditCard(content, bankId)
        }
    }

    private fun parseAccount(content: String, bankId: Int): List<Transaction> {
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("date;") }
        if (headerIndex == -1) return emptyList()

        val headers = lines[headerIndex].split(";")
            .map { it.trim().lowercase() }

        val dateIndex = headers.indexOf("date")
        val conceptIndex = headers.indexOf("concept")
        val amountIndex = headers.indexOf("amount")
        val balanceIndex = headers.indexOf("balance")

        return lines.drop(headerIndex + 1)
            .filter { it.isNotBlank() && !it.all { c -> c == ';' } }
            .mapNotNull { line ->
                val cols = line.split(";")
                runCatching {
                    val amount = parseAmount(cols[amountIndex])
                    Transaction(
                        transactionDate = parseDate(cols[dateIndex].trim()),
                        concept = cols[conceptIndex].trim(),
                        flowType = if (amount >= 0) "H" else "D",
                        amount = Math.abs(amount),
                        balance = parseAmount(cols[balanceIndex]),
                        paymentType = "D",
                        bankId = bankId
                    )
                }.getOrNull()
            }
    }

    private fun parseCreditCard(content: String, bankId: Int): List<Transaction> {
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("Fecha;") }
        if (headerIndex == -1) return emptyList()

        return lines.drop(headerIndex + 1)
            .filter { it.isNotBlank() && !it.all { c -> c == ';' } }
            .mapNotNull { line ->
                val cols = line.split(";")
                if (cols.size < 3) return@mapNotNull null
                runCatching {
                    val amount = parseAmount(cols[2])
                    Transaction(
                        transactionDate = parseDate(cols[0].trim()),
                        concept = cols[1].trim(),
                        flowType = if (amount >= 0) "H" else "D",
                        amount = Math.abs(amount),
                        balance = null,
                        paymentType = "C",
                        bankId = bankId
                    )
                }.getOrNull()
            }
    }

    private fun parseAmount(value: String): Double {
        return value.trim()
            .replace(".", "")
            .replace(",", ".")
            .toDouble()
    }

    private fun parseDate(value: String): String {
        val parts = value.split("/")
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }
}