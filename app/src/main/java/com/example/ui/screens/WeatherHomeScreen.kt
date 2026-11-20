package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyPoint
import com.example.domain.model.HourlyPoint
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherFormatter
import com.example.domain.model.WeatherSnapshot
import com.example.ui.WeatherUiState
import com.example.ui.components.AnimatedWeatherGlyph
import com.example.ui.components.HourlyTempCurveChart
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.IceCyan
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarWarmGlow
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun WeatherHomeScreen(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    onCycleCity: (Int) -> Unit,
    onRequestGps: () -> Unit,
    onToggleUnit: () -> Unit,
    onOpenLocations: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.activeSnapshot
    val scrollState = rememberScrollState()

    val spinTransition = rememberInfiniteTransition(label = "refreshSpin")
    val refreshRotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // City Selector Pill with prev/next arrows
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = GlassSurfaceDark,
                    border = BorderStroke(1.dp, GlassBorderDark),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onCycleCity(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prev_city_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Ciudad anterior",
                                tint = Color.White,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onOpenLocations)
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("city_header_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = "Ubicación",
                                tint = IceCyan,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.activeLocation.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onCycleCity(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("next_city_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Siguiente ciudad",
                                tint = Color.White,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // GPS button
                Surface(
                    shape = CircleShape,
                    color = GlassSurfaceDark,
                    border = BorderStroke(1.dp, GlassBorderDark),
                ) {
                    IconButton(
                        onClick = onRequestGps,
                        enabled = !uiState.isFetchingGps,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("gps_button"),
                    ) {
                        if (uiState.isFetchingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Usar mi ubicación",
                                tint = Color.White,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Unit Toggle °C / °F
                Surface(
                    shape = CircleShape,
                    color = GlassSurfaceDark,
                    border = BorderStroke(1.dp, GlassBorderDark),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(onClick = onToggleUnit)
                        .testTag("unit_toggle_button"),
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.temperatureUnit.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Refresh button
                Surface(
                    shape = CircleShape,
                    color = GlassSurfaceDark,
                    border = BorderStroke(1.dp, GlassBorderDark),
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_weather_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar clima",
                            tint = Color.White,
                            modifier = Modifier.rotate(if (uiState.isRefreshing) refreshRotation else 0f),
                        )
                    }
                }
            }

            // Status / Error Banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null || uiState.statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val isError = uiState.errorMessage != null
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isError) Color(0x55E53935) else Color(0x4438B6FF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismissBanner),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: uiState.statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        if (isError) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("Reintentar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (snapshot == null) {
                LoadingOrEmptyHero(
                    cityName = uiState.activeLocation.name,
                    isLoading = uiState.isRefreshing || uiState.isLoadingInitial,
                    onRetry = onRefresh,
                )
            } else {
                // Hero Atmospheric Section
                HeroWeatherSection(
                    snapshot = snapshot,
                    unit = uiState.temperatureUnit,
                    use24HourClock = uiState.use24HourClock,
                    clockTick = uiState.clockTickEpochMs,
                )

                // Hourly Forecast + Temperature Curve
                HourlyForecastSection(
                    hourly = snapshot.hourly,
                    unit = uiState.temperatureUnit,
                )

                // 7-Day Forecast
                DailyForecastSection(
                    daily = snapshot.daily,
                    unit = uiState.temperatureUnit,
                )

                // Atmospheric Bento Grid
                AtmosphericMetricsGrid(snapshot = snapshot, unit = uiState.temperatureUnit)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingOrEmptyHero(
    cityName: String,
    isLoading: Boolean,
    onRetry: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "Obteniendo clima en vivo para $cityName…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
            } else {
                Text(
                    text = "Sin datos para $cityName",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyan,
                        contentColor = Color(0xFF091326),
                    ),
                ) {
                    Text("Cargar pronóstico")
                }
            }
        }
    }
}

@Composable
private fun HeroWeatherSection(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
    use24HourClock: Boolean,
    @Suppress("UNUSED_PARAMETER") clockTick: Long,
) {
    val targetTemp = WeatherFormatter.convertTemp(snapshot.currentTempC, unit).toFloat()
    val animatedTemp by animateFloatAsState(
        targetValue = targetTemp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
        label = "heroTemp",
    )

    val tzId = snapshot.timeZoneId.ifBlank { TimeZone.getDefault().id }
    val liveTime = WeatherFormatter.formatLiveTimeForZone(tzId, use24HourClock)
    val liveDate = WeatherFormatter.formatDateForZone(tzId)
    val season = snapshot.season

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Live Local Time & Season Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassSurfaceDark,
                border = BorderStroke(1.dp, GlassBorderDark),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Hora local",
                        tint = IceCyan,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$liveTime · $liveDate",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassSurfaceDark,
                border = BorderStroke(1.dp, GlassBorderDark),
            ) {
                Text(
                    text = "${season.emoji} ${season.labelEs}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedWeatherGlyph(
                condition = snapshot.condition,
                isDay = snapshot.isDay,
                size = 86.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${animatedTemp.roundToInt()}°",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 82.sp),
                color = Color.White,
            )
        }

        Text(
            text = snapshot.conditionDescription,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))

        val feelsLike = WeatherFormatter.formatTemp(snapshot.apparentTempC, unit, includeUnit = true)
        val high = WeatherFormatter.formatTemp(snapshot.todayMaxC, unit)
        val low = WeatherFormatter.formatTemp(snapshot.todayMinC, unit)
        Text(
            text = "Sensación $feelsLike  ·  Máx $high  Mín $low",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${season.labelEs}: ${season.descriptionEs}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun HourlyForecastSection(
    hourly: List<HourlyPoint>,
    unit: TemperatureUnit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = "Pronóstico por horas",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            HourlyTempCurveChart(
                hourly = hourly,
                unit = unit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(hourly.take(18), key = { it.isoTime }) { point ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = if (point.hourLabel == "Ahora") 0.22f else 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .width(68.dp)
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = point.hourLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.86f),
                            )
                            AnimatedWeatherGlyph(
                                condition = point.condition,
                                isDay = point.isDay,
                                size = 30.dp,
                            )
                            Text(
                                text = WeatherFormatter.formatTemp(point.tempC, unit),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            if (point.precipProb > 0) {
                                Text(
                                    text = "${point.precipProb}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IceCyan,
                                )
                            } else {
                                Text(
                                    text = " ",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastSection(
    daily: List<DailyPoint>,
    unit: TemperatureUnit,
) {
    if (daily.isEmpty()) return
    val weekMin = daily.minOf { it.minTempC }
    val weekMax = daily.maxOf { it.maxTempC }
    val span = (weekMax - weekMin).coerceAtLeast(1.0)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pronóstico de 7 días",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            daily.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.width(68.dp),
                    )

                    AnimatedWeatherGlyph(
                        condition = day.condition,
                        isDay = true,
                        size = 28.dp,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (day.precipProbMax >= 15) "${day.precipProbMax}%" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = IceCyan,
                        modifier = Modifier.width(34.dp),
                    )

                    Text(
                        text = WeatherFormatter.formatTemp(day.minTempC, unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.width(34.dp),
                    )

                    // Thermal range bar
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                    ) {
                        val startFrac = ((day.minTempC - weekMin) / span).toFloat().coerceIn(0f, 1f)
                        val endFrac = ((day.maxTempC - weekMin) / span).toFloat().coerceIn(startFrac, 1f)
                        val barWidth = maxWidth * (endFrac - startFrac).coerceAtLeast(0.12f)
                        val startOffset = maxWidth * startFrac

                        Box(
                            modifier = Modifier
                                .padding(start = startOffset)
                                .width(barWidth)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IceCyan, SolarAmber, SolarWarmGlow),
                                    ),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = WeatherFormatter.formatTemp(day.maxTempC, unit),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmosphericMetricsGrid(
    snapshot: WeatherSnapshot,
    unit: TemperatureUnit,
) {
    val windDir = WeatherFormatter.formatWindDirection(snapshot.windDirectionDeg)
    val sunrise = snapshot.sunriseIso.substringAfter("T", "--:--").take(5)
    val sunset = snapshot.sunsetIso.substringAfter("T", "--:--").take(5)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricBentoCard(
                icon = Icons.Rounded.WaterDrop,
                title = "Humedad",
                value = "${snapshot.humidityPercent}%",
                subtitle = "Nubosidad ${snapshot.cloudCoverPercent}%",
                modifier = Modifier.weight(1f),
            )
            MetricBentoCard(
                icon = Icons.Rounded.Air,
                title = "Viento",
                value = "${snapshot.windSpeedKmh.roundToInt()} km/h",
                subtitle = "Dirección $windDir (${snapshot.windDirectionDeg}°)",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricBentoCard(
                icon = Icons.Rounded.WbSunny,
                title = "Índice UV máx",
                value = "${snapshot.uvIndexMax.roundToInt()}",
                subtitle = "Nivel ${WeatherFormatter.uvLabelEs(snapshot.uvIndexMax)}",
                modifier = Modifier.weight(1f),
            )
            MetricBentoCard(
                icon = Icons.Rounded.Compress,
                title = "Presión",
                value = "${snapshot.pressureHpa.roundToInt()} hPa",
                subtitle = "Sensación ${WeatherFormatter.formatTemp(snapshot.apparentTempC, unit, true)}",
                modifier = Modifier.weight(1f),
            )
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WbTwilight,
                        contentDescription = "Ciclo solar",
                        tint = SolarAmber,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ciclo solar",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = "Temporada actual: ${snapshot.season.emoji} ${snapshot.season.labelEs}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Amanecer $sunrise",
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarAmber,
                    )
                    Text(
                        text = "Atardecer $sunset",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBentoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = IceCyan,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = GlassSurfaceDark,
        border = BorderStroke(1.dp, GlassBorderDark),
        content = content,
    )
}
