package com.jexpop.appkotlininggas.domain.usecase

import com.jexpop.appkotlininggas.data.model.Transaction
import com.jexpop.appkotlininggas.data.repository.CategorizationException
import com.jexpop.appkotlininggas.data.repository.CategorizationRepository
import com.jexpop.appkotlininggas.data.repository.CategorizationRule

class CategorizationUseCase(
    private val repository: CategorizationRepository = CategorizationRepository()
) {

    companion object {
        // Rango por defecto (numeración de usuario, 1-based) cuando la regla no
        // define range_start/range_end propios. Mantiene el comportamiento previo
        // a la v1.0.14 para reglas ya existentes.
        private const val DEFAULT_RANGE_START = 18
        private const val DEFAULT_RANGE_END = 30
    }

    suspend fun categorize(
        transactions: List<Transaction>,
        month: String
    ): Result<List<Transaction>> {
        return runCatching {
            val exceptions = repository.getExceptions(month).getOrThrow()
            val rules = repository.getRules().getOrThrow()

            transactions.map { transaction ->
                val groupId = findGroupId(transaction, exceptions, rules)
                transaction.copy(groupId = groupId)
            }
        }
    }

    private fun findGroupId(
        transaction: Transaction,
        exceptions: List<CategorizationException>,
        rules: List<CategorizationRule>
    ): Int? {
        // 1. Buscar en excepciones manuales
        val exception = exceptions.firstOrNull { ex ->
            ex.value1?.let { transaction.concept.contains(it, ignoreCase = true) } == true
        }
        if (exception != null) return exception.group_id

        // 2. Buscar en reglas automáticas
        return findByRule(transaction, rules)
    }

    private fun findByRule(
        transaction: Transaction,
        rules: List<CategorizationRule>
    ): Int? {
        for (rule in rules) {
            val matched = when (rule.rule_type) {
                1 -> matchFullText(transaction.concept, rule)
                2 -> matchFirst15(transaction.concept, rule)
                4 -> matchFirst3AndPositions(transaction.concept, rule)
                5 -> matchFirst3WithAmount(transaction.concept, transaction, rule)
                6 -> matchFirst20WithAmount(transaction.concept, transaction, rule)
                7 -> matchFirst3PositionsWithAmount(transaction.concept, transaction, rule)
                99 -> transaction.paymentType == "C"
                else -> false
            }
            if (matched) return rule.group_id
        }
        return null
    }

    // Tipo 1: texto completo
    private fun matchFullText(concept: String, rule: CategorizationRule): Boolean {
        return rule.value1?.let { concept.equals(it, ignoreCase = true) } == true
    }

    // Tipo 2: primeros 15 caracteres
    private fun matchFirst15(concept: String, rule: CategorizationRule): Boolean {
        val prefix = concept.take(15)
        return rule.value1?.let { prefix.contains(it, ignoreCase = true) } == true
    }

    /**
     * Extrae el rango de texto configurado en la regla (numeración de usuario, 1-based,
     * inclusive). Si la regla no define range_start/range_end, usa el rango por defecto
     * 18-30 (comportamiento previo a la v1.0.14).
     */
    private fun extractRange(concept: String, rule: CategorizationRule): String {
        val start = rule.range_start ?: DEFAULT_RANGE_START
        val end = rule.range_end ?: DEFAULT_RANGE_END
        // Numeración de usuario 1-based -> índice de código 0-based: restar 1 al inicio.
        val startIndex = start - 1
        if (startIndex < 0 || startIndex >= concept.length || end < start) return ""
        val endIndex = minOf(end, concept.length)
        return concept.substring(startIndex, endIndex)
    }

    // Tipo 4: primeros 3 chars + rango configurable (por defecto 18-30)
    private fun matchFirst3AndPositions(concept: String, rule: CategorizationRule): Boolean {
        val first3 = concept.take(3)
        val rangeText = extractRange(concept, rule)
        return rule.value1?.let { first3.equals(it, ignoreCase = true) } == true &&
                rule.value2?.let { rangeText.contains(it, ignoreCase = true) } == true
    }

    /**
     * El importe esperado por una regla (value2/value3/value4 en tipos 5, 6, 7) se
     * introduce siempre como valor absoluto positivo. Si la transacción es un gasto
     * o un ingreso lo decide flow_type ("D"/"H"), no el signo de amount (que no es
     * fiable como distinción gasto/ingreso en este modelo). rule.is_income decide
     * qué flow_type espera la regla, para poder distinguir conceptos idénticos que
     * a veces son un cargo y a veces un abono.
     */
    private fun flowTypeMatches(flowType: String, isIncome: Boolean): Boolean {
        val expected = if (isIncome) "H" else "D"
        return flowType == expected
    }

    // Tipo 5: primeros 3 chars + valor numérico de corte (gasto por defecto, is_income=true para ingresos)
    private fun matchFirst3WithAmount(concept: String, transaction: Transaction, rule: CategorizationRule): Boolean {
        val first3 = concept.take(3)
        val cutoff = rule.value2?.toDoubleOrNull() ?: return false
        val comparison = rule.value3 ?: return false
        val first3Match = rule.value1?.let { first3.equals(it, ignoreCase = true) } == true
        if (!flowTypeMatches(transaction.flowType, rule.is_income)) return false
        val absAmount = kotlin.math.abs(transaction.amount)
        val amountMatch = when (comparison) {
            ">" -> absAmount > cutoff
            "<" -> absAmount < cutoff
            ">=" -> absAmount >= cutoff
            "<=" -> absAmount <= cutoff
            else -> false
        }
        return first3Match && amountMatch
    }

    // Tipo 6: primeros 20 chars + rango de importe (gasto por defecto, is_income=true para ingresos)
    private fun matchFirst20WithAmount(concept: String, transaction: Transaction, rule: CategorizationRule): Boolean {
        // Validar que hay suficientes caracteres para capturar primeros 20
        if (concept.length < 20) return false
        val first20 = concept.take(20)
        val min = rule.value2?.toDoubleOrNull() ?: return false
        val max = rule.value3?.toDoubleOrNull() ?: return false
        if (!flowTypeMatches(transaction.flowType, rule.is_income)) return false
        val absAmount = kotlin.math.abs(transaction.amount)
        return rule.value1?.let { first20.contains(it, ignoreCase = true) } == true &&
                absAmount >= min && absAmount <= max
    }

    // Tipo 7: primeros 3 chars + rango configurable (por defecto 18-30) + rango de importe
    // (gasto por defecto, is_income=true para ingresos)
    private fun matchFirst3PositionsWithAmount(concept: String, transaction: Transaction, rule: CategorizationRule): Boolean {
        val first3 = concept.take(3)
        val rangeText = extractRange(concept, rule)
        val min = rule.value3?.toDoubleOrNull() ?: return false
        val max = rule.value4?.toDoubleOrNull() ?: return false
        if (!flowTypeMatches(transaction.flowType, rule.is_income)) return false
        val absAmount = kotlin.math.abs(transaction.amount)
        return rule.value1?.let { first3.equals(it, ignoreCase = true) } == true &&
                rule.value2?.let { rangeText.contains(it, ignoreCase = true) } == true &&
                absAmount >= min && absAmount <= max
    }
}