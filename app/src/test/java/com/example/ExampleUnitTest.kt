package com.example

import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `temperature conversion celsius and fahrenheit`() {
        assertEquals("20°C", WeatherFormatter.formatTemp(20.0, TemperatureUnit.CELSIUS, includeUnit = true))
        assertEquals("68°F", WeatherFormatter.formatTemp(20.0, TemperatureUnit.FAHRENHEIT, includeUnit = true))
        assertEquals("32°", WeatherFormatter.formatTemp(0.0, TemperatureUnit.FAHRENHEIT, includeUnit = false))
    }

    @Test
    fun `hemispheres compute opposite seasons`() {
        val north = WeatherFormatter.calculateSeason(latitude = 19.43, timeZoneId = "America/Mexico_City")
        val south = WeatherFormatter.calculateSeason(latitude = -34.60, timeZoneId = "America/Argentina/Buenos_Aires")
        assertNotEquals(north, south)
    }

    @Test
    fun `wmo codes map to expected weather conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(63))
        assertEquals(WeatherCondition.STORM, WeatherCondition.fromWmoCode(95))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(73))
    }
}
