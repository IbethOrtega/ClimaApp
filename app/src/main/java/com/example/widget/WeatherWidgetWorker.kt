package com.example.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.WeatherRepository

class WeatherWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = WeatherRepository.getInstance(applicationContext)
        val result = repository.refreshActiveLocation(syncWidgets = true)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
