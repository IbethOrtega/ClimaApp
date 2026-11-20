package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.HourlyPoint
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherFormatter
import com.example.ui.theme.ArcticIce
import com.example.ui.theme.AzureSky
import com.example.ui.theme.BrightSkyTop
import com.example.ui.theme.DeepSkyNight
import com.example.ui.theme.MistBlue
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StormSlate
import com.example.ui.theme.TwilightCobalt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class RainDropSeed(val xFrac: Float, val yOffset: Float, val speed: Float, val length: Float)
private data class StarSeed(val xFrac: Float, val yFrac: Float, val radius: Float, val phase: Float)
private data class SnowSeed(val xFrac: Float, val yOffset: Float, val radius: Float, val swayPhase: Float, val speed: Float)

@Composable
fun AnimatedSkyBackground(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetTop = when {
        !isDay && condition in listOf(WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY) -> Color(0xFF070F22)
        condition == WeatherCondition.STORM || condition == WeatherCondition.RAIN -> Color(0xFF152436)
        condition == WeatherCondition.CLOUDY || condition == WeatherCondition.FOG -> if (isDay) Color(0xFF3E5670) else Color(0xFF1A2636)
        condition == WeatherCondition.SNOW -> if (isDay) Color(0xFF446F99) else Color(0xFF192B42)
        else -> BrightSkyTop
    }

    val targetMid = when {
        !isDay && condition in listOf(WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY) -> Color(0xFF112244)
        condition == WeatherCondition.STORM || condition == WeatherCondition.RAIN -> StormSlate
        condition == WeatherCondition.CLOUDY || condition == WeatherCondition.FOG -> if (isDay) MistBlue else Color(0xFF131D2B)
        condition == WeatherCondition.SNOW -> if (isDay) ArcticIce else Color(0xFF132238)
        else -> AzureSky
    }

    val targetBottom = when {
        !isDay -> DeepSkyNight
        condition == WeatherCondition.STORM || condition == WeatherCondition.RAIN -> Color(0xFF0E1824)
        condition == WeatherCondition.CLOUDY || condition == WeatherCondition.FOG -> Color(0xFF233245)
        condition == WeatherCondition.SNOW -> Color(0xFF23405E)
        else -> TwilightCobalt
    }

    val topColor by animateColorAsState(targetTop, tween(900), label = "skyTop")
    val midColor by animateColorAsState(targetMid, tween(900), label = "skyMid")
    val bottomColor by animateColorAsState(targetBottom, tween(900), label = "skyBottom")

    val infiniteTransition = rememberInfiniteTransition(label = "skyAnim")
    val linearCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "linearCycle",
    )
    val fastCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "fastCycle",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(3800), RepeatMode.Reverse),
        label = "pulse",
    )

    val rainSeeds = remember {
        List(44) { i ->
            RainDropSeed(
                xFrac = ((i * 37 + 13) % 100) / 100f,
                yOffset = ((i * 53 + 7) % 100) / 100f,
                speed = 1.0f + (i % 4) * 0.28f,
                length = 18f + (i % 5) * 6f,
            )
        }
    }
    val starSeeds = remember {
        List(42) { i ->
            StarSeed(
                xFrac = ((i * 61 + 17) % 100) / 100f,
                yFrac = ((i * 29 + 5) % 58) / 100f,
                radius = 1.6f + (i % 3) * 1.1f,
                phase = (i * 0.45f),
            )
        }
    }
    val snowSeeds = remember {
        List(36) { i ->
            SnowSeed(
                xFrac = ((i * 43 + 11) % 100) / 100f,
                yOffset = ((i * 31 + 19) % 100) / 100f,
                radius = 2.5f + (i % 4) * 1.4f,
                swayPhase = i * 0.7f,
                speed = 0.7f + (i % 3) * 0.25f,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, midColor, bottomColor))),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Night stars
            if (!isDay) {
                starSeeds.forEach { star ->
                    val twinkle = (0.35f + 0.65f * ((sin(linearCycle * 2f * PI.toFloat() * 3f + star.phase) + 1f) / 2f))
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle * 0.78f),
                        radius = star.radius,
                        center = Offset(w * star.xFrac, h * star.yFrac),
                    )
                }
            }

            // Sun or Moon glow in upper right sky
            if (condition in listOf(WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY)) {
                val celestialCenter = Offset(w * 0.82f, h * 0.14f)
                if (isDay) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SolarAmber.copy(alpha = 0.55f),
                                SolarAmber.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                            center = celestialCenter,
                            radius = w * 0.42f * pulse,
                        ),
                        radius = w * 0.42f * pulse,
                        center = celestialCenter,
                    )
                    rotate(degrees = linearCycle * 360f, pivot = celestialCenter) {
                        for (ray in 0 until 12) {
                            val angle = ray * 30.0 * PI / 180.0
                            val start = Offset(
                                celestialCenter.x + cos(angle).toFloat() * 44f,
                                celestialCenter.y + sin(angle).toFloat() * 44f,
                            )
                            val end = Offset(
                                celestialCenter.x + cos(angle).toFloat() * 74f,
                                celestialCenter.y + sin(angle).toFloat() * 74f,
                            )
                            drawLine(
                                color = SolarAmber.copy(alpha = 0.38f),
                                start = start,
                                end = end,
                                strokeWidth = 4f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    drawCircle(
                        color = SolarAmber.copy(alpha = 0.9f),
                        radius = 34f,
                        center = celestialCenter,
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x55DCEBFF), Color.Transparent),
                            center = celestialCenter,
                            radius = w * 0.28f * pulse,
                        ),
                        radius = w * 0.28f * pulse,
                        center = celestialCenter,
                    )
                    drawCircle(
                        color = Color(0xFFE9F2FF),
                        radius = 28f,
                        center = celestialCenter,
                    )
                }
            }

            // Drifting translucent clouds
            if (condition != WeatherCondition.CLEAR) {
                val cloudAlpha = when (condition) {
                    WeatherCondition.PARTLY_CLOUDY -> 0.16f
                    WeatherCondition.CLOUDY, WeatherCondition.FOG -> 0.26f
                    else -> 0.22f
                }
                drawDriftingCloud(
                    centerX = ((w * 1.4f) * linearCycle) - w * 0.2f,
                    centerY = h * 0.13f,
                    scale = 1.25f,
                    alpha = cloudAlpha,
                )
                drawDriftingCloud(
                    centerX = ((w * 1.5f) * ((linearCycle + 0.52f) % 1f)) - w * 0.25f,
                    centerY = h * 0.22f,
                    scale = 0.9f,
                    alpha = cloudAlpha * 0.85f,
                )
            }

            // Rain & Storm
            if (condition in listOf(WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.STORM)) {
                if (condition == WeatherCondition.STORM) {
                    val flashPhase = (fastCycle * 5f) % 5f
                    if (flashPhase < 0.22f) {
                        drawRect(color = Color.White.copy(alpha = 0.14f))
                    }
                }
                val dropColor = Color(0xFF9CD8FF).copy(
                    alpha = if (condition == WeatherCondition.DRIZZLE) 0.35f else 0.55f,
                )
                rainSeeds.forEach { drop ->
                    val progress = (fastCycle * drop.speed + drop.yOffset) % 1f
                    val y = progress * h
                    val x = drop.xFrac * w - progress * 45f
                    drawLine(
                        color = dropColor,
                        start = Offset(x, y),
                        end = Offset(x - 6f, y + drop.length),
                        strokeWidth = if (condition == WeatherCondition.DRIZZLE) 1.8f else 2.6f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Snow
            if (condition == WeatherCondition.SNOW) {
                snowSeeds.forEach { flake ->
                    val progress = (linearCycle * flake.speed * 2.5f + flake.yOffset) % 1f
                    val y = progress * h
                    val sway = sin(progress * 6f * PI.toFloat() + flake.swayPhase) * 24f
                    val x = flake.xFrac * w + sway
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = flake.radius,
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDriftingCloud(
    centerX: Float,
    centerY: Float,
    scale: Float,
    alpha: Float,
) {
    val c = Color.White.copy(alpha = alpha)
    drawCircle(color = c, radius = 48f * scale, center = Offset(centerX, centerY))
    drawCircle(color = c, radius = 38f * scale, center = Offset(centerX - 44f * scale, centerY + 10f * scale))
    drawCircle(color = c, radius = 42f * scale, center = Offset(centerX + 46f * scale, centerY + 8f * scale))
    drawRoundRect(
        color = c,
        topLeft = Offset(centerX - 80f * scale, centerY + 6f * scale),
        size = Size(162f * scale, 42f * scale),
        cornerRadius = CornerRadius(24f * scale, 24f * scale),
    )
}

@Composable
fun AnimatedWeatherGlyph(
    condition: WeatherCondition,
    isDay: Boolean,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "glyph")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "glyphSpin",
    )
    val bob by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glyphBob",
    )
    val dropPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "dropPhase",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        when (condition) {
            WeatherCondition.CLEAR -> {
                if (isDay) {
                    rotate(spin, Offset(cx, cy)) {
                        for (i in 0 until 8) {
                            val a = i * 45.0 * PI / 180.0
                            drawLine(
                                color = SolarAmber,
                                start = Offset(cx + cos(a).toFloat() * w * 0.28f, cy + sin(a).toFloat() * h * 0.28f),
                                end = Offset(cx + cos(a).toFloat() * w * 0.42f, cy + sin(a).toFloat() * h * 0.42f),
                                strokeWidth = w * 0.07f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                    drawCircle(color = SolarAmber, radius = w * 0.21f, center = Offset(cx, cy))
                } else {
                    drawCircle(color = Color(0xFFE3EEFF), radius = w * 0.26f, center = Offset(cx, cy))
                    drawCircle(color = Color(0xFF152749), radius = w * 0.20f, center = Offset(cx + w * 0.10f, cy - h * 0.08f))
                }
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) {
                    drawCircle(color = SolarAmber, radius = w * 0.17f, center = Offset(cx - w * 0.14f, cy - h * 0.14f))
                } else {
                    drawCircle(color = Color(0xFFDCEBFF), radius = w * 0.15f, center = Offset(cx - w * 0.14f, cy - h * 0.14f))
                }
                drawMiniCloud(cx + w * 0.04f, cy + h * 0.08f + bob, w * 0.68f, Color(0xFFF3F8FF))
            }
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> {
                drawMiniCloud(cx, cy + bob, w * 0.78f, Color(0xFFE7EFFA))
            }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> {
                drawMiniCloud(cx, cy - h * 0.08f, w * 0.72f, Color(0xFFDCE8F7))
                for (i in -1..1) {
                    val x = cx + i * w * 0.16f
                    val yStart = cy + h * 0.12f + ((dropPhase + (i + 1) * 0.25f) % 1f) * h * 0.18f
                    drawLine(
                        color = Color(0xFF72C6FF),
                        start = Offset(x, yStart),
                        end = Offset(x - w * 0.04f, yStart + h * 0.13f),
                        strokeWidth = w * 0.065f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            WeatherCondition.STORM -> {
                drawMiniCloud(cx, cy - h * 0.10f, w * 0.72f, Color(0xFFB8C9DE))
                val bolt = Path().apply {
                    moveTo(cx + w * 0.04f, cy + h * 0.04f)
                    lineTo(cx - w * 0.09f, cy + h * 0.22f)
                    lineTo(cx + w * 0.01f, cy + h * 0.22f)
                    lineTo(cx - w * 0.06f, cy + h * 0.41f)
                    lineTo(cx + w * 0.12f, cy + h * 0.17f)
                    lineTo(cx + w * 0.01f, cy + h * 0.17f)
                    close()
                }
                drawPath(bolt, color = SolarAmber)
            }
            WeatherCondition.SNOW -> {
                drawMiniCloud(cx, cy - h * 0.08f, w * 0.72f, Color(0xFFEBF4FF))
                for (i in -1..1) {
                    val x = cx + i * w * 0.17f
                    val y = cy + h * 0.18f + ((dropPhase + (i + 1) * 0.3f) % 1f) * h * 0.18f
                    drawCircle(color = Color(0xFFB3E5FC), radius = w * 0.055f, center = Offset(x, y))
                }
            }
        }
    }
}

private fun DrawScope.drawMiniCloud(cx: Float, cy: Float, width: Float, color: Color) {
    drawCircle(color = color, radius = width * 0.22f, center = Offset(cx - width * 0.18f, cy + width * 0.03f))
    drawCircle(color = color, radius = width * 0.28f, center = Offset(cx + width * 0.02f, cy - width * 0.06f))
    drawCircle(color = color, radius = width * 0.21f, center = Offset(cx + width * 0.23f, cy + width * 0.04f))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - width * 0.38f, cy - width * 0.01f),
        size = Size(width * 0.78f, width * 0.26f),
        cornerRadius = CornerRadius(width * 0.13f, width * 0.13f),
    )
}

@Composable
fun HourlyTempCurveChart(
    hourly: List<HourlyPoint>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    val slice = hourly.take(10)
    if (slice.size < 2) return

    Canvas(modifier = modifier) {
        val temps = slice.map { WeatherFormatter.convertTemp(it.tempC, unit).toFloat() }
        val minT = (temps.minOrNull() ?: 0f) - 1.5f
        val maxT = (temps.maxOrNull() ?: 30f) + 1.5f
        val range = (maxT - minT).coerceAtLeast(2f)

        val padX = 16.dp.toPx()
        val padY = 14.dp.toPx()
        val usableW = size.width - padX * 2
        val usableH = size.height - padY * 2

        val points = temps.mapIndexed { idx, t ->
            val x = padX + (idx.toFloat() / (temps.size - 1)) * usableW
            val norm = (t - minT) / range
            val y = padY + (1f - norm) * usableH
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x557AD7F0), Color.Transparent),
            ),
        )
        drawPath(
            path = linePath,
            color = Color(0xFF9BE7FF),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        points.forEachIndexed { idx, pt ->
            drawCircle(
                color = if (idx == 0) SolarAmber else Color.White,
                radius = if (idx == 0) 5.dp.toPx() else 3.dp.toPx(),
                center = pt,
            )
        }
    }
}
