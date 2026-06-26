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
                throw Exception("NO_TRANSACTIONS")
            }
            val yearMonth = transactions.first().transactionDate.substring(0, 7)
            val paymentType = transactions.first().paymentType
            repository.deleteByMonthBankAndType(yearMonth, bankId, paymentType).getOrThrow()
            repository.insertTransactions(transactions).getOrThrow()
            transactions.size
        }
    }
}