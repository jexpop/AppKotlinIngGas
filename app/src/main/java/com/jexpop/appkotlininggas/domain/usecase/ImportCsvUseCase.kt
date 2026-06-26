package com.jexpop.appkotlininggas.domain.usecase

import com.jexpop.appkotlininggas.data.parser.CsvParser
import com.jexpop.appkotlininggas.data.repository.PeriodRepository
import com.jexpop.appkotlininggas.data.repository.TransactionRepository

class ImportCsvUseCase(
    private val repository: TransactionRepository = TransactionRepository(),
    private val periodRepository: PeriodRepository = PeriodRepository()
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

            val yearMonth = transactions.first().transactionDate.substring(0, 7).replace("-", "")
            val year = yearMonth.substring(0, 4)
            val paymentType = transactions.first().paymentType

            // Gestionar período y mes
            periodRepository.getOrCreateYear(year).getOrThrow()
            val periodMonth = periodRepository.getOrCreateMonth(yearMonth).getOrThrow()
            periodRepository.setCurrentMonth(periodMonth.id!!).getOrThrow()

            // Borrar transacciones existentes del mismo mes, banco y tipo
            repository.deleteByMonthBankAndType(yearMonth, bankId, paymentType).getOrThrow()

            // Asignar period_month_id y sequence_number a cada transacción
            val transactionsWithPeriod = transactions
                .sortedBy { it.transactionDate }
                .mapIndexed { index, transaction ->
                    transaction.copy(
                        periodMonthId = periodMonth.id,
                        sequenceNumber = index + 1
                    )
                }

            repository.insertTransactions(transactionsWithPeriod).getOrThrow()
            transactionsWithPeriod.size
        }
    }
}