package de.tedoki.mixtape.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Erzeugt und verwaltet ein authentisches analoges UKW-Zwischenstationsrauschen (FM Interstation Hiss/Noise).
 *
 * Charakteristik eines echten analogen FM-Tuners:
 * - Breitbandiges thermisches Rauschen mit Filterung (VHF FM Demodulator-Charakter)
 * - Nahtloser, knisterfreier Loop über einen hardwarenahen AudioTrack (PCM 16-Bit, 44.1 kHz)
 * - Sofortige, latenzfreie Lautstärkeregelung (Crossfade) je nach Abstand zur nächsten Senderfrequenz
 */
class FmNoisePlayer {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 3 // 3-Sekunden-Buffer für natürlichen Rauschloop
        private const val TOTAL_SAMPLES = SAMPLE_RATE * DURATION_SECONDS
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentTargetVolume = 0.0f
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var playbackJob: Job? = null

    // Vorberechneter PCM-Puffer mit authentischem gefiltertem FM-Rauschen
    private val pcmData: ShortArray by lazy {
        generateAuthenticFmNoisePcm()
    }

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(TOTAL_SAMPLES * 2)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(pcmData, 0, pcmData.size)
            // Auf Endlosschleife stellen
            audioTrack?.setLoopPoints(0, pcmData.size, -1)
            audioTrack?.setVolume(0.0f)
        } catch (e: Exception) {
            // AudioTrack Fallback
        }
    }

    /**
     * Erzeugt analog gefiltertes UKW-Zwischenrauschen:
     * Weißes Rauschen mit Tief- und Bandpassfilterung (350 Hz bis 8000 Hz)
     * sowie dezentem thermischen Zufalls-Knistern.
     */
    private fun generateAuthenticFmNoisePcm(): ShortArray {
        val buffer = ShortArray(TOTAL_SAMPLES)
        val random = Random(1337) // Fester Seed für wiederholbare akustische Textur

        var lastSample = 0.0
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0

        for (i in 0 until TOTAL_SAMPLES) {
            // 1. Weißes Rauschen
            val white = (random.nextGaussian()).coerceIn(-3.0, 3.0) / 3.0

            // 2. Pink-Noise-Filterung (Paul Kellet's filter approximation für wärmeren Sound)
            b0 = 0.99765 * b0 + white * 0.0990460
            b1 = 0.96300 * b1 + white * 0.2965164
            b2 = 0.57000 * b2 + white * 1.0526913
            val pink = (b0 + b1 + b2 + white * 0.1848) * 0.35

            // 3. Dezent modulierter FM-Hiss (hochpassiger Anteil für das typische Zischen)
            val diff = (white - lastSample) * 0.4
            lastSample = white

            // 4. Gelegentliches feines Knistern (statische Entladungen in der Atmosphäre)
            val crackle = if (random.nextInt(3500) == 0) {
                (random.nextDouble() - 0.5) * 0.6
            } else 0.0

            val combined = (pink * 0.45 + diff * 0.50 + crackle).coerceIn(-1.0, 1.0)
            buffer[i] = (combined * 24000).toInt().toShort()
        }

        // Weicher Übergang am Loop-Ende (Crossfade 500 Samples), um Klickgeräusche zu vermeiden
        val fadeLen = 500
        for (i in 0 until fadeLen) {
            val weight = i.toDouble() / fadeLen
            val start = buffer[i].toDouble()
            val end = buffer[TOTAL_SAMPLES - fadeLen + i].toDouble()
            buffer[TOTAL_SAMPLES - fadeLen + i] = (end * (1.0 - weight) + start * weight).toInt().toShort()
        }

        return buffer
    }

    /**
     * Startet den Rauschloop (stumm, falls Lautstärke 0 ist).
     */
    fun start() {
        if (!isPlaying) {
            try {
                audioTrack?.play()
                isPlaying = true
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Pausiert oder stoppt das Rauschen.
     */
    fun stop() {
        if (isPlaying) {
            try {
                audioTrack?.setVolume(0.0f)
                audioTrack?.pause()
                isPlaying = false
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Setzt die Rausch-Lautstärke direkt oder gleitend (0.0f bis 1.0f).
     */
    fun setVolume(volume: Float) {
        val target = volume.coerceIn(0.0f, 1.0f)
        currentTargetVolume = target
        if (target > 0.001f) {
            if (!isPlaying) start()
            try {
                // Feine Skalierung: Maximale Rauschausgabe auf 65% Gesamtpegel deckeln,
                // damit es angenehm und nicht ohrenbetäubend laut ist
                audioTrack?.setVolume(target * 0.65f)
            } catch (ignored: Exception) {}
        } else {
            try {
                audioTrack?.setVolume(0.0f)
            } catch (ignored: Exception) {}
        }
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (ignored: Exception) {}
    }
}
