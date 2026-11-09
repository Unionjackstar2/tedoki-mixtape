package de.tedoki.mixtape.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.TrackMarker
import de.tedoki.mixtape.ui.components.CassetteSpineView
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark
import de.tedoki.mixtape.ui.theme.ShareTechMonoFontFamily

/**
 * Authentic vintage cassette rack (Kassettenregal) screen:
 * - Cassettes are slotted into horizontal rack compartments
 * - Only the spine (Rückseite / Schmalseite) of each cassette is visible with labeling, J-card colors, and C-60 badge
 * - Tapping a cassette opens the detailed inspection screen (CassetteDetailScreen) with full cassette artwork,
 *   recorded songs (radio metadata), length, and direct "In Deck einlegen" playback!
 */
@Composable
fun CassetteShelfScreen(
    cassettes: List<Cassette>,
    allTracks: Map<String, List<TrackMarker>> = emptyMap(),
    onSelectTape: (Cassette) -> Unit,
    onCreateNewTape: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
            .statusBarsPadding()
    ) {
        // Top Header Bar: [<] "Kassettenregal" [+]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück zum Deck",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kassettenregal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${cassettes.size} Mixtapes • C-60 Kassetten-Archiv",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            IconButton(onClick = onCreateNewTape) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Neues Mixtape anlegen",
                    tint = Color(0xFFFFB300)
                )
            }
        }

        // Scrollable Rack Cabinet Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Authentic Hi-Fi / Teak Wood Shelf Cabinet Enclosure
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    // Dark wooden veneer / HiFi rack cabinet gradient
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2C1E16), // Dark walnut wood top
                                Color(0xFF1E140E),
                                Color(0xFF150D09)  // Shadowed base
                            )
                        )
                    )
                    .border(2.5.dp, Color(0xFF5A3822), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Vintage Metal Rack Plaque / Emblem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2B2117),
                                        Color(0xFF8D6E4B),
                                        Color(0xFFD7B377),
                                        Color(0xFF8D6E4B),
                                        Color(0xFF2B2117)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF4E3725), RoundedCornerShape(4.dp))
                            .padding(vertical = 4.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TEDOKI HI-FI STEREO CASSETTE RACK • C-60",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2A1B0E),
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Internal Cassette Rack Bay (Recessed with guide rails)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D0907))
                            .border(1.dp, Color(0xFF211710), RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Render each cassette slotted into the rack
                        cassettes.forEachIndexed { index, tape ->
                            val slotNumber = String.format("%02d", index + 1)

                            CassetteRackSlot(
                                slotNumber = slotNumber,
                                cassette = tape,
                                onClick = { onSelectTape(tape) }
                            )
                        }

                        // Provide empty rack slots so the shelf authentically looks like a physical rack with capacity!
                        val emptySlotsCount = (6 - cassettes.size).coerceAtLeast(2)
                        for (i in 0 until emptySlotsCount) {
                            val slotIndex = cassettes.size + i + 1
                            val slotNumber = String.format("%02d", slotIndex)

                            EmptyRackSlot(
                                slotNumber = slotNumber,
                                onClick = onCreateNewTape
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Information hint under the shelf
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF191B20))
                    .border(0.5.dp, Color(0xFF282C34), RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tippe auf eine Kassette im Regal, um Hülle, Songs & J-Card anzuzeigen und sie im Deck abzuspielen.",
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary action button to create a new tape
            Button(
                onClick = onCreateNewTape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Neues C-60 Mixtape erstellen",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Single cassette slot in the rack:
 * Displays slot rails, slot number, and the CassetteSpineView.
 */
@Composable
private fun CassetteRackSlot(
    slotNumber: String,
    cassette: Cassette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Guide Rail & Slot Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(36.dp)
            ) {
                // Outer molded slot guide tab
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF3E2C1F))
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Slot number
                Text(
                    text = slotNumber,
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8D6E4B)
                )
            }

            // Cassette Spine (Rückseite der Hülle)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                CassetteSpineView(
                    cassette = cassette,
                    onClick = onClick
                )
            }

            // Right Guide Rail
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(30.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF3E2C1F))
                )
            }
        }

        // Horizontal Wooden Shelf Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A3423),
                            Color(0xFF2C1F16),
                            Color(0xFF140E0A)
                        )
                    )
                )
        )
    }
}

/**
 * Empty rack slot showing empty space ready to hold a new mixtape
 */
@Composable
private fun EmptyRackSlot(
    slotNumber: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Guide Rail & Slot Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF261B13))
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = slotNumber,
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF4A3525)
                )
            }

            // Empty Slot Recess
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF090604))
                    .border(1.dp, Color(0xFF2A1C12), RoundedCornerShape(3.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+ Freies Regalfach",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFF5D4330),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Right Guide Rail
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(30.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF261B13))
                )
            }
        }

        // Horizontal Shelf Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF382619),
                            Color(0xFF21160F),
                            Color(0xFF0E0906)
                        )
                    )
                )
        )
    }
}
