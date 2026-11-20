package com.example.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.data.repository.WeatherRepository

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val repository = WeatherRepository.getInstance(context)
        val result = repository.refreshActiveLocation(syncWidgets = true)
        if (result.isFailure) {
            WidgetSyncHelper.updateAllWidgets(context)
        }
    }
}

class PreviousCityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.getInstance(context).cycleWidgetLocation(-1)
    }
}

class NextCityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.getInstance(context).cycleWidgetLocation(1)
    }
}

class ToggleUnitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.getInstance(context).toggleTemperatureUnit()
    }
}

class ToggleForecastModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.getInstance(context).toggleForecastWidgetMode()
    }
}
