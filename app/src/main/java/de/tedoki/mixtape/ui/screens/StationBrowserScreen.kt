package de.tedoki.mixtape.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.tedoki.mixtape.data.FavoritesManager
import de.tedoki.mixtape.data.RadioBrowserService
import de.tedoki.mixtape.model.RadioStation
import de.tedoki.mixtape.ui.components.StationLogoView
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark
import de.tedoki.mixtape.ui.theme.DigitalLcdBackground
import de.tedoki.mixtape.ui.theme.ShareTechMonoFontFamily
import kotlin.math.roundToInt

enum class StationBrowserTab {
    SEARCH,
    MANAGE_FAVORITES
}

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

    val favoriteStations by favoritesManager.favoriteStations.collectAsState()
    val favoriteUuids by favoritesManager.favoriteUuids.collectAsState()
    val limitToTop8 by favoritesManager.limitToTop8InDeck.collectAsState()

    var activeTab by remember { mutableStateOf(StationBrowserTab.SEARCH) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }

    val categoryTabs = listOf("Alle", "Pop & Charts", "Rock & Indie", "News & Info", "Oldies & 80s", "DAB+")

    var searchResults by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Execute search whenever query or category changes
    LaunchedEffect(searchQuery, selectedCategory, favoriteStations) {
        if (activeTab == StationBrowserTab.SEARCH) {
            isLoading = true
            searchResults = radioService.searchStations(
                query = searchQuery,
                filter = selectedCategory,
                favoriteStations = favoriteStations
            )
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
            .statusBarsPadding()
    ) {
        // 1. Top App Bar with Hi-Fi Styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2128))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück zum Deck",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RADIOSENDER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = ShareTechMonoFontFamily,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Text(
                    text = "${favoriteStations.size} Favoriten gespeichert" + if (limitToTop8) " • Top 8 auf Deck aktiv" else " • Alle auf Deck aktiv",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB300)
                )
            }

            // Quick Counter Pill leading to favorites management
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color(0xFFFFB300) else Color(0xFF262C36))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        activeTab = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) StationBrowserTab.SEARCH else StationBrowserTab.MANAGE_FAVORITES
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color.Black else Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${favoriteStations.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color.Black else Color.White
                    )
                }
            }
        }

        // 2. Active Station LCD Banner (Play/Pause, Frequency, Name)
        if (currentStation != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF13171F), Color(0xFF0C0E13))
                        )
                    )
                    .border(
                        1.dp,
                        if (isRadioPlaying) Color(0xFF2E7D32) else Color(0xFF333B48),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationLogoView(
                        station = currentStation,
                        modifier = Modifier.size(width = 46.dp, height = 36.dp),
                        cornerRadius = 6.dp,
                        fallbackTextSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FM ${String.format("%.1f", currentStation.dialFrequencyMhz)} MHz",
                                fontSize = 10.sp,
                                fontFamily = ShareTechMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRadioPlaying) "● LIVE" else "○ PAUSE",
                                fontSize = 9.sp,
                                fontFamily = ShareTechMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isRadioPlaying) Color(0xFF81C784) else Color(0xFFB0BEC5)
                            )
                        }

                        Text(
                            text = currentStation.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (nowPlayingText.isNotBlank()) nowPlayingText else currentStation.slogan.ifBlank { "Radio Live Stream" },
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play/Stop Button
                    IconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isRadioPlaying) Color(0xFF2E7D32) else Color(0xFFE65100))
                    ) {
                        Icon(
                            imageVector = if (isRadioPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isRadioPlaying) "Anhalten" else "Starten",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Navigation Tabs: [🔍 Sender suchen] | [★ Favoriten verwalten (X)]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF14171E))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val searchTabBg by animateColorAsState(
                targetValue = if (activeTab == StationBrowserTab.SEARCH) Color(0xFF2E3544) else Color.Transparent,
                label = "searchTabBg"
            )
            val favTabBg by animateColorAsState(
                targetValue = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color(0xFF8D5B0E) else Color.Transparent,
                label = "favTabBg"
            )

            // Tab 1: Suche
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(searchTabBg)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        activeTab = StationBrowserTab.SEARCH
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (activeTab == StationBrowserTab.SEARCH) Color.White else Color(0xFF90A4AE),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sender suchen",
                        fontSize = 13.sp,
                        fontWeight = if (activeTab == StationBrowserTab.SEARCH) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == StationBrowserTab.SEARCH) Color.White else Color(0xFF90A4AE)
                    )
                }
            }

            // Tab 2: Favoriten verwalten
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(favTabBg)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        activeTab = StationBrowserTab.MANAGE_FAVORITES
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color(0xFFFFD54F) else Color(0xFF90A4AE),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Favoriten (${favoriteStations.size})",
                        fontSize = 13.sp,
                        fontWeight = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == StationBrowserTab.MANAGE_FAVORITES) Color(0xFFFFD54F) else Color(0xFF90A4AE)
                    )
                }
            }
        }

        // 4. Tab Content: Search or Manage Favorites
        when (activeTab) {
            StationBrowserTab.SEARCH -> {
                SearchTabContent(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    categoryTabs = categoryTabs,
                    onCategorySelect = { selectedCategory = it },
                    isLoading = isLoading,
                    searchResults = searchResults,
                    favoriteUuids = favoriteUuids,
                    currentStation = currentStation,
                    onToggleFavorite = { station ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.toggleFavorite(station)
                    },
                    onStationSelect = { station ->
                        onStationSelect(station)
                        onBack()
                    }
                )
            }

            StationBrowserTab.MANAGE_FAVORITES -> {
                ManageFavoritesTabContent(
                    favoriteStations = favoriteStations,
                    limitToTop8 = limitToTop8,
                    onToggleLimitToTop8 = { enabled ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.setLimitToTop8InDeck(enabled)
                    },
                    currentStation = currentStation,
                    onMoveUp = { index ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.moveFavoriteUp(index)
                    },
                    onMoveDown = { index ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.moveFavoriteDown(index)
                    },
                    onMove = { fromIndex, toIndex ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.moveFavorite(fromIndex, toIndex)
                    },
                    onRemove = { uuid ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        favoritesManager.removeFavorite(uuid)
                    },
                    onStationSelect = { station ->
                        onStationSelect(station)
                        onBack()
                    },
                    onSwitchToSearch = {
                        activeTab = StationBrowserTab.SEARCH
                    }
                )
            }
        }
    }
}

/**
 * Tab 1: Search and Discover radio stations
 */
@Composable
private fun SearchTabContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    categoryTabs: List<String>,
    onCategorySelect: (String) -> Unit,
    isLoading: Boolean,
    searchResults: List<RadioStation>,
    favoriteUuids: Set<String>,
    currentStation: RadioStation?,
    onToggleFavorite: (RadioStation) -> Unit,
    onStationSelect: (RadioStation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Sender oder Musikrichtung eingeben...", color = Color(0xFF78909C), fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Löschen",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFB300),
                unfocusedBorderColor = Color(0xFF333B48),
                focusedContainerColor = Color(0xFF181C24),
                unfocusedContainerColor = Color(0xFF181C24)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Filter Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryTabs) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF202530))
                        .clickable { onCategorySelect(category) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color(0xFFCFD8DC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Station Results List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFFB300), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Suche Sender online...", color = Color(0xFF90A4AE), fontSize = 13.sp)
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = Color(0xFF455A64),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Keine Sender gefunden",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Probiere einen anderen Suchbegriff oder wechsle die Kategorie.",
                        fontSize = 13.sp,
                        color = Color(0xFF90A4AE),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.stationuuid }) { station ->
                    val isCurrentlyTuned = station.stationuuid == currentStation?.stationuuid
                    val isFav = favoriteUuids.contains(station.stationuuid)

                    StationSearchItemRow(
                        station = station,
                        isCurrentlyTuned = isCurrentlyTuned,
                        isFavorite = isFav,
                        onToggleFavorite = { onToggleFavorite(station) },
                        onClick = { onStationSelect(station) }
                    )
                }
            }
        }
    }
}

/**
 * Tab 2: Manage Favorites with Drag & Drop and Checkmark for Deck Limiting
 */
@Composable
private fun ManageFavoritesTabContent(
    favoriteStations: List<RadioStation>,
    limitToTop8: Boolean,
    onToggleLimitToTop8: (Boolean) -> Unit,
    currentStation: RadioStation?,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onSwitchToSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Control Card: Checkmark / Switch for Limiting to Top 8 in Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1F29))
                .border(1.dp, Color(0xFF333D4F), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nur erste 8 Sender im Deck auswählbar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (limitToTop8) {
                            "Aktiv: Nur die oberen 8 Favoriten (#1 bis #8) sind auf der FM-Skala des Decks wählbar."
                        } else {
                            "Inaktiv: Alle ${favoriteStations.size} Favoriten sind auf der FM-Skala des Decks wählbar."
                        },
                        fontSize = 11.sp,
                        color = if (limitToTop8) Color(0xFFFFB300) else Color(0xFF90A4AE),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = limitToTop8,
                    onCheckedChange = onToggleLimitToTop8,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB300),
                        checkedTrackColor = Color(0xFF5D400A),
                        uncheckedThumbColor = Color(0xFF78909C),
                        uncheckedTrackColor = Color(0xFF263238)
                    ),
                    modifier = Modifier.testTag("limit_top_8_switch")
                )
            }
        }

        // Info Banner with Drag & Drop instructions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REIHENFOLGE DER FAVORITEN",
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

        if (favoriteStations.isEmpty()) {
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
                        tint = Color(0xFF455A64),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Noch keine Favoriten gespeichert",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Suche im Tab 'Sender suchen' nach deinen Lieblingssendern und markiere sie mit dem Stern ★.",
                        fontSize = 13.sp,
                        color = Color(0xFF90A4AE),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFB300))
                            .clickable { onSwitchToSearch() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Jetzt Sender suchen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        } else {
            // Drag and Drop reorderable list
            var draggingIndex by remember { mutableIntStateOf(-1) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(favoriteStations, key = { _, item -> item.stationuuid }) { index, station ->
                    val isDeckActive = if (limitToTop8) index < 8 else true
                    val isCurrentlyTuned = station.stationuuid == currentStation?.stationuuid

                    FavoriteManageItemRow(
                        index = index,
                        station = station,
                        isDeckActive = isDeckActive,
                        isCurrentlyTuned = isCurrentlyTuned,
                        canMoveUp = index > 0,
                        canMoveDown = index < favoriteStations.size - 1,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onRemove = { onRemove(station.stationuuid) },
                        onClick = { onStationSelect(station) },
                        onDragDelta = { deltaY ->
                            // Drag reorder logic: 70dp approximate item height
                            val itemHeightPx = 180f
                            val targetIndex = (index + (deltaY / itemHeightPx).roundToInt()).coerceIn(0, favoriteStations.size - 1)
                            if (targetIndex != index) {
                                onMove(index, targetIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Item Row in the Search Results
 */
@Composable
private fun StationSearchItemRow(
    station: RadioStation,
    isCurrentlyTuned: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrentlyTuned) Color(0xFF272F3D) else Color(0xFF191D24))
            .border(
                width = if (isCurrentlyTuned) 1.5.dp else 0.5.dp,
                color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color(0xFF2C3340),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationLogoView(
            station = station,
            modifier = Modifier.size(width = 50.dp, height = 38.dp),
            cornerRadius = 6.dp,
            fallbackTextSize = 12.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = station.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Frequency badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2C3442))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", station.dialFrequencyMhz)} MHz",
                        fontSize = 9.sp,
                        fontFamily = ShareTechMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (station.slogan.isNotBlank()) station.slogan else station.tags.ifBlank { station.country },
                fontSize = 11.sp,
                color = Color(0xFF90A4AE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Favorite Toggle Star
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isFavorite) "Favorit entfernen" else "Zu Favoriten hinzufügen",
                tint = if (isFavorite) Color(0xFFFFB300) else Color(0xFF78909C),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Item Row in the Favorites Management with Reorder & Deck-Preset indicator
 */
@Composable
private fun FavoriteManageItemRow(
    index: Int,
    station: RadioStation,
    isDeckActive: Boolean,
    isCurrentlyTuned: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    onDragDelta: (Float) -> Unit
) {
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrentlyTuned) Color(0xFF262E3B) else Color(0xFF191D24))
            .border(
                width = if (isCurrentlyTuned) 1.5.dp else 1.dp,
                color = when {
                    isCurrentlyTuned -> Color(0xFFFFB300)
                    isDeckActive -> Color(0xFF4A3E20)
                    else -> Color(0xFF2A2F3A)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle with gesture detection
        Box(
            modifier = Modifier
                .size(36.dp)
                .pointerInput(station.stationuuid) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { accumulatedDragY = 0f },
                        onDrag = { _, dragAmount ->
                            accumulatedDragY += dragAmount.y
                            onDragDelta(accumulatedDragY)
                        },
                        onDragEnd = { accumulatedDragY = 0f },
                        onDragCancel = { accumulatedDragY = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Verschieben",
                tint = if (isDeckActive) Color(0xFFFFB300) else Color(0xFF78909C),
                modifier = Modifier.size(20.dp)
            )
        }

        // Preset number badge (#1 .. #8 highlighted in gold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDeckActive) Color(0xFFFFB300) else Color(0xFF2C3240))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "#${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = ShareTechMonoFontFamily,
                    color = if (isDeckActive) Color.Black else Color.White
                )
                if (isDeckActive && index < 8) {
                    Text(
                        text = "DECK",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Station Monogram / Logo
        StationLogoView(
            station = station,
            modifier = Modifier.size(width = 42.dp, height = 32.dp),
            cornerRadius = 5.dp,
            fallbackTextSize = 10.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Station Details (clickable to play immediately)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = station.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyTuned) Color(0xFFFFB300) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.1f", station.dialFrequencyMhz)} MHz",
                    fontSize = 10.sp,
                    fontFamily = ShareTechMonoFontFamily,
                    color = Color(0xFFFFD54F)
                )
            }
            Text(
                text = station.slogan.ifBlank { station.tags.ifBlank { "FM Favorit" } },
                fontSize = 10.sp,
                color = Color(0xFF90A4AE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Reorder Buttons (Up & Down arrows for guaranteed 1-tap reordering)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Nach oben",
                    tint = if (canMoveUp) Color(0xFFFFB300) else Color(0xFF37474F),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Nach unten",
                    tint = if (canMoveDown) Color(0xFFFFB300) else Color(0xFF37474F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        // Delete / Remove Favorite Button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Aus Favoriten entfernen",
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
