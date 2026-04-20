package com.kapi.ledgerroast

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kapi.ledgerroast.data.AppDatabase
import com.kapi.ledgerroast.data.AppRepository
import com.kapi.ledgerroast.prefs.AppPreferences
import com.kapi.ledgerroast.reminder.ReminderScheduler
import com.kapi.ledgerroast.ui.AppRoot
import com.kapi.ledgerroast.ui.theme.KapiLedgerRoastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val db = AppDatabase.get(this)
        val prefs = AppPreferences.get(this)
        val repo = AppRepository(db, prefs)
        val scheduler = ReminderScheduler(this)

        setContent {
            val settings by repo.settingsFlow.collectAsStateWithLifecycle(initialValue = repo.settingsSnapshot())

            LaunchedEffect(settings.reminderEnabled, settings.dailyReminderHour) {
                scheduler.sync(settings)
            }

            KapiLedgerRoastTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(repo = repo)
                }
            }
        }
    }
}
