package com.kapi.ledgerroast.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kapi_settings")

class AppPreferences private constructor(private val context: Context) {
    private object Keys {
        val roastEnabled = booleanPreferencesKey("roast_enabled")
        val intensity = intPreferencesKey("intensity")
        val fourthWall = booleanPreferencesKey("fourth_wall")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val dailyReminderHour = intPreferencesKey("daily_reminder_hour")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            roastEnabled = prefs[Keys.roastEnabled] ?: true,
            intensity = prefs[Keys.intensity] ?: 2,
            fourthWall = prefs[Keys.fourthWall] ?: true,
            reminderEnabled = prefs[Keys.reminderEnabled] ?: true,
            dailyReminderHour = prefs[Keys.dailyReminderHour] ?: 21
        ).normalized()
    }

    suspend fun setRoastEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.roastEnabled] = enabled }
    }

    suspend fun setIntensity(intensity: Int) {
        context.dataStore.edit { it[Keys.intensity] = intensity.coerceIn(1, 3) }
    }

    suspend fun setFourthWall(enabled: Boolean) {
        context.dataStore.edit { it[Keys.fourthWall] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.reminderEnabled] = enabled }
    }

    suspend fun setDailyReminderHour(hour: Int) {
        context.dataStore.edit { it[Keys.dailyReminderHour] = hour.coerceIn(0, 23) }
    }

    companion object {
        @Volatile
        private var instance: AppPreferences? = null

        fun get(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
