package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.data.local.BrisaPreferencesState
import com.example.data.local.WeatherPreferencesStore
import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.domain.model.DailyPoint
import com.example.domain.model.HourlyPoint
import com.example.domain.model.SavedLocation
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import com.example.domain.model.WeatherSnapshot
import com.example.widget.WidgetSyncHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class WeatherRepository private constructor(private val appContext: Context) {

    val store = WeatherPreferencesStore(appContext)
    val stateFlow: Flow<BrisaPreferencesState> = store.stateFlow

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val forecastApi: OpenMeteoForecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoForecastApi::class.java)

    private val geocodingApi: OpenMeteoGeocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoGeocodingApi::class.java)

    suspend fun refreshWeather(location: SavedLocation, syncWidgets: Boolean = true): Result<WeatherSnapshot> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = forecastApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
                val snapshot = mapResponseToSnapshot(location, response)
                store.saveWeatherSnapshot(snapshot)
                if (syncWidgets) {
                    WidgetSyncHelper.updateAllWidgets(appContext)
                }
                snapshot
            }
        }
    }

    suspend fun refreshActiveLocation(syncWidgets: Boolean = true): Result<WeatherSnapshot> {
        val state = store.getSnapshotState()
        val active = state.savedLocations.firstOrNull { it.id == state.selectedLocationId }
            ?: state.savedLocations.first()
        return refreshWeather(active, syncWidgets = syncWidgets)
    }

    suspend fun selectLocationAndRefresh(locationId: String): Result<WeatherSnapshot> {
        store.setSelectedLocation(locationId)
        WidgetSyncHelper.updateAllWidgets(appContext)
        return refreshActiveLocation(syncWidgets = true)
    }

    suspend fun addLocationAndSelect(location: SavedLocation): Result<WeatherSnapshot> {
        store.addOrUpdateLocation(location, selectImmediately = true)
        WidgetSyncHelper.updateAllWidgets(appContext)
        return refreshWeather(location, syncWidgets = true)
    }

    suspend fun removeLocation(locationId: String) {
        store.removeLocation(locationId)
        WidgetSyncHelper.updateAllWidgets(appContext)
    }

    suspend fun toggleTemperatureUnit(): TemperatureUnit {
        val next = store.toggleTemperatureUnit()
        WidgetSyncHelper.updateAllWidgets(appContext)
        return next
    }

    suspend fun setUse24HourClock(use24h: Boolean) {
        store.setUse24HourClock(use24h)
        WidgetSyncHelper.updateAllWidgets(appContext)
    }

    suspend fun toggleForecastWidgetMode() {
        store.toggleForecastWidgetMode()
        WidgetSyncHelper.updateAllWidgets(appContext)
    }

    suspend fun cycleWidgetLocation(step: Int): Result<WeatherSnapshot> {
        val next = store.cycleLocation(step)
        WidgetSyncHelper.updateAllWidgets(appContext)
        return refreshWeather(next, syncWidgets = true)
    }

    suspend fun searchCities(query: String): Result<List<SavedLocation>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return Result.success(emptyList())
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = geocodingApi.searchLocations(query = trimmed)
                response.results.orEmpty().map { dto ->
                    SavedLocation(
                        id = "city_${dto.id}",
                        name = dto.name,
                        country = dto.country.orEmpty(),
                        admin1 = dto.admin1,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        isCurrentGps = false,
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentGpsLocationAndWeather(): Result<WeatherSnapshot> {
        val coordsResult = withContext(Dispatchers.Main) {
            runCatching {
                val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                val lastKnown = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
                val fresh = lastKnown ?: suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val tokenSource = CancellationTokenSource()
                    cont.invokeOnCancellation { tokenSource.cancel() }
                    fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
                fresh ?: error("No se pudo obtener la ubicación del dispositivo")
            }
        }

        val androidLocation = coordsResult.getOrElse { return Result.failure(it) }
        val resolvedName = resolveCityName(androidLocation.latitude, androidLocation.longitude)
        val gpsSavedLocation = SavedLocation(
            id = "gps_current",
            name = resolvedName.first,
            country = resolvedName.second,
            admin1 = "Ubicación actual",
            latitude = androidLocation.latitude,
            longitude = androidLocation.longitude,
            isCurrentGps = true,
        )
        return addLocationAndSelect(gpsSavedLocation)
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveCityName(lat: Double, lon: Double): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(appContext, Locale("es", "ES"))
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            cont.resume(list)
                        }
                    }
                } else {
                    geocoder.getFromLocation(lat, lon, 1).orEmpty()
                }
                val first = addresses.firstOrNull()
                val city = first?.locality ?: first?.subAdminArea ?: first?.adminArea ?: "Mi ubicación"
                val country = first?.countryName.orEmpty()
                city to country
            }.getOrDefault("Mi ubicación" to "")
        }
    }

    private fun mapResponseToSnapshot(
        location: SavedLocation,
        response: OpenMeteoForecastResponse,
    ): WeatherSnapshot {
        val current = response.current
        val tzId = response.timezone.ifBlank { TimeZone.getDefault().id }
        val tz = TimeZone.getTimeZone(tzId)
        val nowHourKey = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).apply {
            timeZone = tz
        }.format(Date())

        val hourlyTimes = response.hourly.time
        val startIndex = hourlyTimes.indexOfFirst { it.startsWith(nowHourKey) }.takeIf { it >= 0 } ?: 0
        val hourlyPoints = (startIndex until minOf(startIndex + 24, hourlyTimes.size)).map { idx ->
            val iso = hourlyTimes[idx]
            val code = response.hourly.weatherCode.getOrElse(idx) { current.weatherCode }
            HourlyPoint(
                isoTime = iso,
                hourLabel = if (idx == startIndex) "Ahora" else WeatherFormatter.formatHourLabel(iso),
                tempC = response.hourly.temperature2m.getOrElse(idx) { current.temperature2m },
                precipProb = response.hourly.precipitationProbability.getOrNull(idx) ?: 0,
                condition = WeatherCondition.fromWmoCode(code),
                isDay = response.hourly.isDay.getOrElse(idx) { current.isDay } == 1,
            )
        }

        val dailyTimes = response.daily.time
        val dailyPoints = dailyTimes.indices.map { idx ->
            val code = response.daily.weatherCode.getOrElse(idx) { current.weatherCode }
            DailyPoint(
                dateIso = dailyTimes[idx],
                dayLabel = WeatherFormatter.formatDayLabel(dailyTimes[idx], idx),
                condition = WeatherCondition.fromWmoCode(code),
                minTempC = response.daily.temperature2mMin.getOrElse(idx) { current.temperature2m - 4 },
                maxTempC = response.daily.temperature2mMax.getOrElse(idx) { current.temperature2m + 4 },
                precipProbMax = response.daily.precipitationProbabilityMax.getOrNull(idx) ?: 0,
                uvMax = response.daily.uvIndexMax.getOrNull(idx) ?: 0.0,
                sunriseIso = response.daily.sunrise.getOrElse(idx) { "" },
                sunsetIso = response.daily.sunset.getOrElse(idx) { "" },
            )
        }

        val today = dailyPoints.firstOrNull()
        return WeatherSnapshot(
            location = location,
            timeZoneId = tzId,
            currentTempC = current.temperature2m,
            apparentTempC = current.apparentTemperature,
            humidityPercent = current.relativeHumidity2m,
            windSpeedKmh = current.windSpeed10m,
            windDirectionDeg = current.windDirection10m,
            pressureHpa = current.surfacePressure,
            cloudCoverPercent = current.cloudCover,
            isDay = current.isDay == 1,
            condition = WeatherCondition.fromWmoCode(current.weatherCode),
            wmoCode = current.weatherCode,
            todayMinC = today?.minTempC ?: (current.temperature2m - 4),
            todayMaxC = today?.maxTempC ?: (current.temperature2m + 4),
            uvIndexMax = today?.uvMax ?: 0.0,
            sunriseIso = today?.sunriseIso.orEmpty(),
            sunsetIso = today?.sunsetIso.orEmpty(),
            hourly = hourlyPoints,
            daily = dailyPoints,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    companion object {
        @Volatile
        private var instance: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return instance ?: synchronized(this) {
                instance ?: WeatherRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
