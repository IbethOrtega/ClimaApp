package com.example.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.local.BrisaPreferencesState
import com.example.data.repository.WeatherRepository
import com.example.domain.model.ForecastWidgetMode
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import com.example.domain.model.WeatherSnapshot
import java.util.TimeZone

class ForecastQuickWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForecastQuickWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSyncHelper.schedulePeriodicSync(context)
    }
}

class ForecastQuickWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WeatherRepository.getInstance(context)
        val initialState = repository.store.getSnapshotState()

        provideContent {
            val revision = currentState<Preferences>()[WidgetSyncHelper.KEY_WIDGET_REVISION] ?: 0L
            val state by repository.stateFlow.collectAsState(initial = initialState)
            ForecastQuickWidgetContent(state = state, revision = revision)
        }
    }
}

@Composable
private fun ForecastQuickWidgetContent(
    state: BrisaPreferencesState,
    @Suppress("UNUSED_PARAMETER") revision: Long,
) {
    val context = LocalContext.current
    val location = state.savedLocations.firstOrNull { it.id == state.selectedLocationId }
        ?: state.savedLocations.first()
    val snapshot: WeatherSnapshot? = state.snapshots[location.id]
    val condition = snapshot?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val isDay = snapshot?.isDay ?: true
    val timeZoneId = snapshot?.timeZoneId?.ifBlank { TimeZone.getDefault().id }
        ?: TimeZone.getDefault().id

    val whiteText = ColorProvider(Color.White)
    val mutedText = ColorProvider(Color(0xD4E6F4FF))

    val compactClock = RemoteViews(context.packageName, R.layout.widget_live_clock_compact).apply {
        setString(R.id.widget_live_clock_compact, "setTimeZone", timeZoneId)
        if (state.use24HourClock) {
            setCharSequence(R.id.widget_live_clock_compact, "setFormat12Hour", "HH:mm")
            setCharSequence(R.id.widget_live_clock_compact, "setFormat24Hour", "HH:mm")
        } else {
            setCharSequence(R.id.widget_live_clock_compact, "setFormat12Hour", "h:mm a")
            setCharSequence(R.id.widget_live_clock_compact, "setFormat24Hour", "HH:mm")
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(condition.widgetBackgroundRes(isDay)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header row: City + Live Clock + Mode toggle + Refresh
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(condition.widgetIconRes(isDay)),
                    contentDescription = condition.labelEs,
                    modifier = GlanceModifier.size(26.dp),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(
                            text = location.name,
                            style = TextStyle(
                                color = whiteText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = snapshot?.let {
                                WeatherFormatter.formatTemp(it.currentTempC, state.temperatureUnit)
                            } ?: "--°",
                            style = TextStyle(
                                color = whiteText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    AndroidRemoteViews(remoteViews = compactClock)
                }

                // Interactive mode switch: Hourly vs 3 Days
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .clickable(actionRunCallback<ToggleForecastModeAction>())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.forecastWidgetMode == ForecastWidgetMode.HOURLY) "Por horas" else "3 días",
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .clickable(actionRunCallback<NextCityAction>())
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_chevron_right),
                        contentDescription = "Siguiente ciudad",
                        modifier = GlanceModifier.size(14.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .clickable(actionRunCallback<RefreshWeatherAction>())
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_refresh),
                        contentDescription = "Actualizar",
                        modifier = GlanceModifier.size(14.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Forecast strip
            if (snapshot == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Toca actualizar para cargar el pronóstico",
                        style = TextStyle(color = mutedText, fontSize = 12.sp),
                    )
                }
            } else if (state.forecastWidgetMode == ForecastWidgetMode.HOURLY) {
                val hours = snapshot.hourly.take(4)
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    hours.forEachIndexed { idx, point ->
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(ImageProvider(R.drawable.widget_pill_glass))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        ) {
                            Text(
                                text = point.hourLabel,
                                style = TextStyle(color = mutedText, fontSize = 11.sp),
                                maxLines = 1,
                            )
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            Image(
                                provider = ImageProvider(point.condition.widgetIconRes(point.isDay)),
                                contentDescription = point.condition.labelEs,
                                modifier = GlanceModifier.size(22.dp),
                            )
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            Text(
                                text = WeatherFormatter.formatTemp(point.tempC, state.temperatureUnit),
                                style = TextStyle(
                                    color = whiteText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        if (idx < hours.lastIndex) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                    }
                }
            } else {
                val days = snapshot.daily.take(3)
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    days.forEachIndexed { idx, day ->
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(ImageProvider(R.drawable.widget_pill_glass))
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        ) {
                            Text(
                                text = day.dayLabel,
                                style = TextStyle(
                                    color = whiteText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 1,
                            )
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            Image(
                                provider = ImageProvider(day.condition.widgetIconRes(true)),
                                contentDescription = day.condition.labelEs,
                                modifier = GlanceModifier.size(22.dp),
                            )
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            Text(
                                text = "${WeatherFormatter.formatTemp(day.maxTempC, state.temperatureUnit)}/${WeatherFormatter.formatTemp(day.minTempC, state.temperatureUnit)}",
                                style = TextStyle(
                                    color = whiteText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                            )
                        }
                        if (idx < days.lastIndex) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}
