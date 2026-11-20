package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.SavedLocation
import com.example.domain.model.WeatherFormatter
import com.example.ui.WeatherUiState
import com.example.ui.components.AnimatedWeatherGlyph
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.IceCyan
import java.util.TimeZone

@Composable
fun LocationsScreen(
    uiState: WeatherUiState,
    onSearchQueryChange: (String) -> Unit,
    onAddCity: (SavedLocation) -> Unit,
    onSelectLocation: (String) -> Unit,
    onRemoveLocation: (String) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ciudades y ubicaciones",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = "Las ciudades guardadas también aparecen en tus widgets interactivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Buscar ciudad o país…",
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar",
                            tint = IceCyan,
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = Color.White,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IceCyan,
                        unfocusedBorderColor = GlassBorderDark,
                        focusedContainerColor = GlassSurfaceDark,
                        unfocusedContainerColor = GlassSurfaceDark,
                        cursorColor = IceCyan,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input"),
                )
            }

            // GPS Action Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = IceCyan.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !uiState.isFetchingGps, onClick = onRequestGps)
                        .testTag("locations_gps_card"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Mi ubicación GPS",
                            tint = IceCyan,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usar mi ubicación actual",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Detecta tu ciudad, hora local y temporada automáticamente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                        if (uiState.isFetchingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }

            // Search Results Section
            if (uiState.searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Resultados de búsqueda",
                        style = MaterialTheme.typography.labelLarge,
                        color = IceCyan,
                    )
                }
                items(uiState.searchResults, key = { "res_${it.id}" }) { result ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = GlassSurfaceDark,
                        border = BorderStroke(1.dp, GlassBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onAddCity(result) }
                            .testTag("search_result_${result.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                if (result.displaySubtitle.isNotBlank()) {
                                    Text(
                                        text = result.displaySubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.72f),
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Añadir ciudad",
                                tint = IceCyan,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Mis ubicaciones (${uiState.savedLocations.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(uiState.savedLocations, key = { it.id }) { loc ->
                val snap = uiState.snapshots[loc.id]
                val isSelected = loc.id == uiState.selectedLocationId
                val tzId = snap?.timeZoneId?.ifBlank { TimeZone.getDefault().id } ?: TimeZone.getDefault().id
                val season = WeatherFormatter.calculateSeason(loc.latitude, tzId)
                val localTime = if (snap != null) {
                    WeatherFormatter.formatLiveTimeForZone(tzId, uiState.use24HourClock)
                } else "--:--"

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.22f) else GlassSurfaceDark,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) IceCyan else GlassBorderDark,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelectLocation(loc.id) }
                        .testTag("saved_city_card_${loc.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Activa",
                                        tint = IceCyan,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            if (loc.displaySubtitle.isNotBlank()) {
                                Text(
                                    text = loc.displaySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.72f),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$localTime · ${season.emoji} ${season.labelEs}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }

                        if (snap != null) {
                            AnimatedWeatherGlyph(
                                condition = snap.condition,
                                isDay = snap.isDay,
                                size = 38.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = WeatherFormatter.formatTemp(snap.currentTempC, uiState.temperatureUnit),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }

                        if (uiState.savedLocations.size > 1) {
                            IconButton(
                                onClick = { onRemoveLocation(loc.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_city_${loc.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Eliminar ciudad",
                                    tint = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
