package com.kapi.ledgerroast.data

import com.kapi.ledgerroast.prefs.AppPreferences
import com.kapi.ledgerroast.prefs.Settings
import com.kapi.ledgerroast.roast.RoastEngine
import com.kapi.ledgerroast.roast.RoastEvent
import com.kapi.ledgerroast.roast.RoastResult
import com.kapi.ledgerroast.roast.RoastTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import java.time.Instant
import java.time.ZoneId

class AppRepository(
    private val db: AppDatabase,
    private val prefs: AppPreferences
) {
    private val engine = RoastEngine()

    val settingsFlow: Flow<Settings> = prefs.settingsFlow

    fun settingsSnapshot(): Settings = Settings(
        roastEnabled = true,
        intensity = 2,
        fourthWall = true,
        reminderEnabled = true,
        dailyReminderHour = 21
    )

    fun observeRecentTransactions(): Flow<List<TransactionEntity>> = db.transactionDao().observeRecent()

    fun observeBudgets(): Flow<List<BudgetEntity>> = db.budgetDao().observeAll()

    fun observeGoals(): Flow<List<GoalEntity>> = db.goalDao().observeAll()

    suspend fun addTransaction(
        type: TxType,
        amountCents: Long,
        category: String,
        note: String?,
        timestampMs: Long
    ): RoastResult? {
        val id = db.transactionDao().insert(
            TransactionEntity(
                type = type,
                amountCents = amountCents,
                category = category,
                note = note?.takeIf { it.isNotBlank() },
                timestampMs = timestampMs
            )
        )

        val settings = prefs.settingsFlow.first()
        val monthRange = monthRange(timestampMs)
        val budget = db.budgetDao().get(category)
        val spent = db.transactionDao().sumExpenseByCategoryBetween(category, monthRange.first, monthRange.second)
        val ratio = if (budget?.monthlyLimitCents != null && budget.monthlyLimitCents > 0) spent.toDouble() / budget.monthlyLimitCents.toDouble() else null
        val isNight = isNight(timestampMs)

        val event = RoastEvent(
            trigger = RoastTrigger.TRANSACTION_SAVED,
            category = category,
            amountCents = amountCents,
            budgetRatio = ratio,
            isNight = isNight,
            isFrequent = false,
            isIncome = type == TxType.INCOME
        )
        return engine.generate(settings, event, salt = id xor timestampMs)
    }

    suspend fun upsertBudget(category: String, monthlyLimitCents: Long) {
        db.budgetDao().upsert(BudgetEntity(category = category, monthlyLimitCents = monthlyLimitCents))
    }

    suspend fun deleteBudget(category: String) {
        db.budgetDao().delete(category)
    }

    suspend fun createGoal(name: String, targetCents: Long): Long {
        return db.goalDao().insert(
            GoalEntity(
                name = name,
                targetCents = targetCents,
                currentCents = 0,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun addGoalProgress(goal: GoalEntity, deltaCents: Long) {
        val next = (goal.currentCents + deltaCents).coerceAtLeast(0)
        db.goalDao().update(goal.copy(currentCents = next))
    }

    suspend fun buildGoalRoast(before: GoalEntity, afterCurrentCents: Long): RoastResult? {
        val settings = prefs.settingsFlow.first()
        val target = before.targetCents.coerceAtLeast(1)
        val beforeRatio = before.currentCents.toDouble() / target.toDouble()
        val afterRatio = afterCurrentCents.toDouble() / target.toDouble()

        val hitMilestone = milestoneHit(beforeRatio, afterRatio)
        val trigger = if (hitMilestone) RoastTrigger.GOAL_MILESTONE else return null

        val event = RoastEvent(
            trigger = trigger,
            category = null,
            amountCents = null,
            budgetRatio = afterRatio,
            isNight = isNight(System.currentTimeMillis()),
            isFrequent = false,
            isIncome = false
        )
        return engine.generate(settings, event, salt = before.id xor afterCurrentCents)
    }

    fun observeMonthlyReport(nowMs: Long): Flow<MonthlyReport> {
        val range = monthRange(nowMs)
        return observeRecentTransactions().mapLatest {
            val sums = db.transactionDao().sumExpenseGroupByCategoryBetween(range.first, range.second)
            val total = sums.sumOf { row -> row.amountCents }
            MonthlyReport(range.first, range.second, total, sums)
        }
    }

    suspend fun setRoastEnabled(enabled: Boolean) = prefs.setRoastEnabled(enabled)
    suspend fun setIntensity(intensity: Int) = prefs.setIntensity(intensity)
    suspend fun setFourthWall(enabled: Boolean) = prefs.setFourthWall(enabled)
    suspend fun setReminderEnabled(enabled: Boolean) = prefs.setReminderEnabled(enabled)
    suspend fun setDailyReminderHour(hour: Int) = prefs.setDailyReminderHour(hour)

    suspend fun buildReminderRoast(): RoastResult? {
        val settings = prefs.settingsFlow.first()
        val now = System.currentTimeMillis()
        val dayRange = dayRange(now)
        val count = db.transactionDao().countBetween(dayRange.first, dayRange.second)
        if (count > 0) return null
        val event = RoastEvent(
            trigger = RoastTrigger.REMINDER,
            category = null,
            amountCents = null,
            budgetRatio = null,
            isNight = isNight(now),
            isFrequent = false,
            isIncome = false
        )
        return engine.generate(settings, event, salt = now)
    }

    suspend fun buildReportRoast(nowMs: Long): RoastResult? {
        val settings = prefs.settingsFlow.first()
        val event = RoastEvent(
            trigger = RoastTrigger.REPORT_VIEWED,
            category = null,
            amountCents = null,
            budgetRatio = null,
            isNight = isNight(nowMs),
            isFrequent = false,
            isIncome = false
        )
        return engine.generate(settings, event, salt = nowMs)
    }

    data class MonthlyReport(
        val monthStartMs: Long,
        val monthEndMs: Long,
        val totalExpenseCents: Long,
        val byCategory: List<CategorySumRow>
    )

    private fun monthRange(nowMs: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val first = date.withDayOfMonth(1)
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to (endExclusive - 1)
    }

    private fun dayRange(nowMs: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to (endExclusive - 1)
    }

    private fun isNight(timestampMs: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val hour = Instant.ofEpochMilli(timestampMs).atZone(zone).hour
        return hour >= 23 || hour <= 5
    }

    private fun milestoneHit(before: Double, after: Double): Boolean {
        val points = doubleArrayOf(0.25, 0.5, 0.75, 1.0)
        return points.any { p -> before < p && after >= p }
    }
}
