package com.jexpop.appkotlininggas.data.parser.strategy

import com.jexpop.appkotlininggas.data.model.Transaction

class BankAAccountParser : CsvParserStrategy {

    override val bankName = "BankA"
    override val csvType = "ACCOUNT"

    override fun canParse(content: String): Boolean {
        return content.lines().any { it.startsWith("date;value_date;concept") }
    }

    override fun parse(content: String, bankId: Int): List<Transaction> {
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("date;") }
        if (headerIndex == -1) return emptyList()

        val headers = lines[headerIndex].split(";").map { it.trim().lowercase() }
        val dateIndex = headers.indexOf("date")
        val conceptIndex = headers.indexOf("concept")
        val amountIndex = headers.indexOf("amount")
        val balanceIndex = headers.indexOf("balance")

        val transactions = lines.drop(headerIndex + 1)
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

        // Validar que todas las transacciones son del mismo mes
        val months = transactions.map { it.transactionDate.substring(0, 7) }.distinct()
        if (months.size > 1) {
            throw Exception("MULTIPLE_MONTHS")
        }

        return transactions
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