package com.kapi.ledgerroast.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kapi.ledgerroast.prefs.Settings
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun sync(settings: Settings) {
        if (!settings.reminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val delayMs = computeInitialDelayMs(settings.dailyReminderHour)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun computeInitialDelayMs(hour: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(Instant.now(), zone)
        val targetToday = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        val target = if (targetToday.isAfter(now)) targetToday else targetToday.plusDays(1)
        val duration = Duration.between(now, target)
        return duration.toMillis().coerceAtLeast(0)
    }

    companion object {
        private const val WORK_NAME = "kapi_daily_reminder"
    }
}
