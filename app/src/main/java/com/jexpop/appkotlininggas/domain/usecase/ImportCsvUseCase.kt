package com.jexpop.appkotlininggas.domain.usecase

import com.jexpop.appkotlininggas.data.parser.CsvParser
import com.jexpop.appkotlininggas.data.repository.TransactionRepository

class ImportCsvUseCase(
    private val repository: TransactionRepository = TransactionRepository()
) {

    suspend fun execute(
        content: String,
        bankId: Int
    ): Result<Int> {
        return runCatching {
            val transactions = CsvParser.parse(content, bankId)
            if (transactions.isEmpty()) {
                throw Exception("No se encontraron transacciones en el fichero")
            }

            val yearMonth = transactions.first().transactionDate.substring(0, 7)
            val paymentType = transactions.first().paymentType

            // Borra solo las transacciones del mismo mes, banco y tipo de pago
            repository.deleteByMonthBankAndType(yearMonth, bankId, paymentType).getOrThrow()

            repository.insertTransactions(transactions).getOrThrow()
            transactions.size
        }
    }
}