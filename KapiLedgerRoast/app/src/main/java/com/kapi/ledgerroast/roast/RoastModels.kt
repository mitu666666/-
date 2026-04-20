package com.kapi.ledgerroast.roast

enum class RoastTrigger {
    TRANSACTION_SAVED,
    BUDGET_NEAR,
    BUDGET_OVER,
    GOAL_MILESTONE,
    GOAL_BEHIND,
    REPORT_VIEWED,
    REMINDER
}

data class RoastEvent(
    val trigger: RoastTrigger,
    val category: String?,
    val amountCents: Long?,
    val budgetRatio: Double?,
    val isNight: Boolean,
    val isFrequent: Boolean
)

data class RoastTemplate(
    val id: Int,
    val tags: Set<String>,
    val roast: String,
    val suggestion: String,
    val intensity: Int
)

data class RoastResult(
    val roastLines: List<String>,
    val suggestion: String,
    val intensity: Int
)
