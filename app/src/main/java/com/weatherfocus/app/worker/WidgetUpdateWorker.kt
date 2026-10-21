package com.weatherfocus.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weatherfocus.app.data.prefs.SettingsRepository
import com.weatherfocus.app.data.repository.WeatherRepository
import com.weatherfocus.app.widget.WidgetCache
import com.weatherfocus.app.widget.WidgetRenderer
import kotlinx.coroutines.flow.first

/** Fetches fresh weather, redraws all home-screen widgets and checks the alert rules (so alerts also fire while the app is closed). */
class WidgetUpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        return try {
            val settings = SettingsRepository(ctx).settingsFlow.first()
            val bundle = WeatherRepository().loadWeather(settings)
            // loadWeather swallows individual source errors; "no data at all" has to count as a failure here,
            // otherwise the widget would be redrawn full of "--" (the "I have to refresh it a few times" bug).
            val gotData = bundle.current.available && bundle.hourlyForAlerts != null
            if (gotData) {
                val now = System.currentTimeMillis()
                WidgetCache.save(ctx, bundle, now)
                WidgetRenderer.renderAll(ctx, settings, bundle, fetchedAt = now)
                runCatching { AlertChecker.run(ctx, settings, bundle.hourlyForAlerts) }
                Result.success()
            } else {
                showCachedOrStopSpinner(ctx)
                if (runAttemptCount < 3) Result.retry() else Result.success()
            }
        } catch (e: Exception) {
            showCachedOrStopSpinner(ctx)
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }

    private suspend fun showCachedOrStopSpinner(ctx: Context) {
        val settings = runCatching { SettingsRepository(ctx).settingsFlow.first() }.getOrNull()
        val cached = WidgetCache.load(ctx)
        if (settings != null && cached?.bundle != null) {
            WidgetRenderer.renderAll(ctx, settings, cached.bundle, fetchedAt = cached.fetchedAt, stale = true)
        } else {
            WidgetRenderer.setRefreshing(ctx, false)
        }
    }
}
