package com.kapi.ledgerroast.roast

import com.kapi.ledgerroast.prefs.Settings
import kotlin.math.roundToLong
import kotlin.random.Random

class RoastEngine {
    fun generate(settings: Settings, event: RoastEvent, salt: Long): RoastResult? {
        if (!settings.roastEnabled) return null

        val triggerTag = when (event.trigger) {
            RoastTrigger.TRANSACTION_SAVED -> if ((event.budgetRatio ?: 0.0) > 1.0) "budget_over" else if ((event.budgetRatio ?: 0.0) >= 0.8) "budget_near" else "expense"
            RoastTrigger.BUDGET_NEAR -> "budget_near"
            RoastTrigger.BUDGET_OVER -> "budget_over"
            RoastTrigger.GOAL_MILESTONE -> "goal"
            RoastTrigger.GOAL_BEHIND -> "goal_behind"
            RoastTrigger.REPORT_VIEWED -> "report"
            RoastTrigger.REMINDER -> "reminder"
        }

        val dynamicTags = buildSet {
            add(triggerTag)
            event.category?.let { add(it) }
            if (event.isNight) add("night")
            if (settings.fourthWall) add("fourth_wall")
            if (event.trigger == RoastTrigger.TRANSACTION_SAVED && event.amountCents != null) {
                if (event.amountCents >= 200_00L) add("big")
            }
        }

        val allowed = RoastTemplatesZh.templates.asSequence()
            .filter { it.intensity <= settings.intensity }
            .filter { tpl -> settings.fourthWall || !tpl.tags.contains("fourth_wall") }
            .toList()

        val candidates = allowed.asSequence()
            .filter { it.tags.any(dynamicTags::contains) }
            .sortedByDescending { tpl -> tpl.tags.count(dynamicTags::contains) }
            .toList()

        val pickFrom = when {
            candidates.isNotEmpty() -> candidates.take(30)
            else -> allowed
        }

        if (pickFrom.isEmpty()) return null

        val seed = salt xor (event.amountCents ?: 0L) xor (event.category?.hashCode()?.toLong() ?: 0L) xor triggerTag.hashCode().toLong()
        val rng = Random(seed)
        val template = pickFrom[rng.nextInt(pickFrom.size)]

        val roastLines = template.roast.split('\n').map { it.trim() }.filter { it.isNotBlank() }.take(2)
        return RoastResult(roastLines = roastLines, suggestion = template.suggestion, intensity = template.intensity)
    }

    fun formatBudgetRatioText(ratio: Double?): String? {
        if (ratio == null || ratio.isNaN() || ratio.isInfinite()) return null
        val pct = (ratio * 100.0).roundToLong().coerceIn(0, 999)
        return "$pct%"
    }
}
