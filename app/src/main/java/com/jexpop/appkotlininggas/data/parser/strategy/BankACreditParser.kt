package com.jexpop.appkotlininggas.data.parser.strategy

import com.jexpop.appkotlininggas.data.model.Transaction

class BankACreditParser : CsvParserStrategy {

    override val bankName = "BankA"
    override val csvType = "CREDIT_CARD"

    override fun canParse(content: String): Boolean {
        return content.lines().any { it.startsWith("Fecha;Comercio / Entidad;Importe;Tipo") }
    }

    override fun parse(content: String, bankId: Int): List<Transaction> {
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("Fecha;Comercio / Entidad") }
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