package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ForecastWidgetMode
import com.example.domain.model.SavedLocation
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.brisaDataStore: DataStore<Preferences> by preferencesDataStore(name = "brisa_weather_prefs")

data class BrisaPreferencesState(
    val savedLocations: List<SavedLocation>,
    val selectedLocationId: String,
    val snapshots: Map<String, WeatherSnapshot>,
    val temperatureUnit: TemperatureUnit,
    val forecastWidgetMode: ForecastWidgetMode,
    val use24HourClock: Boolean,
)

class WeatherPreferencesStore(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val locationListType = Types.newParameterizedType(List::class.java, SavedLocation::class.java)
    private val locationListAdapter = moshi.adapter<List<SavedLocation>>(locationListType)

    private val snapshotMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        WeatherSnapshot::class.java,
    )
    private val snapshotMapAdapter = moshi.adapter<Map<String, WeatherSnapshot>>(snapshotMapType)

    val stateFlow: Flow<BrisaPreferencesState> = context.brisaDataStore.data.map { prefs ->
        val locations = parseLocations(prefs[KEY_LOCATIONS])
        val selectedId = prefs[KEY_SELECTED_ID]?.takeIf { id -> locations.any { it.id == id } }
            ?: locations.first().id
        val snapshots = parseSnapshots(prefs[KEY_SNAPSHOTS])
        val unit = runCatching {
            TemperatureUnit.valueOf(prefs[KEY_TEMP_UNIT] ?: TemperatureUnit.CELSIUS.name)
        }.getOrDefault(TemperatureUnit.CELSIUS)
        val widgetMode = runCatching {
            ForecastWidgetMode.valueOf(prefs[KEY_FORECAST_WIDGET_MODE] ?: ForecastWidgetMode.HOURLY.name)
        }.getOrDefault(ForecastWidgetMode.HOURLY)
        val use24h = prefs[KEY_USE_24H_CLOCK] ?: false

        BrisaPreferencesState(
            savedLocations = locations,
            selectedLocationId = selectedId,
            snapshots = snapshots,
            temperatureUnit = unit,
            forecastWidgetMode = widgetMode,
            use24HourClock = use24h,
        )
    }

    suspend fun getSnapshotState(): BrisaPreferencesState = stateFlow.first()

    suspend fun setSelectedLocation(locationId: String) {
        context.brisaDataStore.edit { prefs ->
            prefs[KEY_SELECTED_ID] = locationId
        }
    }

    suspend fun addOrUpdateLocation(location: SavedLocation, selectImmediately: Boolean = true) {
        context.brisaDataStore.edit { prefs ->
            val current = parseLocations(prefs[KEY_LOCATIONS]).toMutableList()
            val existingIndex = current.indexOfFirst {
                it.id == location.id || (location.isCurrentGps && it.isCurrentGps)
            }
            if (existingIndex >= 0) {
                current[existingIndex] = location
            } else {
                if (location.isCurrentGps) {
                    current.add(0, location)
                } else {
                    current.add(location)
                }
            }
            prefs[KEY_LOCATIONS] = locationListAdapter.toJson(current)
            if (selectImmediately) {
                prefs[KEY_SELECTED_ID] = location.id
            }
        }
    }

    suspend fun removeLocation(locationId: String) {
        context.brisaDataStore.edit { prefs ->
            val current = parseLocations(prefs[KEY_LOCATIONS]).filterNot { it.id == locationId }
            val safeList = current.ifEmpty { DEFAULT_LOCATIONS }
            prefs[KEY_LOCATIONS] = locationListAdapter.toJson(safeList)
            if (prefs[KEY_SELECTED_ID] == locationId) {
                prefs[KEY_SELECTED_ID] = safeList.first().id
            }
            val snapshots = parseSnapshots(prefs[KEY_SNAPSHOTS]).toMutableMap()
            snapshots.remove(locationId)
            prefs[KEY_SNAPSHOTS] = snapshotMapAdapter.toJson(snapshots)
        }
    }

    suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) {
        context.brisaDataStore.edit { prefs ->
            val current = parseSnapshots(prefs[KEY_SNAPSHOTS]).toMutableMap()
            current[snapshot.location.id] = snapshot
            prefs[KEY_SNAPSHOTS] = snapshotMapAdapter.toJson(current)
        }
    }

    suspend fun toggleTemperatureUnit(): TemperatureUnit {
        var next = TemperatureUnit.CELSIUS
        context.brisaDataStore.edit { prefs ->
            val current = runCatching {
                TemperatureUnit.valueOf(prefs[KEY_TEMP_UNIT] ?: TemperatureUnit.CELSIUS.name)
            }.getOrDefault(TemperatureUnit.CELSIUS)
            next = if (current == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
            prefs[KEY_TEMP_UNIT] = next.name
        }
        return next
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.brisaDataStore.edit { prefs ->
            prefs[KEY_TEMP_UNIT] = unit.name
        }
    }

    suspend fun toggleForecastWidgetMode(): ForecastWidgetMode {
        var next = ForecastWidgetMode.HOURLY
        context.brisaDataStore.edit { prefs ->
            val current = runCatching {
                ForecastWidgetMode.valueOf(prefs[KEY_FORECAST_WIDGET_MODE] ?: ForecastWidgetMode.HOURLY.name)
            }.getOrDefault(ForecastWidgetMode.HOURLY)
            next = if (current == ForecastWidgetMode.HOURLY) ForecastWidgetMode.DAILY else ForecastWidgetMode.HOURLY
            prefs[KEY_FORECAST_WIDGET_MODE] = next.name
        }
        return next
    }

    suspend fun setUse24HourClock(use24h: Boolean) {
        context.brisaDataStore.edit { prefs ->
            prefs[KEY_USE_24H_CLOCK] = use24h
        }
    }

    suspend fun cycleLocation(step: Int): SavedLocation {
        var selected = DEFAULT_LOCATIONS.first()
        context.brisaDataStore.edit { prefs ->
            val locations = parseLocations(prefs[KEY_LOCATIONS])
            val currentId = prefs[KEY_SELECTED_ID] ?: locations.first().id
            val currentIndex = locations.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val nextIndex = ((currentIndex + step) % locations.size + locations.size) % locations.size
            selected = locations[nextIndex]
            prefs[KEY_SELECTED_ID] = selected.id
        }
        return selected
    }

    private fun parseLocations(json: String?): List<SavedLocation> {
        if (json.isNullOrBlank()) return DEFAULT_LOCATIONS
        return runCatching {
            locationListAdapter.fromJson(json)?.takeIf { it.isNotEmpty() } ?: DEFAULT_LOCATIONS
        }.getOrDefault(DEFAULT_LOCATIONS)
    }

    private fun parseSnapshots(json: String?): Map<String, WeatherSnapshot> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            snapshotMapAdapter.fromJson(json) ?: emptyMap()
        }.getOrDefault(emptyMap())
    }

    companion object {
        private val KEY_LOCATIONS = stringPreferencesKey("saved_locations")
        private val KEY_SELECTED_ID = stringPreferencesKey("selected_location_id")
        private val KEY_SNAPSHOTS = stringPreferencesKey("weather_snapshots")
        private val KEY_TEMP_UNIT = stringPreferencesKey("temperature_unit")
        private val KEY_FORECAST_WIDGET_MODE = stringPreferencesKey("forecast_widget_mode")
        private val KEY_USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")

        val DEFAULT_LOCATIONS = listOf(
            SavedLocation(
                id = "cdmx",
                name = "Ciudad de México",
                country = "México",
                admin1 = "CDMX",
                latitude = 19.4326,
                longitude = -99.1332,
            ),
            SavedLocation(
                id = "madrid",
                name = "Madrid",
                country = "España",
                admin1 = "Comunidad de Madrid",
                latitude = 40.4168,
                longitude = -3.7038,
            ),
            SavedLocation(
                id = "buenos_aires",
                name = "Buenos Aires",
                country = "Argentina",
                admin1 = "CABA",
                latitude = -34.6037,
                longitude = -58.3816,
            ),
        )
    }
}
