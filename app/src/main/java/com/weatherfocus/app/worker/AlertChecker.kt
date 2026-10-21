package com.weatherfocus.app.worker

import android.content.Context
import com.weatherfocus.app.data.alert.CustomWeatherAlertEngine
import com.weatherfocus.app.data.model.AppSettings
import com.weatherfocus.app.data.model.OpenMeteoHourly
import com.weatherfocus.app.notification.NotificationHelper

object AlertChecker {
    /** Evaluates the user's alert rules against [hourly] and posts notifications for anything new. */
    fun run(context: Context, settings: AppSettings, hourly: OpenMeteoHourly?) {
        if (!settings.customAlertRules.enabled || hourly == null) return
        val matches = CustomWeatherAlertEngine.evaluate(hourly, settings.customAlertRules)
        NotificationHelper.notifyNew(context, matches, settings.doNotDisturb)
    }
}
