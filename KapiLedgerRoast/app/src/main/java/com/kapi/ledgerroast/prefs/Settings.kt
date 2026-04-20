package com.kapi.ledgerroast.prefs

data class Settings(
    val roastEnabled: Boolean,
    val intensity: Int,
    val fourthWall: Boolean,
    val reminderEnabled: Boolean,
    val dailyReminderHour: Int
) {
    fun normalized(): Settings {
        val fixedIntensity = intensity.coerceIn(1, 3)
        val fixedHour = dailyReminderHour.coerceIn(0, 23)
        return copy(intensity = fixedIntensity, dailyReminderHour = fixedHour)
    }
}
