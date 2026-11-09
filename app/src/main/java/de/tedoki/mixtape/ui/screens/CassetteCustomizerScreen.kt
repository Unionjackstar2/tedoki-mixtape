package de.tedoki.mixtape.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.ui.components.CassetteView
import de.tedoki.mixtape.ui.theme.DeckSurfaceDark

@Composable
fun CassetteCustomizerScreen(
    initialCassette: Cassette,
    onSave: (Cassette) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var title by remember { mutableStateOf(initialCassette.title) }
    var sideA by remember { mutableStateOf(initialCassette.sideA) }
    var sideB by remember { mutableStateOf(initialCassette.sideB) }
    var notes by remember { mutableStateOf(initialCassette.notes) }
    var selectedColorHex by remember { mutableStateOf(initialCassette.colorHex) }

    val colorPalette = listOf(
        "#212328", // Deep Black
        "#DCD2BE", // Vintage Cream
        "#D86228", // Retro Orange
        "#2D8B88", // Pastel Teal
        "#2E5B9A", // Electric Blue
        "#387A4C", // Emerald Green
        "#5E3C7A"  // Deep Purple
    )

    val previewCassette = remember(title, sideA, sideB, selectedColorHex, notes) {
        initialCassette.copy(
            title = title,
            sideA = sideA,
            sideB = sideB,
            notes = notes,
            colorHex = selectedColorHex
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckSurfaceDark)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
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

            Text(
                text = "Kassette gestalten",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                onSave(previewCassette)
                onBack()
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Speichern",
                    tint = Color(0xFFFFB300)
                )
            }
        }

        // Live Cassette Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            CassetteView(
                cassette = previewCassette,
                isSpinning = false,
                tapePositionSec = previewCassette.currentPositionSec,
                totalCapacitySec = previewCassette.totalCapacitySec,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Name / Label
            Column {
                Text(
                    text = "Name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB0BEC5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("z.B. Sommer 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedContainerColor = Color(0xFF1E2127),
                        unfocusedContainerColor = Color(0xFF1E2127)
                    )
                )
            }

            // Side A
            Column {
                Text(
                    text = "Seite A (optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB0BEC5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = sideA,
                    onValueChange = { sideA = it },
                    placeholder = { Text("z.B. Radioaufnahme Roadtrip") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedContainerColor = Color(0xFF1E2127),
                        unfocusedContainerColor = Color(0xFF1E2127)
                    )
                )
            }

            // Side B
            Column {
                Text(
                    text = "Seite B (optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB0BEC5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = sideB,
                    onValueChange = { sideB = it },
                    placeholder = { Text("z.B. Sommerhits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedContainerColor = Color(0xFF1E2127),
                        unfocusedContainerColor = Color(0xFF1E2127)
                    )
                )
            }

            // Gehäusefarbe Palette
            Column {
                Text(
                    text = "Gehäusefarbe",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB0BEC5)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPalette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0xFF455A64),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Action Button
            Button(
                onClick = {
                    onSave(previewCassette)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Kassette speichern",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
