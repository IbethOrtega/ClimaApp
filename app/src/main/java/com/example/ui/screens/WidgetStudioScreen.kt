package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ForecastWidgetMode
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import com.example.ui.WeatherUiState
import com.example.ui.components.AnimatedWeatherGlyph
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.IceCyan
import com.example.widget.ClockSeasonWeatherWidgetReceiver
import com.example.widget.ForecastQuickWidgetReceiver
import java.util.TimeZone

@Composable
fun WidgetStudioScreen(
    uiState: WeatherUiState,
    onCycleCity: (Int) -> Unit,
    onToggleUnit: () -> Unit,
    onToggleClockFormat: () -> Unit,
    onToggleForecastMode: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snapshot = uiState.activeSnapshot
    val condition = snapshot?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val isDay = snapshot?.isDay ?: true
    val tzId = snapshot?.timeZoneId?.ifBlank { TimeZone.getDefault().id } ?: TimeZone.getDefault().id
    val season = WeatherFormatter.calculateSeason(uiState.activeLocation.latitude, tzId)
    val liveTime = WeatherFormatter.formatLiveTimeForZone(tzId, uiState.use24HourClock)
    val liveDate = WeatherFormatter.formatDateForZone(tzId)

    val widgetGradient = when {
        !isDay && condition in listOf(WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY) ->
            listOf(Color(0xFF1F3666), Color(0xFF0A1226))
        condition in listOf(WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.STORM) ->
            listOf(Color(0xFF2D5073), Color(0xFF162436))
        condition in listOf(WeatherCondition.CLOUDY, WeatherCondition.FOG) ->
            listOf(Color(0xFF546E8A), Color(0xFF2A3B4F))
        condition == WeatherCondition.SNOW ->
            listOf(Color(0xFF5F8FB8), Color(0xFF2D4C6E))
        else ->
            listOf(Color(0xFF3BA0F2), Color(0xFF1B5DA6))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = "Estudio de widgets",
                    tint = IceCyan,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Widgets interactivos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "Prueba sus controles aquí o añádelos directo a tu pantalla de inicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
            }

            // Widget 1: Clock + Season + Location + Weather
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "1. Reloj, Clima y Temporada",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Muestra la hora exacta de la ubicación en vivo, la temporada del año (${season.labelEs}) y controles rápidos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )

                    // Live Interactive Preview of Widget 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(widgetGradient))
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Ubicación",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.activeLocation.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                WidgetGlassPill(text = "${season.emoji} ${season.labelEs}")
                                Spacer(modifier = Modifier.width(6.dp))
                                WidgetGlassPill(
                                    text = uiState.temperatureUnit.symbol,
                                    onClick = onToggleUnit,
                                    modifier = Modifier.testTag("studio_widget1_unit"),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = liveTime,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "$liveDate · ${snapshot?.conditionDescription ?: "Clima"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedWeatherGlyph(
                                        condition = condition,
                                        isDay = isDay,
                                        size = 42.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = snapshot?.let {
                                                WeatherFormatter.formatTemp(it.currentTempC, uiState.temperatureUnit)
                                            } ?: "--°",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                                            color = Color.White,
                                        )
                                        Text(
                                            text = if (snapshot != null) {
                                                "${WeatherFormatter.formatTemp(snapshot.todayMaxC, uiState.temperatureUnit)} / ${WeatherFormatter.formatTemp(snapshot.todayMinC, uiState.temperatureUnit)}"
                                            } else "-- / --",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = "Anterior",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onCycleCity(-1) },
                                        )
                                        Text(
                                            text = "Cambiar ciudad",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = "Siguiente",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onCycleCity(1) },
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(onClick = onRefresh),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Actualizar",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Actualizar",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            requestPinWidget(
                                context = context,
                                receiverClass = ClockSeasonWeatherWidgetReceiver::class.java,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyan,
                            contentColor = Color(0xFF091326),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pin_clock_season_widget"),
                    ) {
                        Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir widget Hora y Temporada", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Widget 2: Interactive Forecast Widget
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "2. Pronóstico Interactivo",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Alterna directamente en el widget entre pronóstico por horas y los próximos 3 días.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(widgetGradient))
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnimatedWeatherGlyph(condition = condition, isDay = isDay, size = 28.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${uiState.activeLocation.name} ${snapshot?.let { WeatherFormatter.formatTemp(it.currentTempC, uiState.temperatureUnit) } ?: "--°"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = liveTime,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.84f),
                                    )
                                }
                                WidgetGlassPill(
                                    text = if (uiState.forecastWidgetMode == ForecastWidgetMode.HOURLY) "Por horas" else "3 días",
                                    onClick = onToggleForecastMode,
                                    modifier = Modifier.testTag("studio_toggle_forecast_mode"),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (snapshot == null) {
                                    Text(
                                        text = "Cargando pronóstico…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                } else if (uiState.forecastWidgetMode == ForecastWidgetMode.HOURLY) {
                                    snapshot.hourly.take(4).forEach { hr ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White.copy(alpha = 0.16f),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(hr.hourLabel, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AnimatedWeatherGlyph(condition = hr.condition, isDay = hr.isDay, size = 24.dp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    WeatherFormatter.formatTemp(hr.tempC, uiState.temperatureUnit),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    snapshot.daily.take(3).forEach { day ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White.copy(alpha = 0.16f),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(day.dayLabel, style = MaterialTheme.typography.labelMedium, color = Color.White)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AnimatedWeatherGlyph(condition = day.condition, isDay = true, size = 24.dp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "${WeatherFormatter.formatTemp(day.maxTempC, uiState.temperatureUnit)}/${WeatherFormatter.formatTemp(day.minTempC, uiState.temperatureUnit)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            requestPinWidget(
                                context = context,
                                receiverClass = ForecastQuickWidgetReceiver::class.java,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.20f),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pin_forecast_widget"),
                    ) {
                        Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir widget Pronóstico", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Widget Settings
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Preferencias de widgets",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reloj en formato 24 horas", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Sincroniza los widgets con formato 24h o 12h AM/PM", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))
                        }
                        Switch(
                            checked = uiState.use24HourClock,
                            onCheckedChange = { onToggleClockFormat() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF091326), checkedTrackColor = IceCyan),
                            modifier = Modifier.testTag("switch_24h_clock"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Usar grados Fahrenheit (°F)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Cambia entre °C y °F en app y widgets", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))
                        }
                        Switch(
                            checked = uiState.temperatureUnit == TemperatureUnit.FAHRENHEIT,
                            onCheckedChange = { onToggleUnit() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF091326), checkedTrackColor = IceCyan),
                            modifier = Modifier.testTag("switch_fahrenheit"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WidgetGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = if (onClick != null) {
            modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        } else {
            modifier
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

private fun requestPinWidget(context: Context, receiverClass: Class<*>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(context, receiverClass)
        if (manager != null && manager.isRequestPinAppWidgetSupported) {
            val requested = manager.requestPinAppWidget(provider, null, null)
            if (requested) return
        }
    }
    Toast.makeText(
        context,
        "Mantén presionada la pantalla de inicio y selecciona Widgets → Brisa.",
        Toast.LENGTH_LONG,
    ).show()
}
