package com.example.domain.model

import androidx.annotation.DrawableRes
import com.example.R
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class SavedLocation(
    val id: String,
    val name: String,
    val country: String,
    val admin1: String? = null,
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean = false,
) {
    val displaySubtitle: String
        get() = listOfNotNull(admin1?.takeIf { it.isNotBlank() && it != name }, country.takeIf { it.isNotBlank() })
            .joinToString(", ")
}

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

enum class ForecastWidgetMode {
    HOURLY,
    DAILY,
}

enum class Season(
    val labelEs: String,
    val emoji: String,
    val descriptionEs: String,
) {
    SPRING("Primavera", "🌸", "Días templados y cielo cambiante"),
    SUMMER("Verano", "☀️", "Mayor radiación solar y días largos"),
    AUTUMN("Otoño", "🍂", "Brisas frescas y tardes doradas"),
    WINTER("Invierno", "❄️", "Aire frío y noches más largas"),
}

enum class WeatherCondition(
    val labelEs: String,
) {
    CLEAR("Despejado"),
    PARTLY_CLOUDY("Parcialmente nublado"),
    CLOUDY("Nublado"),
    FOG("Neblina"),
    DRIZZLE("Llovizna"),
    RAIN("Lluvia"),
    STORM("Tormenta"),
    SNOW("Nieve");

    @DrawableRes
    fun widgetIconRes(isDay: Boolean): Int = when (this) {
        CLEAR -> if (isDay) R.drawable.ic_widget_sun else R.drawable.ic_widget_moon
        PARTLY_CLOUDY -> if (isDay) R.drawable.ic_widget_partly_cloudy else R.drawable.ic_widget_cloud
        CLOUDY, FOG -> R.drawable.ic_widget_cloud
        DRIZZLE, RAIN -> R.drawable.ic_widget_rain
        STORM -> R.drawable.ic_widget_storm
        SNOW -> R.drawable.ic_widget_snow
    }

    @DrawableRes
    fun widgetBackgroundRes(isDay: Boolean): Int = when (this) {
        CLEAR, PARTLY_CLOUDY -> if (isDay) R.drawable.widget_bg_sunny else R.drawable.widget_bg_night
        CLOUDY, FOG -> if (isDay) R.drawable.widget_bg_cloudy else R.drawable.widget_bg_night
        DRIZZLE, RAIN, STORM -> R.drawable.widget_bg_rain
        SNOW -> R.drawable.widget_bg_snow
    }

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> STORM
            else -> PARTLY_CLOUDY
        }

        fun wmoDescriptionEs(code: Int): String = when (code) {
            0 -> "Cielo despejado"
            1 -> "Mayormente despejado"
            2 -> "Parcialmente nublado"
            3 -> "Cielo cubierto"
            45, 48 -> "Niebla y neblina"
            51, 53 -> "Llovizna ligera"
            55, 56, 57 -> "Llovizna intensa"
            61 -> "Lluvia ligera"
            63 -> "Lluvia moderada"
            65, 66, 67 -> "Lluvia fuerte"
            71, 73, 77 -> "Nevada ligera"
            75, 85, 86 -> "Nevada intensa"
            80, 81 -> "Chubascos aislados"
            82 -> "Chubascos fuertes"
            95 -> "Tormenta eléctrica"
            96, 99 -> "Tormenta con granizo"
            else -> "Clima variable"
        }
    }
}

@JsonClass(generateAdapter = true)
data class HourlyPoint(
    val isoTime: String,
    val hourLabel: String,
    val tempC: Double,
    val precipProb: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

@JsonClass(generateAdapter = true)
data class DailyPoint(
    val dateIso: String,
    val dayLabel: String,
    val condition: WeatherCondition,
    val minTempC: Double,
    val maxTempC: Double,
    val precipProbMax: Int,
    val uvMax: Double,
    val sunriseIso: String,
    val sunsetIso: String,
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val location: SavedLocation,
    val timeZoneId: String,
    val currentTempC: Double,
    val apparentTempC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val pressureHpa: Double,
    val cloudCoverPercent: Int,
    val isDay: Boolean,
    val condition: WeatherCondition,
    val wmoCode: Int,
    val todayMinC: Double,
    val todayMaxC: Double,
    val uvIndexMax: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
    val updatedAtEpochMs: Long,
) {
    val conditionDescription: String
        get() = WeatherCondition.wmoDescriptionEs(wmoCode)

    val season: Season
        get() = WeatherFormatter.calculateSeason(location.latitude, timeZoneId)
}

object WeatherFormatter {
    fun convertTemp(tempC: Double, unit: TemperatureUnit): Int = when (unit) {
        TemperatureUnit.CELSIUS -> tempC.roundToInt()
        TemperatureUnit.FAHRENHEIT -> ((tempC * 9.0 / 5.0) + 32.0).roundToInt()
    }

    fun formatTemp(tempC: Double, unit: TemperatureUnit, includeUnit: Boolean = false): String {
        val value = convertTemp(tempC, unit)
        return if (includeUnit) "$value${unit.symbol}" else "$value°"
    }

    fun calculateSeason(latitude: Double, timeZoneId: String): Season {
        val tz = TimeZone.getTimeZone(timeZoneId.ifBlank { TimeZone.getDefault().id })
        val cal = Calendar.getInstance(tz)
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mmdd = month * 100 + day

        val northSeason = when {
            mmdd in 320..620 -> Season.SPRING
            mmdd in 621..922 -> Season.SUMMER
            mmdd in 923..1220 -> Season.AUTUMN
            else -> Season.WINTER
        }
        return if (latitude >= 0.0) {
            northSeason
        } else {
            when (northSeason) {
                Season.SPRING -> Season.AUTUMN
                Season.SUMMER -> Season.WINTER
                Season.AUTUMN -> Season.SPRING
                Season.WINTER -> Season.SUMMER
            }
        }
    }

    fun formatLiveTimeForZone(timeZoneId: String, use24Hour: Boolean): String {
        val tz = TimeZone.getTimeZone(timeZoneId.ifBlank { TimeZone.getDefault().id })
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale("es", "ES")).apply {
            timeZone = tz
        }.format(Date())
    }

    fun formatDateForZone(timeZoneId: String): String {
        val tz = TimeZone.getTimeZone(timeZoneId.ifBlank { TimeZone.getDefault().id })
        val raw = SimpleDateFormat("EEE, d MMM", Locale("es", "ES")).apply {
            timeZone = tz
        }.format(Date())
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    fun formatWindDirection(degrees: Int): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        val index = (((degrees % 360) + 360) % 360 / 45.0).roundToInt() % 8
        return dirs[index]
    }

    fun formatHourLabel(isoTime: String): String {
        // Expects "YYYY-MM-DDTHH:MM"
        return isoTime.substringAfter("T", isoTime).take(5)
    }

    fun formatDayLabel(dateIso: String, index: Int): String {
        if (index == 0) return "Hoy"
        if (index == 1) return "Mañana"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateIso) ?: return dateIso
            val formatted = SimpleDateFormat("EEE", Locale("es", "ES")).format(date)
            formatted.replace(".", "").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
            }
        } catch (_: Exception) {
            dateIso
        }
    }

    fun uvLabelEs(uv: Double): String = when {
        uv < 3.0 -> "Bajo"
        uv < 6.0 -> "Moderado"
        uv < 8.0 -> "Alto"
        uv < 11.0 -> "Muy alto"
        else -> "Extremo"
    }
}
