package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoritesManager
import com.example.data.RadioBrowserService
import com.example.model.RadioStation
import com.example.ui.components.StationLogoView
import com.example.ui.theme.DeckSurfaceDark
import com.example.ui.theme.ShareTechMonoFontFamily

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.DigitalLcdText
import com.example.ui.theme.DigitalLcdSubtext

@Composable
fun StationBrowserScreen(
    currentStation: RadioStation?,
    isRadioPlaying: Boolean = false,
    nowPlayingText: String = "",
    onTogglePlayback: () -> Unit = {},
    onStationSelect: (RadioStation) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val view = LocalView.current

    val radioService = remember { RadioBrowserService() }
    val favoritesManager = remember { FavoritesManager.getInstance(context) }
    val favoriteUuids by favoritesManager.favoriteUuids.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("★ Favoriten") }
    val filterTabs = listOf("★ Favoriten", "Beliebt", "Alle", "DAB+", "Internet")

    var stationsList by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // List of favorite stations for the dedicated quick-access bar
    val favoriteStations = remember(favoriteUuids, radioService.curatedStations) {
        radioService.curatedStations.filter { favoriteUuids.contains(it.stationuuid) }
    }

    LaunchedEffect(searchQuery, selectedFilter, favoriteUuids) {
        isLoading = true
        stationsList = radioService.searchStations(searchQuery, selectedFilter, favoriteUuids)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
            .statusBarsPadding()
    ) {
        // Header Bar: [<] "Radiosender" [🔍]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Radiosender",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${favoriteUuids.size} Favoriten gespeichert",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB300)
                )
            }

            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Suche",
                    tint = Color.White
                )
            }
        }

        // Top Now-Playing Banner: Current station with Play / Stop button
        if (currentStation != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF14171D))
                    .border(1.dp, if (isRadioPlaying) Color(0xFF2E7D32) else Color(0xFF2E333D), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    StationLogoView(
                        station = currentStation,
                        modifier = Modifier.size(width = 46.dp, height = 34.dp),
                        cornerRadius = 5.dp,
                        fallbackTextSize = 10.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Station Name & Now playing
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AKTUELL:",
                                fontSize = 9.sp,
                                fontFamily = ShareTechMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isRadioPlaying) Color(0xFF81C784) else Color(0xFF90A4AE),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRadioPlaying) "● WIEDERGABE" else "○ ANGEHALTEN",
                                fontSize = 9.sp,
                                fontFamily = ShareTechMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isRadioPlaying) Color(0xFF81C784) else Color(0xFFE65100)
                            )
                        }
                        Text(
                            text = currentStation.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (nowPlayingText.isNotBlank()) nowPlayingText else (currentStation.slogan.ifBlank { "Live Radio" }),
                            fontSize = 11.sp,
                            color = Color(0xFFB0BEC5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play / Stop Button
                    IconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isRadioPlaying) Color(0xFF2E7D32) else Color(0xFFE65100))
                            .border(1.dp, if (isRadioPlaying) Color(0xFF81C784) else Color(0xFFFFB74D), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isRadioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isRadioPlaying) "Radio anhalten" else "Radio starten",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Search bar (if active)
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Sender oder Genre suchen...", color = Color(0xFF90A4AE)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFB300),
                    unfocusedBorderColor = Color(0xFF37474F),
                    focusedContainerColor = Color(0xFF1E2127),
                    unfocusedContainerColor = Color(0xFF1E2127)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Dedicated Schnellzugriff-Bereich: "GESPEICHERTE FAVORITEN" (Carousel)
        if (favoriteStations.isNotEmpty() && !isSearchActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GESPEICHERTE FAVORITEN",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "${favoriteStations.size} Sender",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favoriteStations, key = { it.stationuuid }) { fav ->
                        val isCurrentlyTuned = fav.stationuuid == currentStation?.stationuuid

                        FavoriteStationCard(
                            station = fav,
                            isCurrentlyTuned = isCurrentlyTuned,
                            onClick = {
                                onStationSelect(fav)
                                onBack()
                            },
                            onRemoveFavorite = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                favoritesManager.toggleFavorite(fav.stationuuid)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Category Filter Tabs: ★ Favoriten, Beliebt, Alle, DAB+, Internet
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTabs) { tab ->
                val isSelected = tab == selectedFilter
                val isFavTab = tab.contains("Favoriten")

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) {
                                if (isFavTab) Color(0xFFC67C00) else Color(0xFF9C5A20)
                            } else {
                                Color(0xFF262A33)
                            }
                        )
                        .clickable { selectedFilter = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Station List Content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFB300))
            }
        } else if (stationsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF546E7A),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFilter.contains("Favoriten")) {
                            "Keine Favoriten gespeichert"
                        } else {
                            "Keine Sender gefunden"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedFilter.contains("Favoriten")) {
                            "Tippe auf das Stern-Symbol ★ neben einem Sender in der Liste, um ihn zu deinen Favoriten hinzuzufügen."
                        } else {
                            "Versuche einen anderen Suchbegriff oder Filter."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF90A4AE),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stationsList, key = { it.stationuuid }) { station ->
                    val isCurrentlyTuned = station.stationuuid == currentStation?.stationuuid
                    val isFav = favoriteUuids.contains(station.stationuuid)

                    StationItemRow(
                        station = station,
                        isCurrentlyTuned = isCurrentlyTuned,
                        isFavorite = isFav,
                        onToggleFavorite = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            favoritesManager.toggleFavorite(station.stationuuid)
                        },
                        onClick = {
                            onStationSelect(station)
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compact Favorite card for the top horizontal quick-access shelf
 */
@Composable
fun FavoriteStationCard(
    station: RadioStation,
    isCurrentlyTuned: Boolean,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val logoColor = try {
        Color(android.graphics.Color.parseColor(station.logoBgHex))
    } catch (e: Exception) {
        Color(0xFFE91E63)
    }

    Row(
        modifier = Modifier
            .width(170.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrentlyTuned) Color(0xFF2E3642) else Color(0xFF1E2127))
            .border(
                width = 1.dp,
                color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color(0xFF333842),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        StationLogoView(
            station = station,
            modifier = Modifier.size(width = 38.dp, height = 30.dp),
            cornerRadius = 4.dp,
            fallbackTextSize = 10.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Name & Slogan
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.slogan.ifBlank { "Favorit" },
                fontSize = 10.sp,
                color = Color(0xFF90A4AE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Active amber star
        IconButton(
            onClick = onRemoveFavorite,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorit entfernen",
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun StationItemRow(
    station: RadioStation,
    isCurrentlyTuned: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCurrentlyTuned) Color(0xFF262C36) else Color(0xFF191B20)
            )
            .border(
                width = if (isCurrentlyTuned) 1.dp else 0.5.dp,
                color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color(0xFF282C34),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Station Logo badge / Real Logo
        StationLogoView(
            station = station,
            modifier = Modifier.size(width = 56.dp, height = 40.dp),
            cornerRadius = 6.dp,
            fallbackTextSize = 12.sp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Name and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.slogan.ifBlank { station.tags },
                fontSize = 13.sp,
                color = Color(0xFF90A4AE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite star button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorit umschalten",
                tint = if (isFavorite) Color(0xFFFFB300) else Color(0xFF78909C),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
