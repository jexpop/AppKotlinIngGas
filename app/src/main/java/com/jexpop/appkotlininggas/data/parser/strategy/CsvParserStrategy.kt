package com.jexpop.appkotlininggas.data.parser.strategy

import com.jexpop.appkotlininggas.data.model.Transaction

interface CsvParserStrategy {
    val bankName: String
    val csvType: String
    fun canParse(content: String): Boolean
    fun parse(content: String, bankId: Int): List<Transaction>
}