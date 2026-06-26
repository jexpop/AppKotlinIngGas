package com.jexpop.appkotlininggas.data.parser.strategy

import com.jexpop.appkotlininggas.data.model.Transaction

class BankACreditParser : CsvParserStrategy {

    override val bankName = "BankA"
    override val csvType = "CREDIT_CARD"

    private val monthMap = mapOf(
        "ENERO" to "01", "FEBRERO" to "02", "MARZO" to "03",
        "ABRIL" to "04", "MAYO" to "05", "JUNIO" to "06",
        "JULIO" to "07", "AGOSTO" to "08", "SEPTIEMBRE" to "09",
        "OCTUBRE" to "10", "NOVIEMBRE" to "11", "DICIEMBRE" to "12"
    )

    override fun canParse(content: String): Boolean {
        return content.lines().any { it.startsWith("Fecha;Comercio / Entidad;Importe;Tipo") }
    }

    override fun parse(content: String, bankId: Int): List<Transaction> {
        val lines = content.lines()

        // Extraer el mes de la cabecera
        val creditMonth = extractCreditMonth(lines)

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
                        creditMonth = creditMonth,
                        bankId = bankId
                    )
                }.getOrNull()
            }
    }

    private fun extractCreditMonth(lines: List<String>): String? {
        val mesLine = lines.firstOrNull { it.startsWith("Mes:") } ?: return null
        val parts = mesLine.split(";")
        if (parts.size < 2) return null
        val mesValor = parts[1].trim().uppercase()
        val tokens = mesValor.split(" ")
        if (tokens.size < 2) return null
        val monthNum = monthMap[tokens[0]] ?: return null
        val year = tokens[1]
        return "$year$monthNum"
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