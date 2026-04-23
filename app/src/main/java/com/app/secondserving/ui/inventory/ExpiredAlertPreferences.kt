package com.app.secondserving.ui.inventory

import android.content.Context

object ExpiredAlertPreferences {
    private const val PREFS_NAME = "inventory_alert_prefs"
    private const val KEY_HIDE_EXPIRED_ALERT_UNTIL_MS = "hide_expired_alert_until_ms"
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    fun shouldShowExpiredAlert(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hiddenUntil = prefs.getLong(KEY_HIDE_EXPIRED_ALERT_UNTIL_MS, 0L)
        return System.currentTimeMillis() >= hiddenUntil
    }

    fun hideExpiredAlertFor24Hours(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_HIDE_EXPIRED_ALERT_UNTIL_MS, System.currentTimeMillis() + ONE_DAY_MS)
            .apply()
    }

    fun resetExpiredAlertVisibility(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_HIDE_EXPIRED_ALERT_UNTIL_MS, 0L)
            .apply()
    }
}
