package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WeatherPreferencesStore
import com.example.data.repository.WeatherRepository
import com.example.domain.model.ForecastWidgetMode
import com.example.domain.model.SavedLocation
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherSnapshot
import com.example.widget.WidgetSyncHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WeatherUiState(
    val savedLocations: List<SavedLocation> = WeatherPreferencesStore.DEFAULT_LOCATIONS,
    val selectedLocationId: String = WeatherPreferencesStore.DEFAULT_LOCATIONS.first().id,
    val snapshots: Map<String, WeatherSnapshot> = emptyMap(),
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val forecastWidgetMode: ForecastWidgetMode = ForecastWidgetMode.HOURLY,
    val use24HourClock: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingInitial: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val searchResults: List<SavedLocation> = emptyList(),
    val isSearching: Boolean = false,
    val isFetchingGps: Boolean = false,
    val statusBanner: String? = null,
    val clockTickEpochMs: Long = System.currentTimeMillis(),
) {
    val activeLocation: SavedLocation
        get() = savedLocations.firstOrNull { it.id == selectedLocationId }
            ?: savedLocations.first()

    val activeSnapshot: WeatherSnapshot?
        get() = snapshots[activeLocation.id]
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository.getInstance(application)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        WidgetSyncHelper.schedulePeriodicSync(application)
        observePreferences()
        startClockTicker()
        refreshAllSavedLocationsOnLaunch()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.stateFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        savedLocations = prefs.savedLocations,
                        selectedLocationId = prefs.selectedLocationId,
                        snapshots = prefs.snapshots,
                        temperatureUnit = prefs.temperatureUnit,
                        forecastWidgetMode = prefs.forecastWidgetMode,
                        use24HourClock = prefs.use24HourClock,
                        isLoadingInitial = prefs.snapshots[prefs.selectedLocationId] == null && current.isRefreshing,
                    )
                }
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(clockTickEpochMs = System.currentTimeMillis()) }
                delay(15_000L)
            }
        }
    }

    private fun refreshAllSavedLocationsOnLaunch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = repository.refreshActiveLocation(syncWidgets = true)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No se pudo actualizar el clima. Verifica tu conexión.",
                    )
                }
            }
            _uiState.update { it.copy(isRefreshing = false, isLoadingInitial = false) }

            // Background warm-up for other saved locations so widget switching is instant
            val currentLocations = _uiState.value.savedLocations
            val selectedId = _uiState.value.selectedLocationId
            currentLocations.filter { it.id != selectedId }.forEach { loc ->
                repository.refreshWeather(loc, syncWidgets = false)
            }
        }
    }

    fun refreshActiveWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = repository.refreshActiveLocation(syncWidgets = true)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoadingInitial = false,
                    errorMessage = if (result.isFailure) {
                        "No se pudo conectar con el servicio meteorológico."
                    } else null,
                )
            }
        }
    }

    fun selectLocation(locationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedLocationId = locationId, isRefreshing = true, errorMessage = null) }
            val result = repository.selectLocationAndRefresh(locationId)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    errorMessage = if (result.isFailure && it.snapshots[locationId] == null) {
                        "Error al obtener el pronóstico de esta ciudad."
                    } else null,
                )
            }
        }
    }

    fun cycleLocation(step: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.cycleWidgetLocation(step)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350L)
            _uiState.update { it.copy(isSearching = true) }
            val result = repository.searchCities(query)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = result.getOrDefault(emptyList()),
                    errorMessage = if (result.isFailure) "Error al buscar ciudades." else it.errorMessage,
                )
            }
        }
    }

    fun addAndSelectCity(location: SavedLocation) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    searchQuery = "",
                    searchResults = emptyList(),
                    errorMessage = null,
                )
            }
            val result = repository.addLocationAndSelect(location)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    statusBanner = "${location.name} añadida",
                    errorMessage = if (result.isFailure) "Ciudad guardada, pero falló la descarga del clima." else null,
                )
            }
        }
    }

    fun removeLocation(locationId: String) {
        viewModelScope.launch {
            repository.removeLocation(locationId)
        }
    }

    fun fetchCurrentGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingGps = true, errorMessage = null) }
            val result = repository.fetchCurrentGpsLocationAndWeather()
            _uiState.update {
                it.copy(
                    isFetchingGps = false,
                    statusBanner = if (result.isSuccess) "Ubicación actual actualizada" else null,
                    errorMessage = if (result.isFailure) {
                        "No pudimos obtener tu ubicación GPS. Intenta buscar tu ciudad manualmente."
                    } else null,
                )
            }
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            repository.toggleTemperatureUnit()
        }
    }

    fun toggleClockFormat() {
        viewModelScope.launch {
            val next = !_uiState.value.use24HourClock
            repository.setUse24HourClock(next)
        }
    }

    fun toggleForecastWidgetMode() {
        viewModelScope.launch {
            repository.toggleForecastWidgetMode()
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null, errorMessage = null) }
    }
}
