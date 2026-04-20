package com.kapi.ledgerroast.ui

import java.math.BigDecimal
import java.math.RoundingMode

object Money {
    fun parseToCents(input: String): Long? {
        val trimmed = input.trim().replace(",", "")
        if (trimmed.isBlank()) return null
        return runCatching {
            val bd = BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP)
            bd.movePointRight(2).longValueExact()
        }.getOrNull()
    }

    fun formatCents(cents: Long): String {
        val bd = BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        return bd.toPlainString()
    }
}
