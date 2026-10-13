package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.example.model.Cassette
import com.example.model.DeckState
import com.example.model.PlaybackSource
import com.example.model.RadioStation
import com.example.model.TrackMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import android.util.Log

class AudioEngine(
    private val context: Context,
    private val okHttpClient: OkHttpClient = defaultOkHttpClient
) {
    companion object {
        var instance: AudioEngine? = null
            private set

        val defaultOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // Continuous radio stream recording without timeout
                .build()
        }

        val DEFAULT_STATION = RadioStation(
            stationuuid = "curated_1live",
            name = "1LIVE",
            slogan = "Popmusik für NRW",
            streamUrl = "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            favicon = "https://www1.wdr.de/radio/1live/resources/img/favicon/apple-touch-icon.png",
            country = "Deutschland",
            tags = "Pop, Chart, Hits",
            dialFrequencyMhz = 105.5f,
            logoBgHex = "#E91E63",
            logoText = "1LIVE",
            isFavorite = true
        )
    }

    data class RecordingResult(
        val cassette: Cassette,
        val trackMarkers: List<TrackMarker>
    )

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Recording completion flow and error reporting
    private val _recordingCompletedFlow = MutableSharedFlow<RecordingResult>(extraBufferCapacity = 5)
    val recordingCompletedFlow: SharedFlow<RecordingResult> = _recordingCompletedFlow.asSharedFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    // 1. Separate state machines for Source, Radio and Cassette Deck
    private val _playbackSource = MutableStateFlow(PlaybackSource.TAPE) // Start fundamentally in TAPE / OFF mode
    val playbackSource: StateFlow<PlaybackSource> = _playbackSource.asStateFlow()

    private val _isTapeMode = MutableStateFlow(true)
    val isTapeMode: StateFlow<Boolean> = _isTapeMode.asStateFlow()

    private val _isRadioPlaying = MutableStateFlow(false)
    val isRadioPlaying: StateFlow<Boolean> = _isRadioPlaying.asStateFlow()

    private val _isTapeAudioPlaying = MutableStateFlow(false)
    val isTapeAudioPlaying: StateFlow<Boolean> = _isTapeAudioPlaying.asStateFlow()

    private val _deckState = MutableStateFlow(DeckState.STOPPED)
    val deckState: StateFlow<DeckState> = _deckState.asStateFlow()

    private val _currentStation = MutableStateFlow<RadioStation?>(DEFAULT_STATION)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _nowPlayingTitle = MutableStateFlow("Radio bereit")
    val nowPlayingTitle: StateFlow<String> = _nowPlayingTitle.asStateFlow()

    private val _activeTape = MutableStateFlow<Cassette?>(null)
    val activeTape: StateFlow<Cassette?> = _activeTape.asStateFlow()

    private val _tapeCounter = MutableStateFlow(0)
    val tapeCounter: StateFlow<Int> = _tapeCounter.asStateFlow()

    private val _tapePositionSec = MutableStateFlow(0)
    val tapePositionSec: StateFlow<Int> = _tapePositionSec.asStateFlow()

    // Real VU meter level outputs (0..16 bars)
    private val _vuLeft = MutableStateFlow(0)
    val vuLeft: StateFlow<Int> = _vuLeft.asStateFlow()

    private val _vuRight = MutableStateFlow(0)
    val vuRight: StateFlow<Int> = _vuRight.asStateFlow()

    private val prefs by lazy { context.getSharedPreferences("app_deck_prefs", Context.MODE_PRIVATE) }

    // Cue/Review Fast-Forward & Rewind audio simulation
    private val _isCueReviewActive = MutableStateFlow(false)
    val isCueReviewActive: StateFlow<Boolean> = _isCueReviewActive.asStateFlow()

    private val _cueReviewAudioEnabled = MutableStateFlow(
        context.getSharedPreferences("app_deck_prefs", Context.MODE_PRIVATE).getBoolean("cue_review_audio_enabled", true)
    )
    val cueReviewAudioEnabled: StateFlow<Boolean> = _cueReviewAudioEnabled.asStateFlow()

    fun setCueReviewAudioEnabled(enabled: Boolean) {
        _cueReviewAudioEnabled.value = enabled
        prefs.edit().putBoolean("cue_review_audio_enabled", enabled).apply()
    }

    // Separate ExoPlayer instances for complete decoupling of Radio and Cassette Deck
    private var radioPlayer: ExoPlayer? = null
    private var tapePlayer: ExoPlayer? = null

    // Authentisches analoges UKW-Zwischenstationsrauschen
    private val fmNoisePlayer = FmNoisePlayer()
    private var currentTunedFrequencyMhz: Float = 101.5f

    // Background jobs
    private var vuMeterJob: Job? = null
    private var tapeProgressJob: Job? = null
    private var recordingJob: Job? = null
    private var recordingFinalizeJob: Job? = null
    private var spoolJob: Job? = null
    private var recordArmTimeoutJob: Job? = null
    private var stopPressedOnce = false

    // Recording state
    private var recordingFile: File? = null
    private var recordingCall: Call? = null
    private var recordStartTime = 0L
    private var recordingInitialPos = 0
    private var tapePlayStartTime = 0L
    private var tapePlayStartPos = 0
    private val recordedMarkers = mutableListOf<TrackMarker>()

    // Real audio amplitude tracking from hardware PCM audio sink
    @Volatile
    private var rawAudioLevelL = 0.0
    @Volatile
    private var rawAudioLevelR = 0.0
    @Volatile
    private var lastPcmReceivedTimestamp = 0L

    init {
        instance = this
        initPlayers()
        startVuMeterEngine()
        startTapeCounterProgressLoop()
    }

    // Audio sink tapping PCM buffers for accurate, real audio visualization
    private fun createAudioSinkForSource(source: PlaybackSource): TeeAudioProcessor.AudioBufferSink {
        return object : TeeAudioProcessor.AudioBufferSink {
            @Volatile
            private var channelCount = 2
            @Volatile
            private var encoding = C.ENCODING_PCM_16BIT

            override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                this.channelCount = channelCount.coerceAtLeast(1)
                this.encoding = encoding
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                processPcmBufferForVu(source, buffer, channelCount, encoding)
            }
        }
    }

    private fun processPcmBufferForVu(
        source: PlaybackSource,
        buffer: ByteBuffer,
        channelCount: Int,
        encoding: Int
    ) {
        // Only process audio from the currently active playback source
        if (_playbackSource.value != source) return
        if (buffer.remaining() == 0) return

        try {
            val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)

            var sumSqL = 0.0
            var sumSqR = 0.0
            var maxPeakL = 0.0
            var maxPeakR = 0.0
            var samplePairs = 0

            if (encoding == C.ENCODING_PCM_FLOAT) {
                val floats = dup.asFloatBuffer()
                val total = floats.remaining()
                if (total == 0) return

                val frames = if (channelCount >= 2) total / 2 else total
                val stepFrames = (frames / 96).coerceAtLeast(1)

                var f = 0
                if (channelCount >= 2) {
                    while (f < frames) {
                        val idx = f * 2
                        if (idx + 1 < total) {
                            val sL = floats.get(idx).toDouble().coerceIn(-1.0, 1.0)
                            val sR = floats.get(idx + 1).toDouble().coerceIn(-1.0, 1.0)
                            val absL = Math.abs(sL)
                            val absR = Math.abs(sR)
                            if (absL > maxPeakL) maxPeakL = absL
                            if (absR > maxPeakR) maxPeakR = absR
                            sumSqL += sL * sL
                            sumSqR += sR * sR
                            samplePairs++
                        }
                        f += stepFrames
                    }
                } else {
                    while (f < frames) {
                        val s = floats.get(f).toDouble().coerceIn(-1.0, 1.0)
                        val absVal = Math.abs(s)
                        if (absVal > maxPeakL) maxPeakL = absVal
                        maxPeakR = maxPeakL
                        sumSqL += s * s
                        sumSqR += s * s
                        samplePairs++
                        f += stepFrames
                    }
                }
            } else {
                // Default 16-bit PCM little-endian
                val shorts = dup.asShortBuffer()
                val total = shorts.remaining()
                if (total == 0) return

                val frames = if (channelCount >= 2) total / 2 else total
                val stepFrames = (frames / 96).coerceAtLeast(1)

                var f = 0
                if (channelCount >= 2) {
                    while (f < frames) {
                        val idx = f * 2
                        if (idx + 1 < total) {
                            val sL = (shorts.get(idx).toDouble() / 32768.0).coerceIn(-1.0, 1.0)
                            val sR = (shorts.get(idx + 1).toDouble() / 32768.0).coerceIn(-1.0, 1.0)
                            val absL = Math.abs(sL)
                            val absR = Math.abs(sR)
                            if (absL > maxPeakL) maxPeakL = absL
                            if (absR > maxPeakR) maxPeakR = absR
                            sumSqL += sL * sL
                            sumSqR += sR * sR
                            samplePairs++
                        }
                        f += stepFrames
                    }
                } else {
                    while (f < frames) {
                        val s = (shorts.get(f).toDouble() / 32768.0).coerceIn(-1.0, 1.0)
                        val absVal = Math.abs(s)
                        if (absVal > maxPeakL) maxPeakL = absVal
                        maxPeakR = maxPeakL
                        sumSqL += s * s
                        sumSqR += s * s
                        samplePairs++
                        f += stepFrames
                    }
                }
            }

            if (samplePairs > 0) {
                val rmsL = Math.sqrt(sumSqL / samplePairs)
                val rmsR = Math.sqrt(sumSqR / samplePairs)

                // Effective signal level: weighted mix of RMS (energy) and Peak (transients)
                val calculatedL = (0.75 * rmsL + 0.25 * maxPeakL).coerceIn(0.0, 1.0)
                val calculatedR = (0.75 * rmsR + 0.25 * maxPeakR).coerceIn(0.0, 1.0)

                // Instantaneous fast rise on fresh PCM transients
                rawAudioLevelL = Math.max(rawAudioLevelL * 0.5, calculatedL)
                rawAudioLevelR = Math.max(rawAudioLevelR * 0.5, calculatedR)
                lastPcmReceivedTimestamp = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Buffer parsing fallback
        }
    }

    private fun createPlayer(source: PlaybackSource): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("TedokiMixtape/1.0 (Android; ExoPlayer)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory)

        // Custom RenderersFactory piping actual decoded PCM audio into TeeAudioProcessor for real VU measurement
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                val teeProcessor = TeeAudioProcessor(createAudioSinkForSource(source))
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(teeProcessor))
                    .build()
            }
        }

        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    private fun safeStartPlaybackService(title: String, subtitle: String = "", isPlaying: Boolean = true) {
        try {
            PlaybackService.start(context, title, subtitle, isPlaying)
        } catch (t: Throwable) {
            Log.e("AudioEngine", "PlaybackService start non-fatal error", t)
        }
    }

    private fun safeStopPlaybackService() {
        try {
            PlaybackService.stop(context)
        } catch (t: Throwable) {
            Log.e("AudioEngine", "PlaybackService stop non-fatal error", t)
        }
    }

    private fun initPlayers() {
        try {
            // Radio player
            radioPlayer = createPlayer(PlaybackSource.RADIO).apply {
                addListener(object : Player.Listener {
                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        val title = mediaMetadata.title?.toString()
                        val artist = mediaMetadata.artist?.toString()
                        if (!title.isNullOrBlank()) {
                            val combined = if (!artist.isNullOrBlank()) "$artist - $title" else title
                            _nowPlayingTitle.value = combined

                            if (_isRadioPlaying.value) {
                                val notifTitle = _currentStation.value?.name ?: "Radio"
                                safeStartPlaybackService(notifTitle, combined, true)
                            }

                            // Capture metadata marker if actively recording
                            if (_deckState.value == DeckState.RECORDING) {
                                val elapsedSec = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt()
                                val markerPos = recordingInitialPos + elapsedSec
                                val tapeId = _activeTape.value?.id ?: "tape_${System.currentTimeMillis()}"
                                val prev = recordedMarkers.lastOrNull()
                                if (prev == null || prev.title != title) {
                                    if (prev != null) {
                                        recordedMarkers[recordedMarkers.size - 1] = prev.copy(endOffsetSec = markerPos)
                                    }
                                    recordedMarkers.add(
                                        TrackMarker(
                                            cassetteId = tapeId,
                                            artist = artist ?: (_currentStation.value?.name ?: "Radio"),
                                            title = title,
                                            startOffsetSec = markerPos,
                                            endOffsetSec = markerPos
                                        )
                                    )
                                }
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                if (_playbackSource.value == PlaybackSource.RADIO) {
                                    val stName = _currentStation.value?.name ?: "Sender"
                                    if (_nowPlayingTitle.value.isBlank() || _nowPlayingTitle.value == "Radio bereit") {
                                        _nowPlayingTitle.value = "Lade $stName..."
                                    }
                                }
                            }
                            Player.STATE_READY -> {
                                if (_playbackSource.value == PlaybackSource.RADIO) {
                                    val station = _currentStation.value
                                    if (_nowPlayingTitle.value.isBlank() || _nowPlayingTitle.value.startsWith("Lade ")) {
                                        _nowPlayingTitle.value = station?.slogan?.ifBlank { "${station.name} Live" } ?: "Live Radio"
                                    }
                                }
                            }
                            Player.STATE_ENDED -> {}
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (_playbackSource.value == PlaybackSource.RADIO) {
                            _isRadioPlaying.value = isPlaying
                            if (isPlaying) {
                                val notifTitle = _currentStation.value?.name ?: "Radio"
                                safeStartPlaybackService(notifTitle, _nowPlayingTitle.value, true)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AudioEngine", "Radio playback error: ${error.errorCodeName} (${error.errorCode})", error)
                        val stationName = _currentStation.value?.name ?: "Sender"
                        _nowPlayingTitle.value = "Stream-Fehler ($stationName)"
                        _isRadioPlaying.value = false
                    }
                })
            }

            // Tape player
            tapePlayer = createPlayer(PlaybackSource.TAPE).apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isTapeAudioPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val playerDurMs = tapePlayer?.duration ?: C.TIME_UNSET
                            if (playerDurMs > 0 && playerDurMs != C.TIME_UNSET) {
                                val durSec = (playerDurMs / 1000L).toInt()
                                val current = _activeTape.value
                                if (current != null && durSec > current.durationSec) {
                                    _activeTape.value = current.copy(durationSec = durSec)
                                }
                            }
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            _isTapeAudioPlaying.value = false
                            if (_playbackSource.value == PlaybackSource.TAPE) {
                                val endPos = getTapeEndPositionSec()
                                _tapePositionSec.value = endPos
                                _tapeCounter.value = (endPos / 2).coerceIn(0, 999)
                                _deckState.value = DeckState.STOPPED
                                safeStopPlaybackService()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AudioEngine", "Tape player playback error: ${error.errorCodeName} (${error.errorCode})", error)
                        _deckState.value = DeckState.STOPPED
                        _isTapeAudioPlaying.value = false
                        _nowPlayingTitle.value = "Wiedergabefehler (${error.errorCodeName})"
                        safeStopPlaybackService()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Player initialization fallback", e)
        }
    }

    // High-performance ~120Hz VU Meter Engine visualizing real audio output from hardware PCM sink
    private fun startVuMeterEngine() {
        vuMeterJob?.cancel()
        vuMeterJob = scope.launch(Dispatchers.Default) {
            var currentMeterL = 0.0
            var currentMeterR = 0.0

            while (isActive) {
                delay(8) // ~120Hz update loop (every 8.3ms)

                val isRadioAudio = _playbackSource.value == PlaybackSource.RADIO && _isRadioPlaying.value
                val isTapeAudio = _playbackSource.value == PlaybackSource.TAPE &&
                        _deckState.value == DeckState.PLAYING &&
                        _isTapeAudioPlaying.value
                val isAudioActivelyPlaying = isRadioAudio || isTapeAudio

                val now = System.currentTimeMillis()
                val hasRecentPcm = (now - lastPcmReceivedTimestamp) < 350L

                if (isAudioActivelyPlaying && hasRecentPcm) {
                    val targetL = rawAudioLevelL
                    val targetR = rawAudioLevelR

                    // Analog needle physics (VU ballistics):
                    // Fast rise time (~20ms) on transient spikes, smooth mechanical release (~250ms)
                    if (targetL > currentMeterL) {
                        currentMeterL += (targetL - currentMeterL) * 0.42
                    } else {
                        currentMeterL += (targetL - currentMeterL) * 0.10
                    }

                    if (targetR > currentMeterR) {
                        currentMeterR += (targetR - currentMeterR) * 0.42
                    } else {
                        currentMeterR += (targetR - currentMeterR) * 0.10
                    }

                    // Gently decay raw target envelope so quiet passages drop immediately
                    rawAudioLevelL *= 0.91
                    rawAudioLevelR *= 0.91

                    _vuLeft.value = rmsToBars(currentMeterL)
                    _vuRight.value = rmsToBars(currentMeterR)
                } else {
                    // Quick decay to zero when stopped, paused or buffering
                    currentMeterL *= 0.72
                    currentMeterR *= 0.72
                    if (currentMeterL < 0.002) currentMeterL = 0.0
                    if (currentMeterR < 0.002) currentMeterR = 0.0
                    rawAudioLevelL = 0.0
                    rawAudioLevelR = 0.0

                    _vuLeft.value = rmsToBars(currentMeterL)
                    _vuRight.value = rmsToBars(currentMeterR)
                }
            }
        }
    }

    private fun rmsToBars(rms: Double): Int {
        if (rms <= 0.004) return 0
        // Decibel range mapping ~ -42dB .. 0dB mapped smoothly to 1..16 bars
        val db = 20.0 * Math.log10(rms.coerceIn(0.004, 1.0))
        val fraction = ((db + 42.0) / 42.0).coerceIn(0.0, 1.0)
        return Math.round(fraction * 16.0).toInt().coerceIn(0, 16)
    }

    // Continuous loop to update tape counter and position during tape playback or recording
    fun resolveTapeAudioFile(tape: Cassette): File? {
        if (tape.filePath.isNotBlank()) {
            val f = File(tape.filePath)
            if (f.exists() && f.length() > 0L) return f
        }
        val defaultRec = File(File(context.filesDir, "recordings"), "${tape.id}.mp3")
        if (defaultRec.exists() && defaultRec.length() > 0L) {
            return defaultRec
        }
        return null
    }

    private fun extractAudioDurationSec(file: File): Int? {
        if (!file.exists() || file.length() <= 0L) return null
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val ms = durationStr?.toLongOrNull() ?: 0L
            if (ms > 0L) (ms / 1000L).toInt() else null
        } catch (e: Exception) {
            Log.w("AudioEngine", "Could not extract duration from ${file.name}", e)
            null
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun startTapeCounterProgressLoop() {
        tapeProgressJob?.cancel()
        tapeProgressJob = scope.launch {
            while (isActive) {
                delay(120)
                if (_deckState.value == DeckState.RECORDING) {
                    val elapsedSec = ((System.currentTimeMillis() - recordStartTime) / 1000L).toInt()
                    val newPos = (recordingInitialPos + elapsedSec).coerceAtMost(3600)
                    _tapePositionSec.value = newPos
                    _tapeCounter.value = (newPos / 2).coerceIn(0, 999)
                } else if (_deckState.value == DeckState.PLAYING && _playbackSource.value == PlaybackSource.TAPE) {
                    val active = _activeTape.value
                    val hasLocalFile = active?.let { resolveTapeAudioFile(it) != null } ?: false
                    val maxCap = getTapeEndPositionSec()

                    if (hasLocalFile && tapePlayer != null) {
                        val curMs = tapePlayer?.currentPosition ?: 0L
                        val curSec = (curMs / 1000L).toInt().coerceIn(0, maxCap)
                        _tapePositionSec.value = curSec
                        _tapeCounter.value = (curSec / 2).coerceIn(0, 999)
                        if (curSec >= maxCap || tapePlayer?.playbackState == Player.STATE_ENDED) {
                            tapePlayer?.pause()
                            _deckState.value = DeckState.PAUSED
                            safeStopPlaybackService()
                        }
                    } else {
                        val elapsedSec = ((System.currentTimeMillis() - tapePlayStartTime) / 1000L).toInt()
                        val newPos = (tapePlayStartPos + elapsedSec).coerceAtMost(maxCap)
                        _tapePositionSec.value = newPos
                        _tapeCounter.value = (newPos / 2).coerceIn(0, 999)
                        if (newPos >= maxCap) {
                            tapePlayer?.pause()
                            _deckState.value = DeckState.PAUSED
                            safeStopPlaybackService()
                        }
                    }
                }
            }
        }
    }

    fun setActiveTape(tape: Cassette?) {
        if (tape == null) {
            _activeTape.value = null
            return
        }
        val audioFile = resolveTapeAudioFile(tape)
        val enriched = if (audioFile != null && tape.filePath.isBlank()) {
            tape.copy(filePath = audioFile.absolutePath)
        } else {
            tape
        }
        val realDuration = if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
            extractAudioDurationSec(audioFile)
        } else null
        val finalTape = if (realDuration != null && realDuration > enriched.durationSec) {
            enriched.copy(durationSec = realDuration)
        } else {
            enriched
        }
        _activeTape.value = finalTape
        _tapeCounter.value = (finalTape.currentPositionSec / 2).coerceIn(0, 999)
        _tapePositionSec.value = finalTape.currentPositionSec
    }

    // SOURCE Switch: TAPE / OFF  vs  RADIO
    fun setPlaybackSource(source: PlaybackSource) {
        if (_playbackSource.value == source) return

        if (source == PlaybackSource.TAPE) {
            // Switching from RADIO to TAPE / OFF:
            // 1. If actively recording, finalize and cleanly save the recording!
            if (_deckState.value == DeckState.RECORDING || 
                _deckState.value == DeckState.RECORDING_STARTING || 
                _deckState.value == DeckState.RECORDING_PAUSED) {
                stopRecording()
            }
            recordArmTimeoutJob?.cancel()
            recordArmTimeoutJob = null
            if (_deckState.value == DeckState.RECORD_ARMED) {
                _deckState.value = DeckState.STOPPED
            }

            // 2. Shut off the radio completely!
            stopRadio()

            // 3. Switch source to TAPE / OFF. Cassette does NOT start automatically!
            _playbackSource.value = PlaybackSource.TAPE
            _isTapeMode.value = true

            val tape = _activeTape.value
            _nowPlayingTitle.value = if (tape != null) "${tape.title} (${tape.sideA})" else "Keine Kassette eingelegt"
        } else {
            // Switching from TAPE / OFF to RADIO:
            // 1. If tape was playing or spooling, stop the tape!
            if (_deckState.value == DeckState.PLAYING ||
                _deckState.value == DeckState.FAST_FORWARDING ||
                _deckState.value == DeckState.REWINDING) {
                stopSpooling()
                tapePlayer?.pause()
                _deckState.value = DeckState.STOPPED
            }

            // 2. Switch source to RADIO
            _playbackSource.value = PlaybackSource.RADIO
            _isTapeMode.value = false

            // 3. Radio starts automatically with the last selected station or default fallback!
            val station = _currentStation.value ?: DEFAULT_STATION
            _currentStation.value = station
            _nowPlayingTitle.value = station.slogan.ifBlank { "${station.name} Live" }
            playRadio(station)
        }
    }

    fun playStation(station: RadioStation) {
        if (_playbackSource.value != PlaybackSource.RADIO) {
            setPlaybackSource(PlaybackSource.RADIO)
        }
        setStation(station)
    }

    fun setStation(station: RadioStation) {
        _currentStation.value = station
        if (_playbackSource.value == PlaybackSource.RADIO) {
            val title = station.slogan.ifBlank { "${station.name} Live" }
            _nowPlayingTitle.value = title
            playRadio(station)
        }
    }

    fun playRadio(station: RadioStation) {
        _currentStation.value = station
        val title = station.slogan.ifBlank { "${station.name} Live" }
        _nowPlayingTitle.value = "Lade ${station.name}..."
        try {
            radioPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                val mediaItem = MediaItem.Builder()
                    .setUri(station.streamUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(station.name)
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.volume = 1.0f
                player.prepare()
                player.play()
            }
            _isRadioPlaying.value = true
            safeStartPlaybackService(station.name, title, true)
        } catch (t: Throwable) {
            Log.e("AudioEngine", "Error playing radio stream", t)
            _nowPlayingTitle.value = "Verbindungsfehler"
            _isRadioPlaying.value = false
        }
    }

    fun toggleRadioPlayback() {
        if (_playbackSource.value != PlaybackSource.RADIO) {
            setPlaybackSource(PlaybackSource.RADIO)
            return
        }
        if (_isRadioPlaying.value) {
            stopRadio()
        } else {
            val station = _currentStation.value ?: DEFAULT_STATION
            playRadio(station)
        }
    }

    fun stopRadio() {
        _isRadioPlaying.value = false
        fmNoisePlayer.setVolume(0.0f)
        try {
            radioPlayer?.stop()
            radioPlayer?.clearMediaItems()
        } catch (e: Exception) {
            // ignore
        }
        if (_deckState.value != DeckState.PLAYING) {
            safeStopPlaybackService()
        }
    }

    /**
     * Wird aufgerufen, wenn der Nutzer an der analogen UKW-Skala oder am Frequenz-Slider dreht/schiebt.
     *
     * Ermittelt den Abstand zur nächsten Senderfrequenz:
     * - Ist der Abstand groß (Sendelücke) und Radio aktiv, ertönt authentisches UKW-Rauschen (sofern FM-Muting aus ist).
     * - Erreicht der Zeiger einen Sender (< 0.12 MHz Toleranz), blendet das Rauschen sofort aus und der Sender spielt klar.
     * - Ist FM-Muting aktiv, bleibt das Band in Lücken stumm (reines digitales Tuning).
     */
    fun updateTuningFrequency(
        frequencyMhz: Float,
        stationPool: List<RadioStation>,
        isFmMutingEnabled: Boolean
    ) {
        currentTunedFrequencyMhz = frequencyMhz

        // Rauschen nur abspielen, wenn wir uns im RADIO-Modus befinden und das Radio eingeschaltet ist
        if (_playbackSource.value != PlaybackSource.RADIO || !_isRadioPlaying.value || isFmMutingEnabled) {
            fmNoisePlayer.setVolume(0.0f)
            radioPlayer?.volume = 1.0f
            return
        }

        if (stationPool.isEmpty()) {
            fmNoisePlayer.setVolume(0.85f)
            radioPlayer?.volume = 0.0f
            return
        }

        // Finde den nächstgelegenen Sender im aktuellen Pool
        val closest = stationPool.minByOrNull { Math.abs(it.dialFrequencyMhz - frequencyMhz) }
        val distance = if (closest != null) Math.abs(closest.dialFrequencyMhz - frequencyMhz) else 10.0f

        // Schwellenwerte für analogen Empfang:
        // <= 0.08 MHz: Perfekt abgestimmt -> 100% Sender, 0% Rauschen
        // 0.08 .. 0.25 MHz: Übergangszone -> weicher Crossfade
        // > 0.25 MHz: Reines Zwischenrauschen -> 100% Rauschen, Sender stummgeschaltet
        when {
            distance <= 0.08f -> {
                fmNoisePlayer.setVolume(0.0f)
                radioPlayer?.volume = 1.0f
            }
            distance in 0.08f..0.25f -> {
                val ratio = ((distance - 0.08f) / (0.25f - 0.08f)).coerceIn(0.0f, 1.0f)
                fmNoisePlayer.setVolume(ratio * 0.85f)
                radioPlayer?.volume = (1.0f - ratio)
            }
            else -> {
                fmNoisePlayer.setVolume(0.85f)
                radioPlayer?.volume = 0.0f
            }
        }
    }

    // Transport buttons: Exclusively control the cassette deck
    fun onPlayPress() {
        stopPressedOnce = false
        if (_playbackSource.value == PlaybackSource.RADIO) {
            // In RADIO mode:
            // If RECORD is armed, RECORD + PLAY combination is completed -> START RECORDING!
            if (_deckState.value == DeckState.RECORD_ARMED) {
                recordArmTimeoutJob?.cancel()
                recordArmTimeoutJob = null
                startRecording()
                return
            }
            // If radio is currently stopped, pressing PLAY starts the radio!
            if (!_isRadioPlaying.value) {
                val st = _currentStation.value ?: DEFAULT_STATION
                playRadio(st)
            }
            return
        }

        // In TAPE / OFF mode:
        stopSpooling()

        if (_deckState.value == DeckState.RECORDING ||
            _deckState.value == DeckState.RECORDING_STARTING ||
            _deckState.value == DeckState.RECORDING_PAUSED) {
            stopRecording()
            scope.launch {
                recordingFinalizeJob?.join()
                executeTapePlay()
            }
            return
        }

        executeTapePlay()
    }

    private fun executeTapePlay() {
        if (_deckState.value == DeckState.PAUSED) {
            val tape = _activeTape.value
            val audioFile = tape?.let { resolveTapeAudioFile(it) }
            if (tape != null && tape.filePath.isNotBlank()) {
                if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
                    Log.e("AudioEngine", "Cannot resume tape playback: audio file missing or empty (${tape.filePath})")
                    _deckState.value = DeckState.STOPPED
                    _isTapeAudioPlaying.value = false
                    _nowPlayingTitle.value = "Audiodatei nicht gefunden"
                    return
                }
            }
            _deckState.value = DeckState.PLAYING
            tapePlayStartTime = System.currentTimeMillis()
            tapePlayStartPos = _tapePositionSec.value
            tapePlayer?.play()
            safeStartPlaybackService(tape?.title ?: "Kassette", tape?.sideA ?: "Wiedergabe", true)
            return
        }

        val tape = _activeTape.value
        if (tape != null && _deckState.value != DeckState.EJECTED) {
            playTape(tape)
        }
    }

    private fun playTape(tape: Cassette) {
        // Resolve audio file if any exists on disk (either in tape.filePath or in filesDir/recordings/$id.mp3)
        val audioFile = resolveTapeAudioFile(tape)
        val currentTape = if (audioFile != null && tape.filePath.isBlank()) {
            val updated = tape.copy(filePath = audioFile.absolutePath)
            _activeTape.value = updated
            updated
        } else {
            tape
        }

        // If audio file exists, extract real duration if durationSec is missing or outdated
        val realDuration = if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
            extractAudioDurationSec(audioFile)
        } else null

        val finalDuration = maxOf(realDuration ?: 0, currentTape.durationSec)
        val effectiveTape = if (finalDuration > currentTape.durationSec) {
            val updated = currentTape.copy(durationSec = finalDuration)
            _activeTape.value = updated
            updated
        } else {
            currentTape
        }

        val maxCap = getTapeEndPositionSec()
        if (_tapePositionSec.value >= maxCap) {
            _tapePositionSec.value = 0
            _tapeCounter.value = 0
        }

        tapePlayStartTime = System.currentTimeMillis()
        tapePlayStartPos = _tapePositionSec.value

        if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
            try {
                tapePlayer?.let { player ->
                    player.stop()
                    player.clearMediaItems()

                    // Unique mediaId containing path, file length and lastModified to prevent Media3
                    // from reusing stale metadata/timelines when file was appended or modified
                    val mediaId = "${audioFile.absolutePath}_${audioFile.length()}_${audioFile.lastModified()}"
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.fromFile(audioFile))
                        .setMediaId(mediaId)
                        .build()

                    player.setMediaItem(mediaItem)
                    player.seekTo((_tapePositionSec.value * 1000L).coerceAtLeast(0L))
                    player.prepare()
                    player.play()
                }
                _deckState.value = DeckState.PLAYING
                _nowPlayingTitle.value = "${effectiveTape.title} (${effectiveTape.sideA})"
                safeStartPlaybackService(effectiveTape.title, effectiveTape.sideA, true)
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error initializing tape file playback: ${audioFile.absolutePath}", e)
                _deckState.value = DeckState.STOPPED
                _isTapeAudioPlaying.value = false
                _nowPlayingTitle.value = "Wiedergabefehler"
            }
        } else if (tape.filePath.isNotBlank()) {
            // Explicit file path specified, but file missing or empty on disk
            Log.e("AudioEngine", "Tape audio file missing or empty: ${tape.filePath}")
            _deckState.value = DeckState.STOPPED
            _isTapeAudioPlaying.value = false
            _nowPlayingTitle.value = "Audiodatei nicht gefunden"
            tapePlayer?.stop()
            tapePlayer?.clearMediaItems()
        } else if (effectiveTape.isSample) {
            try {
                tapePlayer?.let { player ->
                    player.stop()
                    player.clearMediaItems()
                    val sampleStream = "https://dispatcher.rndfnk.com/rbb/radioeins/live/mp3/mid"
                    player.setMediaItem(MediaItem.fromUri(sampleStream))
                    player.prepare()
                    player.play()
                }
                _deckState.value = DeckState.PLAYING
                _nowPlayingTitle.value = "${effectiveTape.title} (${effectiveTape.sideA})"
                safeStartPlaybackService(effectiveTape.title, effectiveTape.sideA, true)
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error playing sample tape", e)
                _deckState.value = DeckState.STOPPED
                _isTapeAudioPlaying.value = false
                _nowPlayingTitle.value = "Wiedergabefehler"
            }
        } else {
            // Blank / unrecorded tape: runs motor silently
            tapePlayer?.stop()
            tapePlayer?.clearMediaItems()
            _isTapeAudioPlaying.value = false
            _deckState.value = DeckState.PLAYING
            _nowPlayingTitle.value = "${effectiveTape.title} (Leerband)"
            safeStartPlaybackService(effectiveTape.title, "Leerband läuft", true)
        }
    }

    fun onPausePress() {
        if (_playbackSource.value == PlaybackSource.RADIO) {
            // In RADIO mode:
            // 1. If actively recording: PAUSE is locked/gesperrt! Recording continues uninterrupted.
            // 2. If not recording: NO effect on radio! Radio continues playing uninterrupted.
            return
        }

        // In TAPE / OFF mode:
        when (_deckState.value) {
            DeckState.FAST_FORWARDING, DeckState.REWINDING -> {
                stopSpooling()
            }
            DeckState.PLAYING -> {
                tapePlayer?.pause()
                _isTapeAudioPlaying.value = false
                _deckState.value = DeckState.PAUSED
                val tape = _activeTape.value
                safeStartPlaybackService(tape?.title ?: "Kassette", "Pausiert", false)
            }
            DeckState.PAUSED -> {
                onPlayPress()
            }
            else -> {}
        }
    }

    fun onStopEjectPress() {
        recordArmTimeoutJob?.cancel()
        recordArmTimeoutJob = null

        if (_playbackSource.value == PlaybackSource.RADIO) {
            // In RADIO mode:
            // 1. If recording: 1st press stops and safely finalizes recording. Cassette remains in deck.
            if (_deckState.value == DeckState.RECORDING || 
                _deckState.value == DeckState.RECORDING_STARTING || 
                _deckState.value == DeckState.RECORDING_PAUSED) {
                stopRecording()
                stopPressedOnce = true
                return
            }

            // If RECORD was armed: disarm
            if (_deckState.value == DeckState.RECORD_ARMED) {
                _deckState.value = DeckState.STOPPED
                stopPressedOnce = true
                return
            }

            // 2. If cassette is currently STOPPED:
            if (_deckState.value == DeckState.STOPPED) {
                if (_activeTape.value == null) return
                if (!stopPressedOnce) {
                    stopPressedOnce = true
                    return
                } else {
                    // Nächster / erneuter Druck: Kassette wird ausgeworfen!
                    stopPressedOnce = false
                    _deckState.value = DeckState.EJECTED
                    return
                }
            }

            // 3. If already EJECTED or no tape: no action
            return
        }

        // In TAPE / OFF mode:
        // 1. If playing, paused, or spooling: 1st press stops tape playback. Cassette remains in deck.
        if (_deckState.value == DeckState.PLAYING ||
            _deckState.value == DeckState.PAUSED ||
            _deckState.value == DeckState.FAST_FORWARDING ||
            _deckState.value == DeckState.REWINDING) {
            stopSpooling()
            tapePlayer?.pause()
            _isTapeAudioPlaying.value = false
            _deckState.value = DeckState.STOPPED
            stopPressedOnce = true
            safeStopPlaybackService()
            return
        }

        // 2. If currently STOPPED:
        if (_deckState.value == DeckState.STOPPED) {
            if (_activeTape.value == null) return
            if (!stopPressedOnce) {
                // First press: Kassette bleibt gestoppt eingelegt
                stopPressedOnce = true
                return
            } else {
                // Nächster / erneuter Druck: Kassette wird ausgeworfen!
                stopPressedOnce = false
                _deckState.value = DeckState.EJECTED
                return
            }
        }
    }

    fun onRecordPress() {
        if (_playbackSource.value != PlaybackSource.RADIO) {
            // In TAPE / OFF mode: no recording without running radio stream!
            return
        }

        if (_activeTape.value == null || _deckState.value == DeckState.EJECTED) {
            // No cassette inserted
            return
        }

        if (_deckState.value == DeckState.RECORDING || 
            _deckState.value == DeckState.RECORDING_STARTING || 
            _deckState.value == DeckState.RECORDING_STOPPING) {
            // Already recording or in transition
            return
        }

        if (_deckState.value == DeckState.RECORD_ARMED) {
            // Toggle off
            recordArmTimeoutJob?.cancel()
            recordArmTimeoutJob = null
            _deckState.value = DeckState.STOPPED
        } else if (_deckState.value == DeckState.STOPPED || _deckState.value == DeckState.PAUSED) {
            // Arm recording (RECORD alone does NOT start recording; user must press PLAY within 5s window)
            _deckState.value = DeckState.RECORD_ARMED
            recordArmTimeoutJob?.cancel()
            recordArmTimeoutJob = scope.launch {
                delay(5000)
                if (_deckState.value == DeckState.RECORD_ARMED) {
                    _deckState.value = DeckState.STOPPED
                }
            }
        }
    }

    private fun startRecording() {
        if (_playbackSource.value != PlaybackSource.RADIO) {
            Log.w("AudioEngine", "Cannot start recording: Source is not RADIO")
            return
        }
        val tape = _activeTape.value ?: run {
            Log.w("AudioEngine", "Cannot start recording: No active cassette in deck")
            return
        }
        val currentSt = _currentStation.value ?: run {
            Log.w("AudioEngine", "Cannot start recording: No radio station selected")
            return
        }
        if (currentSt.streamUrl.isBlank()) {
            Log.e("AudioEngine", "Cannot start recording: Radio station stream URL is empty (${currentSt.name})")
            _recordingError.value = "Keine Stream-URL für ${currentSt.name} verfügbar"
            _deckState.value = DeckState.STOPPED
            return
        }

        _recordingError.value = null
        _deckState.value = DeckState.RECORDING
        Log.i("AudioEngine", "Starting recording for station: ${currentSt.name}, url: ${currentSt.streamUrl}")

        // Ensure radio stream is playing through speakers
        if (!_isRadioPlaying.value) {
            playRadio(currentSt)
        }

        // Destination file
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        val tapeId = tape.id
        val targetFile = if (tape.filePath.isNotBlank()) {
            File(tape.filePath)
        } else {
            File(recordingsDir, "$tapeId.mp3")
        }
        recordingFile = targetFile
        val shouldAppend = targetFile.exists() && targetFile.length() > 0

        // Cancel previous recording call/job
        try {
            recordingCall?.cancel()
        } catch (e: Exception) {
            // non-fatal
        }
        recordingCall = null
        recordingJob?.cancel()

        recordingJob = scope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url(currentSt.streamUrl)
                .header("User-Agent", "TedokiMixtape/1.0 (Android; Recording)")
                .build()

            val call = okHttpClient.newCall(request)
            recordingCall = call
            var bytesWritten = 0L

            try {
                Log.i("AudioEngine", "Connecting to radio stream for recording...")
                val response = call.execute()
                val statusCode = response.code

                if (!response.isSuccessful) {
                    Log.e("AudioEngine", "Failed to open stream for recording: HTTP $statusCode - ${response.message}")
                    response.close()
                    withContext(Dispatchers.Main) {
                        _deckState.value = DeckState.STOPPED
                        _recordingError.value = "HTTP-Fehler beim Aufnehmen: $statusCode"
                    }
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    Log.e("AudioEngine", "Recording failed: Response body is null")
                    response.close()
                    withContext(Dispatchers.Main) {
                        _deckState.value = DeckState.STOPPED
                        _recordingError.value = "Keine Audiodaten vom Stream empfangen"
                    }
                    return@launch
                }

                val stream = body.byteStream()
                Log.i("AudioEngine", "Recording stream connected successfully. Writing to: ${targetFile.absolutePath} (append: $shouldAppend)")

                withContext(Dispatchers.Main) {
                    _deckState.value = DeckState.RECORDING
                    recordStartTime = System.currentTimeMillis()
                    recordingInitialPos = _tapePositionSec.value
                    recordedMarkers.clear()

                    val currentTitle = _nowPlayingTitle.value
                    if (currentTitle.isNotBlank() && currentTitle != "Radio bereit" && !currentTitle.startsWith("Lade ")) {
                        val parts = currentTitle.split(" - ", limit = 2)
                        val artist = if (parts.size > 1) parts[0].trim() else currentSt.name
                        val title = if (parts.size > 1) parts[1].trim() else currentTitle
                        recordedMarkers.add(
                            TrackMarker(
                                cassetteId = tapeId,
                                artist = artist,
                                title = title,
                                startOffsetSec = recordingInitialPos,
                                endOffsetSec = recordingInitialPos
                            )
                        )
                    }
                }

                FileOutputStream(targetFile, shouldAppend).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (isActive && (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORDING_STARTING)) {
                        read = stream.read(buffer)
                        if (read == -1) {
                            Log.i("AudioEngine", "Recording stream closed by remote server (EOF)")
                            break
                        }
                        fos.write(buffer, 0, read)
                        bytesWritten += read
                    }
                    fos.flush()
                }
                response.close()
                Log.i("AudioEngine", "Recording loop ended. Wrote $bytesWritten bytes in this session. Total file size: ${targetFile.length()} bytes")
            } catch (e: Exception) {
                if (e is java.io.InterruptedIOException || e is java.net.SocketException || !isActive) {
                    Log.i("AudioEngine", "Recording stopped normally by user. Bytes written: $bytesWritten")
                } else {
                    Log.e("AudioEngine", "Error during radio recording: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        _recordingError.value = "Aufnahmefehler: ${e.localizedMessage ?: "Verbindungsabbruch"}"
                    }
                }
            } finally {
                Log.i("AudioEngine", "Recording IO stream finalized. File exists: ${targetFile.exists()}, size: ${targetFile.length()} bytes")
            }
        }
    }

    private fun stopRecording() {
        val currentTape = _activeTape.value ?: return
        val targetFile = recordingFile

        _deckState.value = DeckState.STOPPED
        Log.i("AudioEngine", "Stopping recording for tape: ${currentTape.title}...")

        // Update activeTape immediately with targetFile path (if valid) and position
        val fileValid = targetFile != null && targetFile.exists() && targetFile.length() > 0L
        val assignedPath = if (fileValid) targetFile!!.absolutePath else currentTape.filePath
        val elapsedSecInitial = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt().coerceAtLeast(1)
        val endSecInitial = (recordingInitialPos + elapsedSecInitial).coerceAtMost(currentTape.totalCapacitySec)
        val initialFinalized = currentTape.copy(
            filePath = assignedPath,
            currentPositionSec = endSecInitial,
            durationSec = maxOf(currentTape.durationSec, endSecInitial)
        )
        _activeTape.value = initialFinalized
        _tapePositionSec.value = endSecInitial
        _tapeCounter.value = (endSecInitial / 2).coerceIn(0, 999)

        val jobToWait = recordingJob
        val callToCancel = recordingCall
        recordingJob = null
        recordingCall = null

        recordingFinalizeJob = scope.launch {
            try {
                try {
                    callToCancel?.cancel()
                } catch (e: Exception) {
                    // non-fatal
                }

                // Await complete termination of the IO write job
                jobToWait?.join()
                Log.i("AudioEngine", "Recording job finished. Validating output file...")

                val elapsedSec = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt().coerceAtLeast(1)
                val tapeId = currentTape.id
                val finalEndSec = (recordingInitialPos + elapsedSec).coerceAtMost(currentTape.totalCapacitySec)

                if (targetFile == null || !targetFile.exists() || targetFile.length() <= 0L) {
                    Log.e("AudioEngine", "Recording failed: Audio file missing or empty (${targetFile?.absolutePath})")
                    _deckState.value = DeckState.STOPPED
                    _recordingError.value = "Aufnahme fehlgeschlagen: Keine Audiodaten empfangen"
                    return@launch
                }

                Log.i("AudioEngine", "Recording validated successfully. Path: ${targetFile.absolutePath}, Size: ${targetFile.length()} bytes")

                if (recordedMarkers.isEmpty()) {
                    val song = _nowPlayingTitle.value
                    val parts = song.split(" - ", limit = 2)
                    val artist = if (parts.size > 1) parts[0].trim() else _currentStation.value?.name ?: "Radio"
                    val title = if (parts.size > 1) parts[1].trim() else song
                    recordedMarkers.add(
                        TrackMarker(
                            cassetteId = tapeId,
                            artist = artist,
                            title = title,
                            startOffsetSec = recordingInitialPos,
                            endOffsetSec = finalEndSec
                        )
                    )
                } else {
                    val lastIdx = recordedMarkers.size - 1
                    recordedMarkers[lastIdx] = recordedMarkers[lastIdx].copy(endOffsetSec = finalEndSec)
                }

                val finalTrackCount = if (currentTape.trackCount > 1) {
                    currentTape.trackCount + recordedMarkers.size
                } else {
                    recordedMarkers.size
                }

                val realDuration = extractAudioDurationSec(targetFile)
                val totalDuration = maxOf(realDuration ?: 0, finalEndSec, currentTape.durationSec)
                val finalPosition = finalEndSec.coerceAtMost(totalDuration)

                val finalizedTape = currentTape.copy(
                    durationSec = totalDuration,
                    filePath = targetFile.absolutePath,
                    radioStationName = _currentStation.value?.name ?: "1LIVE",
                    trackCount = finalTrackCount.coerceAtLeast(1),
                    currentPositionSec = finalPosition
                )

                _activeTape.value = finalizedTape
                _tapePositionSec.value = finalPosition
                _tapeCounter.value = (finalPosition / 2).coerceIn(0, 999)
                _deckState.value = DeckState.STOPPED
                _recordingError.value = null

                val result = RecordingResult(finalizedTape, recordedMarkers.toList())
                _recordingCompletedFlow.emit(result)
                Log.i("AudioEngine", "Recording finalized and emitted: ${finalizedTape.title}, ${finalizedTape.durationSec}s, ${finalizedTape.trackCount} tracks")
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error finalizing recording: ${e.message}", e)
                _deckState.value = DeckState.STOPPED
                _recordingError.value = "Fehler beim Finalisieren: ${e.localizedMessage}"
            } finally {
                recordingFinalizeJob = null
            }
        }
    }

    fun onFastForwardPress() {
        if (_activeTape.value == null || _deckState.value == DeckState.EJECTED) return
        if (_deckState.value == DeckState.FAST_FORWARDING) {
            val wasCue = _isCueReviewActive.value
            stopSpooling()
            if (wasCue) {
                executeTapePlay()
            }
            return
        }
        if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORD_ARMED) {
            return
        }

        // Cue mode is engaged ONLY if tape was actively PLAYING in TAPE mode and option is enabled!
        val wasPlaying = (_deckState.value == DeckState.PLAYING && _playbackSource.value == PlaybackSource.TAPE)
        val shouldCue = wasPlaying && _cueReviewAudioEnabled.value && _activeTape.value != null

        _isCueReviewActive.value = shouldCue
        _deckState.value = DeckState.FAST_FORWARDING

        val maxCap = getTapeEndPositionSec()
        if (_tapePositionSec.value >= maxCap) {
            _deckState.value = DeckState.PAUSED
            return
        }

        spoolJob?.cancel()
        if (shouldCue) {
            // CUE: Real 3x fast-forward audio with authentic analog pitch increase (chipmunk effect)
            tapePlayer?.let { player ->
                try {
                    player.playbackParameters = PlaybackParameters(3.0f, 3.0f)
                    player.play()
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error setting cue playback parameters", e)
                }
            }
            spoolJob = scope.launch {
                while (isActive && _deckState.value == DeckState.FAST_FORWARDING) {
                    delay(80)
                    val playerDur = tapePlayer?.duration ?: C.TIME_UNSET
                    val hasActiveMedia = (playerDur > 0L && playerDur != C.TIME_UNSET)
                    val nextSec = if (hasActiveMedia) {
                        val curMs = tapePlayer?.currentPosition ?: (_tapePositionSec.value * 1000L)
                        (curMs / 1000L).toInt().coerceIn(0, maxCap)
                    } else {
                        (_tapePositionSec.value + 3).coerceAtMost(maxCap)
                    }
                    _tapePositionSec.value = nextSec
                    _tapeCounter.value = (nextSec / 2).coerceIn(0, 999)

                    if (nextSec >= maxCap || tapePlayer?.playbackState == Player.STATE_ENDED) {
                        stopSpooling()
                        break
                    }
                }
            }
        } else {
            // Silent spooling (e.g. from STOP or PAUSED)
            tapePlayer?.pause()
            try {
                tapePlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            } catch (_: Exception) {}

            spoolJob = scope.launch {
                while (isActive && _deckState.value == DeckState.FAST_FORWARDING) {
                    delay(100)
                    val nextPos = (_tapePositionSec.value + 6).coerceAtMost(maxCap)
                    _tapePositionSec.value = nextPos
                    _tapeCounter.value = (nextPos / 2).coerceIn(0, 999)

                    if (nextPos >= maxCap) {
                        stopSpooling()
                        break
                    }
                }
            }
        }
    }

    fun onRewindPress() {
        if (_activeTape.value == null || _deckState.value == DeckState.EJECTED) return
        if (_deckState.value == DeckState.REWINDING) {
            val wasReview = _isCueReviewActive.value
            stopSpooling()
            if (wasReview) {
                executeTapePlay()
            }
            return
        }
        if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORD_ARMED) {
            return
        }

        // Review mode is engaged ONLY if tape was actively PLAYING in TAPE mode and option is enabled!
        val wasPlaying = (_deckState.value == DeckState.PLAYING && _playbackSource.value == PlaybackSource.TAPE)
        val shouldReview = wasPlaying && _cueReviewAudioEnabled.value && _activeTape.value != null

        _isCueReviewActive.value = shouldReview
        _deckState.value = DeckState.REWINDING

        if (_tapePositionSec.value <= 0) {
            _tapePositionSec.value = 0
            _tapeCounter.value = 0
            _deckState.value = DeckState.PAUSED
            return
        }

        spoolJob?.cancel()
        if (shouldReview) {
            // REVIEW: Reverse scanning chunks with 3x speed & 3x pitch
            try {
                tapePlayer?.playbackParameters = PlaybackParameters(3.0f, 3.0f)
            } catch (_: Exception) {}

            spoolJob = scope.launch {
                while (isActive && _deckState.value == DeckState.REWINDING) {
                    val currentMs = tapePlayer?.currentPosition ?: (_tapePositionSec.value * 1000L)
                    val targetMs = (currentMs - 800L).coerceAtLeast(0L)
                    try {
                        tapePlayer?.seekTo(targetMs)
                        tapePlayer?.play()
                    } catch (_: Exception) {}

                    delay(100)

                    val newSec = (targetMs / 1000L).toInt().coerceAtLeast(0)
                    _tapePositionSec.value = newSec
                    _tapeCounter.value = (newSec / 2).coerceIn(0, 999)

                    if (targetMs <= 0L) {
                        _tapePositionSec.value = 0
                        _tapeCounter.value = 0
                        stopSpooling()
                        break
                    }
                }
            }
        } else {
            // Silent rewind (e.g. from STOP or PAUSED)
            tapePlayer?.pause()
            try {
                tapePlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            } catch (_: Exception) {}

            spoolJob = scope.launch {
                while (isActive && _deckState.value == DeckState.REWINDING) {
                    delay(100)
                    val nextPos = (_tapePositionSec.value - 6).coerceAtLeast(0)
                    _tapePositionSec.value = nextPos
                    _tapeCounter.value = (nextPos / 2).coerceIn(0, 999)

                    if (nextPos <= 0) {
                        _tapePositionSec.value = 0
                        _tapeCounter.value = 0
                        stopSpooling()
                        break
                    }
                }
            }
        }
    }

    fun stopSpooling() {
        spoolJob?.cancel()
        spoolJob = null
        _isCueReviewActive.value = false
        try {
            tapePlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f)
        } catch (_: Exception) {}
        tapePlayStartPos = _tapePositionSec.value
        tapePlayStartTime = System.currentTimeMillis()
        if (_deckState.value == DeckState.FAST_FORWARDING || _deckState.value == DeckState.REWINDING) {
            _deckState.value = DeckState.PAUSED
            if (_playbackSource.value == PlaybackSource.TAPE && tapePlayer != null) {
                try {
                    tapePlayer?.pause()
                    tapePlayer?.seekTo((_tapePositionSec.value * 1000L).coerceAtLeast(0L))
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun resetCounter() {
        _tapeCounter.value = 0
        _tapePositionSec.value = 0
    }

    fun insertTape(tape: Cassette) {
        stopPressedOnce = false
        setActiveTape(tape)
        _deckState.value = DeckState.STOPPED
        val current = _activeTape.value ?: tape
        _tapePositionSec.value = current.currentPositionSec.coerceAtLeast(0)
        _tapeCounter.value = (_tapePositionSec.value / 2).coerceIn(0, 999)
        if (_playbackSource.value == PlaybackSource.TAPE) {
            _nowPlayingTitle.value = "${current.title} (${current.sideA})"
        }
    }

    fun getTapeEndPositionSec(): Int {
        val tape = _activeTape.value ?: return 3600
        val baseCap = if (tape.durationSec > 0) tape.durationSec else tape.totalCapacitySec
        val playerDurMs = tapePlayer?.duration ?: C.TIME_UNSET
        val playerSec = if (playerDurMs > 0 && playerDurMs != C.TIME_UNSET) (playerDurMs / 1000L).toInt() else 0
        return maxOf(baseCap, playerSec)
    }

    /**
     * Explicit stop method for media service / notification / audio output stops.
     * Stops currently active audio playback (radio or tape) cleanly WITHOUT
     * triggering mechanical cassette STOP/EJECT two-stage logic or ejecting the tape.
     */
    fun stopActiveAudioPlayback() {
        if (_playbackSource.value == PlaybackSource.RADIO) {
            stopRadio()
        } else if (_playbackSource.value == PlaybackSource.TAPE) {
            if (_deckState.value == DeckState.PLAYING ||
                _deckState.value == DeckState.PAUSED ||
                _deckState.value == DeckState.FAST_FORWARDING ||
                _deckState.value == DeckState.REWINDING
            ) {
                stopSpooling()
                tapePlayer?.pause()
                _isTapeAudioPlaying.value = false
                _deckState.value = DeckState.STOPPED
            }
        }
    }

    // Compatibility methods for existing callers
    fun play() = onPlayPress()
    fun pause() = onPausePress()
    fun stop() = onStopEjectPress()
    fun eject() = onStopEjectPress()
    fun recordArm() = onRecordPress()
    fun fastForward() = onFastForwardPress()
    fun rewind() = onRewindPress()

    fun release() {
        onStopEjectPress()
        stopRadio()
        vuMeterJob?.cancel()
        tapeProgressJob?.cancel()
        radioPlayer?.release()
        radioPlayer = null
        tapePlayer?.release()
        tapePlayer = null
    }
}
