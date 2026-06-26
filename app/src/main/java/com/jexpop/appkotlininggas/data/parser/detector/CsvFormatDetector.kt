package com.jexpop.appkotlininggas.data.parser.detector

import com.jexpop.appkotlininggas.data.parser.strategy.BankAAccountParser
import com.jexpop.appkotlininggas.data.parser.strategy.BankACreditParser
import com.jexpop.appkotlininggas.data.parser.strategy.CsvParserStrategy

object CsvFormatDetector {

    private val parsers: List<CsvParserStrategy> = listOf(
        BankAAccountParser(),
        BankACreditParser()
        // Aquí se añadirán los parsers de nuevos bancos
    )

    fun detect(content: String): CsvParserStrategy? {
        return parsers.firstOrNull { it.canParse(content) }
    }
}