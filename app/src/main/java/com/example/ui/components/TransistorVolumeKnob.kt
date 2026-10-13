package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fotorealistischer analoger Vintage-Lautstärkeregler (Rotary Volume Knob)
 * Exakt nach der Bildvorlage:
 * - Drehbares konzentrisch gebürstetes Metallrad mit Indikatorstrich
 * - Punktskala mit 11 Graduierungspunkten, "VOLUME" Kopfzeile, "MIN" und "MAX"
 * - Bidirektionale Koppelung: Dreht sich beim manuellen Wischen UND synchron
 *   beim Drücken der Smartphone-Hardwaretasten (Lauter / Leiser).
 */
@Composable
fun TransistorVolumeKnob(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    onVolumeChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolumeRatio by remember {
        mutableFloatStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume).coerceIn(0f, 1f)
        )
    }

    // Bidirektionale Hardwaretasten-Synchronisation via BroadcastReceiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
                        val currentStreamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newRatio = (currentStreamVol.toFloat() / maxVolume).coerceIn(0f, 1f)
                        currentVolumeRatio = newRatio
                        onVolumeChanged?.invoke(newRatio)
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Winkelspanne: 270 Grad, von -135° (MIN, ca. 7:30 Uhr) bis +135° (MAX, ca. 4:30 Uhr)
    val currentAngleDeg = -135f + currentVolumeRatio * 270f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(maxVolume) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                        val currentTouch = change.position
                        val prevTouch = currentTouch - dragAmount

                        val angleCurrent = atan2(currentTouch.y - center.y, currentTouch.x - center.x)
                        val anglePrev = atan2(prevTouch.y - center.y, prevTouch.x - center.x)

                        var angleDelta = angleCurrent - anglePrev
                        if (angleDelta > PI) angleDelta -= (2 * PI).toFloat()
                        if (angleDelta < -PI) angleDelta += (2 * PI).toFloat()

                        // Umrechnung Winkeländerung (270 Grad = 4.712 Rad) in Volumenänderung
                        val deltaRatio = angleDelta / (270f * PI.toFloat() / 180f)
                        val updatedRatio = (currentVolumeRatio + deltaRatio).coerceIn(0f, 1f)
                        currentVolumeRatio = updatedRatio

                        val newIndex = (updatedRatio * maxVolume).toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newIndex, 0)
                        onVolumeChanged?.invoke(updatedRatio)
                    }
                }
        ) {
            val cX = this.size.width / 2f
            val cY = this.size.height / 2f
            val knobRadius = this.size.width * 0.32f
            val scaleRadius = this.size.width * 0.42f

            // 1. "VOLUME" Text oben zentriert
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = this@Canvas.size.width * 0.082f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                color = android.graphics.Color.parseColor("#E0E3EA")
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.08f
            }
            drawContext.canvas.nativeCanvas.drawText("VOLUME", cX, cY - scaleRadius - 10f, textPaint)

            // 2. Kreisförmige Punktskala (11 Dots entlang des 270°-Bogens)
            val dotCount = 11
            val dotStartAngle = 135f // 135° im Uhrzeigersinn (entspricht -135° bzw. 225° mathematisch)
            for (i in 0 until dotCount) {
                val fraction = i.toFloat() / (dotCount - 1)
                val angleDeg = 135f + fraction * 270f
                val rad = Math.toRadians(angleDeg.toDouble())
                val dotX = cX + scaleRadius * cos(rad).toFloat()
                val dotY = cY + scaleRadius * sin(rad).toFloat()

                val isCurrentOrBelow = fraction <= (currentVolumeRatio + 0.04f)
                val dotColor = if (isCurrentOrBelow) Color(0xFFF0E5D0) else Color(0xFF6B7280)
                val dotRadius = if (i == 0 || i == dotCount - 1) 3.2f else 2.5f

                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }

            // 3. Beschriftung "MIN" und "MAX"
            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = this@Canvas.size.width * 0.068f
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                color = android.graphics.Color.parseColor("#CFD4DC")
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.05f
            }
            // MIN Position (unten links)
            val minAngleRad = Math.toRadians(135.0)
            val minX = cX + (scaleRadius + 16f) * cos(minAngleRad).toFloat()
            val minY = cY + (scaleRadius + 18f) * sin(minAngleRad).toFloat() + 10f
            drawContext.canvas.nativeCanvas.drawText("MIN", minX, minY, labelPaint)

            // MAX Position (unten rechts)
            val maxAngleRad = Math.toRadians((135 + 270).toDouble())
            val maxX = cX + (scaleRadius + 16f) * cos(maxAngleRad).toFloat()
            val maxY = cY + (scaleRadius + 18f) * sin(maxAngleRad).toFloat() + 10f
            drawContext.canvas.nativeCanvas.drawText("MAX", maxX, maxY, labelPaint)

            // 4. Äußerer Bezel-Schatten & Vertiefung
            drawCircle(
                color = Color(0xFF0A0C0E),
                radius = knobRadius + 4f,
                center = Offset(cX, cY)
            )

            // 5. Äußerer geriffelter/gefräster Metallring (Chrom-Highlight)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF333842),
                        Color(0xFF555C6B),
                        Color(0xFF22262E),
                        Color(0xFF666E7F),
                        Color(0xFF333842)
                    ),
                    center = Offset(cX, cY)
                ),
                radius = knobRadius,
                center = Offset(cX, cY)
            )

            // 6. Konzentrisch gebürsteter dunkler Metallkern
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2D323B),
                        Color(0xFF1E2127),
                        Color(0xFF14161A),
                        Color(0xFF0F1114)
                    ),
                    center = Offset(cX, cY),
                    radius = knobRadius - 3f
                ),
                radius = knobRadius - 3f,
                center = Offset(cX, cY)
            )

            // Konzentrische Rillen (Feine Mikrogravur)
            for (r in listOf(0.35f, 0.55f, 0.75f, 0.90f)) {
                drawCircle(
                    color = Color(0xFF3C4350).copy(alpha = 0.35f),
                    radius = (knobRadius - 3f) * r,
                    center = Offset(cX, cY),
                    style = Stroke(width = 1f)
                )
            }

            // 7. Radialer Licht-Reflexions-Kegel (Authentischer Metallic-Schimmer)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.03f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.03f)
                    ),
                    center = Offset(cX, cY)
                ),
                radius = knobRadius - 4f,
                center = Offset(cX, cY)
            )

            // 8. Rotierender Zeiger / Indikator-Strich (feine weiße/silberne Gravurlinie)
            // Mathematical angle: Angle where 0° is right (3 o'clock).
            // Our knob: -135° is at 7:30 (135° down-left in Android canvas coordinates).
            val needleAngleDeg = (currentAngleDeg + 90f + 180f) // maps angle smoothly
            val needleRad = Math.toRadians((135f + currentVolumeRatio * 270f).toDouble())

            val startDistance = knobRadius * 0.28f
            val endDistance = knobRadius * 0.88f

            val startX = cX + startDistance * cos(needleRad).toFloat()
            val startY = cY + startDistance * sin(needleRad).toFloat()
            val endX = cX + endDistance * cos(needleRad).toFloat()
            val endY = cY + endDistance * sin(needleRad).toFloat()

            // Leichter Schatten unter dem Strich
            drawLine(
                color = Color.Black.copy(alpha = 0.6f),
                start = Offset(startX + 1f, startY + 1f),
                end = Offset(endX + 1f, endY + 1f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            // Feiner weißer Indikatorstrich wie im Foto
            drawLine(
                color = Color(0xFFF7F5EE),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}
