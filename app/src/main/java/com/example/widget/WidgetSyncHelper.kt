package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetSyncHelper {
    val KEY_WIDGET_REVISION = longPreferencesKey("widget_revision")
    private const val PERIODIC_SYNC_WORK = "brisa_periodic_weather_sync"

    suspend fun updateAllWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val now = System.currentTimeMillis()

        val clockWidget = ClockSeasonWeatherWidget()
        for (glanceId in manager.getGlanceIds(ClockSeasonWeatherWidget::class.java)) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[KEY_WIDGET_REVISION] = now
            }
            clockWidget.update(context, glanceId)
        }

        val forecastWidget = ForecastQuickWidget()
        for (glanceId in manager.getGlanceIds(ForecastQuickWidget::class.java)) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[KEY_WIDGET_REVISION] = now
            }
            forecastWidget.update(context, glanceId)
        }
    }

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
