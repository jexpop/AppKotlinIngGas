package com.jexpop.appkotlininggas.data.parser

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.data.parser.detector.CsvFormatDetector

object CsvParser {

    fun parse(content: String, bankId: Int): List<Transaction> {
        return CsvFormatDetector.detect(content)?.parse(content, bankId)
            ?: throw Exception("FORMAT_NOT_RECOGNIZED")
    }
}