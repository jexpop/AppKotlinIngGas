package com.jexpop.appkotlininggas.domain.usecase

import android.content.Context
import com.jexpop.appkotlininggas.data.DriveManager
import com.jexpop.appkotlininggas.data.EncryptionManager
import com.jexpop.appkotlininggas.data.StorageManager
import com.jexpop.appkotlininggas.data.parser.CsvParser
import com.jexpop.appkotlininggas.data.repository.BankRepository
import com.jexpop.appkotlininggas.data.repository.PeriodRepository
import com.jexpop.appkotlininggas.data.repository.TransactionRepository

class ImportCsvUseCase(
    private val repository: TransactionRepository = TransactionRepository(),
    private val periodRepository: PeriodRepository = PeriodRepository(),
    private val categorizationUseCase: CategorizationUseCase = CategorizationUseCase(),
    private val bankRepository: BankRepository = BankRepository()
) {

    suspend fun execute(
        content: String,
        bankId: Int,
        context: Context
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

            // Borrar transacciones existentes
            if (paymentType == "C") {
                val creditMonth = transactions.first().creditMonth ?: yearMonth
                repository.deleteByCreditMonthAndBank(creditMonth, bankId).getOrThrow()
            } else {
                repository.deleteByMonthBankAndType(yearMonth, bankId, paymentType).getOrThrow()
            }

            // Asignar period_month_id y ordenar
            val transactionsWithPeriod = transactions
                .sortedBy { it.transactionDate }
                .map { it.copy(periodMonthId = periodMonth.id) }

            // Categorizar
            val categorizedTransactions = categorizationUseCase
                .categorize(transactionsWithPeriod, yearMonth)
                .getOrThrow()

            // Insertar transacciones
            repository.insertTransactions(categorizedTransactions).getOrThrow()

            // Backup cifrado si hay contraseña configurada
            if (EncryptionManager.hasPassword(context)) {
                val password = EncryptionManager.getPassword(context)!!
                val bank = bankRepository.getAllBanks().getOrNull()
                    ?.firstOrNull { it.id == bankId }
                val bankCode = bank?.code ?: bankId.toString()
                val fileName = EncryptionManager.generateFileName(bankCode, paymentType, yearMonth)
                val encryptedData = EncryptionManager.encrypt(content.toByteArray(), password)

                // Subir a Supabase Storage
                StorageManager.uploadCsvBackup(fileName, encryptedData)
                    .onFailure { android.util.Log.e("BACKUP", "Error Supabase Storage: ${it.message}") }

                // Subir a Drive
                DriveManager.uploadCsvBackup(context, fileName, encryptedData)
                    .onFailure { android.util.Log.e("BACKUP", "Error Drive: ${it.message}") }
            }

            categorizedTransactions.size
        }
    }
}