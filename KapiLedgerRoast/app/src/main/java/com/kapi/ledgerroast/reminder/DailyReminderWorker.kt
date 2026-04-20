package com.kapi.ledgerroast.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kapi.ledgerroast.data.AppDatabase
import com.kapi.ledgerroast.data.AppRepository
import com.kapi.ledgerroast.prefs.AppPreferences

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val prefs = AppPreferences.get(applicationContext)
        val repo = AppRepository(db, prefs)

        val roast = repo.buildReminderRoast() ?: return Result.success()
        val title = "咔皮来敲门"
        val body = (roast.roastLines + listOf("建议：${roast.suggestion}")).joinToString("\n")
        KapiNotification.show(applicationContext, title, body)
        return Result.success()
    }
}
