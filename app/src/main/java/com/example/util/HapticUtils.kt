package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticUtils {
    /**
     * Feiner haptischer Takt ("leichter mechanischer Widerstand") beim Verschieben
     * von analogen FM-Schiebereglern und Drehknöpfen.
     * Erzeugt einen dezenten mikro-mechanischen Klick (EFFECT_TICK / CLOCK_TICK) wie bei einem echten metallischen Rastschieber.
     */
    fun performSliderTick(context: Context, view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    return
                }
            }

            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {
            try {
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } catch (_: Exception) {}
        }
    }
}
