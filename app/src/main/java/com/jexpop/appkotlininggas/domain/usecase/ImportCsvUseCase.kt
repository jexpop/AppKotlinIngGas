package com.jexpop.appkotlininggas.domain.usecase

import com.jexpop.appkotlininggas.data.model.CsvType
import com.jexpop.appkotlininggas.data.parser.CsvParser
import com.jexpop.appkotlininggas.data.repository.TransactionRepository

class ImportCsvUseCase(
    private val repository: TransactionRepository = TransactionRepository()
) {

    suspend fun execute(
        content: String,
        csvType: CsvType,
        bankId: Int
    ): Result<Int> {
        return runCatching {
            val transactions = CsvParser.parse(content, csvType, bankId)
            if (transactions.isEmpty()) {
                throw Exception("No se encontraron transacciones en el fichero")
            }

            // Detectar el año+mes del CSV (todas las transacciones son del mismo mes)
            val yearMonth = transactions.first().transactionDate.substring(0, 7)

            // Borrar transacciones existentes del mismo mes y banco
            repository.deleteByMonthAndBank(yearMonth, bankId).getOrThrow()

            // Insertar las nuevas transacciones
            repository.insertTransactions(transactions).getOrThrow()

            transactions.size
        }
    }
}