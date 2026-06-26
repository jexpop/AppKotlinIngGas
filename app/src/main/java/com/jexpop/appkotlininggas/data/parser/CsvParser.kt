package com.jexpop.appkotlininggas.data.parser

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.data.parser.detector.CsvFormatDetector

object CsvParser {

    fun parse(content: String, bankId: Int): List<Transaction> {
        val strategy = CsvFormatDetector.detect(content)
            ?: throw Exception("Formato de fichero no reconocido")
        return strategy.parse(content, bankId)
    }
}