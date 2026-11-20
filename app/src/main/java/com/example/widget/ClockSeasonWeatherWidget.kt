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
import androidx.glance.LocalSize
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
import androidx.glance.layout.fillMaxHeight
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
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import com.example.domain.model.WeatherSnapshot
import java.util.TimeZone

class ClockSeasonWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockSeasonWeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSyncHelper.schedulePeriodicSync(context)
    }
}

class ClockSeasonWeatherWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WeatherRepository.getInstance(context)
        val initialState = repository.store.getSnapshotState()

        provideContent {
            val revision = currentState<Preferences>()[WidgetSyncHelper.KEY_WIDGET_REVISION] ?: 0L
            val state by repository.stateFlow.collectAsState(initial = initialState)
            ClockSeasonWidgetContent(state = state, revision = revision)
        }
    }
}

@Composable
private fun ClockSeasonWidgetContent(
    state: BrisaPreferencesState,
    @Suppress("UNUSED_PARAMETER") revision: Long,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isCompactWidth = size.width < 240.dp

    val location = state.savedLocations.firstOrNull { it.id == state.selectedLocationId }
        ?: state.savedLocations.first()
    val snapshot: WeatherSnapshot? = state.snapshots[location.id]
    val condition = snapshot?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val isDay = snapshot?.isDay ?: true
    val timeZoneId = snapshot?.timeZoneId?.ifBlank { TimeZone.getDefault().id }
        ?: TimeZone.getDefault().id
    val season = WeatherFormatter.calculateSeason(location.latitude, timeZoneId)

    val whiteText = ColorProvider(Color.White)
    val mutedText = ColorProvider(Color(0xD9E6F4FF))

    val clockRemoteViews = RemoteViews(context.packageName, R.layout.widget_live_clock).apply {
        setString(R.id.widget_live_clock_text, "setTimeZone", timeZoneId)
        setString(R.id.widget_live_date_text, "setTimeZone", timeZoneId)
        if (state.use24HourClock) {
            setCharSequence(R.id.widget_live_clock_text, "setFormat12Hour", "HH:mm")
            setCharSequence(R.id.widget_live_clock_text, "setFormat24Hour", "HH:mm")
        } else {
            setCharSequence(R.id.widget_live_clock_text, "setFormat12Hour", "h:mm a")
            setCharSequence(R.id.widget_live_clock_text, "setFormat24Hour", "HH:mm")
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(condition.widgetBackgroundRes(isDay)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            // Top bar: Location + interactive city switcher + refresh + unit toggle
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_location),
                    contentDescription = "Ubicación",
                    modifier = GlanceModifier.size(14.dp),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = location.name,
                    style = TextStyle(
                        color = whiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )

                // Season badge ("Temporada")
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${season.emoji} ${season.labelEs}",
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Interactive °C / °F pill
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .clickable(actionRunCallback<ToggleUnitAction>())
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.temperatureUnit == TemperatureUnit.CELSIUS) "°C" else "°F",
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Main content row: Live Clock + Weather Temp & Controls
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                // Left: Live Clock + Date
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    AndroidRemoteViews(remoteViews = clockRemoteViews)
                    if (!isCompactWidth) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        val condText = snapshot?.conditionDescription ?: "Toca actualizar"
                        Text(
                            text = condText,
                            style = TextStyle(
                                color = mutedText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                }

                // Right: Weather Icon + Temperature + High/Low
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(condition.widgetIconRes(isDay)),
                        contentDescription = condition.labelEs,
                        modifier = GlanceModifier.size(38.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text(
                            text = snapshot?.let {
                                WeatherFormatter.formatTemp(it.currentTempC, state.temperatureUnit)
                            } ?: "--°",
                            style = TextStyle(
                                color = whiteText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        val hiLo = if (snapshot != null) {
                            "${WeatherFormatter.formatTemp(snapshot.todayMaxC, state.temperatureUnit)} / ${WeatherFormatter.formatTemp(snapshot.todayMinC, state.temperatureUnit)}"
                        } else {
                            "-- / --"
                        }
                        Text(
                            text = hiLo,
                            style = TextStyle(
                                color = mutedText,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Bottom interactive control bar: Switch City + Refresh
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_chevron_left),
                        contentDescription = "Ciudad anterior",
                        modifier = GlanceModifier
                            .size(18.dp)
                            .clickable(actionRunCallback<PreviousCityAction>()),
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Cambiar ciudad",
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_chevron_right),
                        contentDescription = "Siguiente ciudad",
                        modifier = GlanceModifier
                            .size(18.dp)
                            .clickable(actionRunCallback<NextCityAction>()),
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_glass))
                        .clickable(actionRunCallback<RefreshWeatherAction>())
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_refresh),
                        contentDescription = "Actualizar clima",
                        modifier = GlanceModifier.size(14.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Actualizar",
                        style = TextStyle(
                            color = whiteText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
