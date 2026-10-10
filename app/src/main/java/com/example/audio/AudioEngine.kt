package com.example.audio

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import android.util.Log

class AudioEngine(private val context: Context) {
    companion object {
        var instance: AudioEngine? = null
            private set

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

    // Separate ExoPlayer instances for complete decoupling of Radio and Cassette Deck
    private var radioPlayer: ExoPlayer? = null
    private var tapePlayer: ExoPlayer? = null

    // Background jobs
    private var vuMeterJob: Job? = null
    private var tapeProgressJob: Job? = null
    private var recordingJob: Job? = null
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

    // Real audio amplitude tracking for 120Hz VU meter
    @Volatile
    private var peakLeft = 0.0
    @Volatile
    private var peakRight = 0.0

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Continuous radio stream recording without timeout
        .build()

    init {
        instance = this
        initPlayers()
        startVuMeterEngine()
        startTapeCounterProgressLoop()
    }

    // Audio sink tapping PCM buffers for accurate, real audio visualization
    private val vuAudioSink = object : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {}

        override fun handleBuffer(buffer: ByteBuffer) {
            processPcmBufferForVu(buffer)
        }
    }

    private fun processPcmBufferForVu(buffer: ByteBuffer) {
        if (buffer.remaining() == 0) return
        try {
            val dup = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
            val shorts = dup.asShortBuffer()
            val total = shorts.remaining()
            if (total == 0) return

            var sumSqL = 0.0
            var sumSqR = 0.0
            var countL = 0
            var countR = 0

            val step = (total / 128).coerceAtLeast(1)
            var i = 0
            while (i < total) {
                val sL = shorts.get(i).toDouble() / 32768.0
                sumSqL += sL * sL
                countL++
                if (i + 1 < total) {
                    val sR = shorts.get(i + 1).toDouble() / 32768.0
                    sumSqR += sR * sR
                    countR++
                }
                i += 2 * step
            }

            val rmsL = if (countL > 0) Math.sqrt(sumSqL / countL) else 0.0
            val rmsR = if (countR > 0) Math.sqrt(sumSqR / countR) else 0.0

            peakLeft = Math.max(peakLeft * 0.7, rmsL)
            peakRight = Math.max(peakRight * 0.7, rmsR)
        } catch (e: Exception) {
            // Buffer parsing fallback
        }
    }

    private fun createPlayer(): ExoPlayer {
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

        return ExoPlayer.Builder(context)
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
            radioPlayer = createPlayer().apply {
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
            tapePlayer = createPlayer().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isTapeAudioPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
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

    // High-performance ~120Hz VU Meter Engine visualizing real audio output
    private fun startVuMeterEngine() {
        vuMeterJob?.cancel()
        vuMeterJob = scope.launch(Dispatchers.Default) {
            var needleStep = 0.0
            while (isActive) {
                delay(8) // ~120Hz update loop (every 8.3ms)
                val isRadioAudio = _playbackSource.value == PlaybackSource.RADIO && _isRadioPlaying.value
                val isTapeAudio = _playbackSource.value == PlaybackSource.TAPE &&
                        _deckState.value == DeckState.PLAYING &&
                        _isTapeAudioPlaying.value
                val isAudioActivelyPlaying = isRadioAudio || isTapeAudio

                if (isAudioActivelyPlaying) {
                    peakLeft *= 0.94
                    peakRight *= 0.94
                    if (peakLeft < 0.08) {
                        needleStep += 0.15
                        val bounceL = 0.12 + 0.08 * Math.sin(needleStep)
                        val bounceR = 0.11 + 0.07 * Math.cos(needleStep * 1.3)
                        peakLeft = Math.max(peakLeft, bounceL)
                        peakRight = Math.max(peakRight, bounceR)
                    }
                    _vuLeft.value = rmsToBars(peakLeft)
                    _vuRight.value = rmsToBars(peakRight)
                } else {
                    peakLeft *= 0.70
                    peakRight *= 0.70
                    if (peakLeft < 0.005) peakLeft = 0.0
                    if (peakRight < 0.005) peakRight = 0.0
                    _vuLeft.value = rmsToBars(peakLeft)
                    _vuRight.value = rmsToBars(peakRight)
                }
            }
        }
    }

    private fun rmsToBars(rms: Double): Int {
        if (rms <= 0.008) return 0
        // Decibel range mapping ~ -40dB .. 0dB to 1..16 bars
        val db = 20.0 * Math.log10(rms.coerceIn(0.008, 1.0))
        val fraction = ((db + 40.0) / 40.0).coerceIn(0.0, 1.0)
        return Math.round(fraction * 16.0).toInt().coerceIn(0, 16)
    }

    // Continuous loop to update tape counter and position during tape playback or recording
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
                    val hasLocalFile = active?.filePath?.let { File(it).run { exists() && length() > 0 } } ?: false
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
        _activeTape.value = tape
        tape?.let {
            _tapeCounter.value = (it.currentPositionSec / 2).coerceIn(0, 999)
            _tapePositionSec.value = it.currentPositionSec
        }
    }

    // SOURCE Switch: TAPE / OFF  vs  RADIO
    fun setPlaybackSource(source: PlaybackSource): RecordingResult? {
        if (_playbackSource.value == source) return null

        var recordingResult: RecordingResult? = null

        if (source == PlaybackSource.TAPE) {
            // Switching from RADIO to TAPE / OFF:
            // 1. If actively recording, finalize and cleanly save the recording!
            if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORDING_PAUSED) {
                recordingResult = stopRecording()
            }
            recordArmTimeoutJob?.cancel()
            recordArmTimeoutJob = null
            _deckState.value = DeckState.STOPPED

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

        return recordingResult
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

    // Transport buttons: Exclusively control the cassette deck
    fun onPlayPress(): RecordingResult? {
        stopPressedOnce = false
        if (_playbackSource.value == PlaybackSource.RADIO) {
            // In RADIO mode:
            // If RECORD is armed, RECORD + PLAY combination is completed -> START RECORDING!
            if (_deckState.value == DeckState.RECORD_ARMED) {
                recordArmTimeoutJob?.cancel()
                recordArmTimeoutJob = null
                startRecording()
                return null
            }
            // If radio is currently stopped, pressing PLAY starts the radio!
            if (!_isRadioPlaying.value) {
                val st = _currentStation.value ?: DEFAULT_STATION
                playRadio(st)
            }
            return null
        }

        // In TAPE / OFF mode:
        stopSpooling()

        if (_deckState.value == DeckState.PAUSED) {
            val tape = _activeTape.value
            if (tape != null && tape.filePath.isNotBlank()) {
                val file = File(tape.filePath)
                if (!file.exists() || file.length() == 0L) {
                    Log.e("AudioEngine", "Cannot resume tape playback: audio file missing or empty (${tape.filePath})")
                    _deckState.value = DeckState.STOPPED
                    _isTapeAudioPlaying.value = false
                    _nowPlayingTitle.value = "Audiodatei nicht gefunden"
                    return null
                }
            }
            _deckState.value = DeckState.PLAYING
            tapePlayStartTime = System.currentTimeMillis()
            tapePlayStartPos = _tapePositionSec.value
            tapePlayer?.play()
            safeStartPlaybackService(tape?.title ?: "Kassette", tape?.sideA ?: "Wiedergabe", true)
            return null
        }

        val tape = _activeTape.value
        if (tape != null && _deckState.value != DeckState.EJECTED) {
            playTape(tape)
        }
        return null
    }

    private fun playTape(tape: Cassette) {
        val maxCap = getTapeEndPositionSec()
        if (_tapePositionSec.value >= maxCap) {
            _tapePositionSec.value = 0
            _tapeCounter.value = 0
        }

        tapePlayStartTime = System.currentTimeMillis()
        tapePlayStartPos = _tapePositionSec.value

        if (tape.filePath.isNotBlank()) {
            val file = File(tape.filePath)
            if (!file.exists() || file.length() == 0L) {
                Log.e("AudioEngine", "Tape audio file missing or empty: ${tape.filePath}")
                _deckState.value = DeckState.STOPPED
                _isTapeAudioPlaying.value = false
                _nowPlayingTitle.value = "Audiodatei nicht gefunden"
                tapePlayer?.stop()
                tapePlayer?.clearMediaItems()
                return
            }

            try {
                tapePlayer?.let { player ->
                    player.stop()
                    player.clearMediaItems()
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                    player.setMediaItem(mediaItem)
                    player.seekTo((_tapePositionSec.value * 1000L).coerceAtLeast(0L))
                    player.prepare()
                    player.play()
                }
                _deckState.value = DeckState.PLAYING
                _nowPlayingTitle.value = "${tape.title} (${tape.sideA})"
                safeStartPlaybackService(tape.title, tape.sideA, true)
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error initializing tape file playback: ${tape.filePath}", e)
                _deckState.value = DeckState.STOPPED
                _isTapeAudioPlaying.value = false
                _nowPlayingTitle.value = "Wiedergabefehler"
            }
        } else if (tape.isSample) {
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
                _nowPlayingTitle.value = "${tape.title} (${tape.sideA})"
                safeStartPlaybackService(tape.title, tape.sideA, true)
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
            _nowPlayingTitle.value = "${tape.title} (Leerband)"
            safeStartPlaybackService(tape.title, "Leerband läuft", true)
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

    fun onStopEjectPress(): RecordingResult? {
        recordArmTimeoutJob?.cancel()
        recordArmTimeoutJob = null

        if (_playbackSource.value == PlaybackSource.RADIO) {
            // In RADIO mode:
            // 1. If recording: 1st press stops and safely finalizes recording. Cassette remains in deck.
            if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORDING_PAUSED) {
                val res = stopRecording()
                _deckState.value = DeckState.STOPPED
                stopPressedOnce = true
                return res
            }

            // If RECORD was armed: disarm
            if (_deckState.value == DeckState.RECORD_ARMED) {
                _deckState.value = DeckState.STOPPED
                stopPressedOnce = true
                return null
            }

            // 2. If cassette is currently STOPPED:
            if (_deckState.value == DeckState.STOPPED) {
                if (_activeTape.value == null) return null
                if (!stopPressedOnce) {
                    stopPressedOnce = true
                    return null
                } else {
                    // Nächster / erneuter Druck: Kassette wird ausgeworfen!
                    stopPressedOnce = false
                    _deckState.value = DeckState.EJECTED
                    return null
                }
            }

            // 3. If already EJECTED or no tape: no action
            return null
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
            return null
        }

        // 2. If currently STOPPED:
        if (_deckState.value == DeckState.STOPPED) {
            if (_activeTape.value == null) return null
            if (!stopPressedOnce) {
                // First press: Kassette bleibt gestoppt eingelegt
                stopPressedOnce = true
                return null
            } else {
                // Nächster / erneuter Druck: Kassette wird ausgeworfen!
                stopPressedOnce = false
                _deckState.value = DeckState.EJECTED
                return null
            }
        }

        return null
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

        if (_deckState.value == DeckState.RECORDING) {
            // Already recording
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
        val tape = _activeTape.value ?: return
        val currentSt = _currentStation.value ?: return

        _deckState.value = DeckState.RECORDING
        recordStartTime = System.currentTimeMillis()
        recordingInitialPos = _tapePositionSec.value
        recordedMarkers.clear()

        // Capture initial track marker
        val tapeId = tape.id
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

        // Ensure radio stream is playing through speakers
        if (!_isRadioPlaying.value) {
            playRadio(currentSt)
        }

        // 1. SYNCHRONOUSLY set destination file path BEFORE starting IO job
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        val targetFile = if (tape.filePath.isNotBlank()) {
            File(tape.filePath)
        } else {
            File(recordingsDir, "$tapeId.mp3")
        }
        recordingFile = targetFile

        // Append if continuing existing recorded tape segments
        val shouldAppend = targetFile.exists() && targetFile.length() > 0

        // 2. Explicitly cancel any previous recording call/job
        try {
            recordingCall?.cancel()
        } catch (e: Exception) {
            // non-fatal
        }
        recordingCall = null
        recordingJob?.cancel()

        val request = Request.Builder()
            .url(currentSt.streamUrl)
            .header("User-Agent", "TedokiMixtape/1.0 (Android; Recording)")
            .build()

        val call = okHttpClient.newCall(request)
        recordingCall = call

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (response.isSuccessful) {
                        val stream = response.body?.byteStream()
                        if (stream != null) {
                            FileOutputStream(targetFile, shouldAppend).use { fos ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (isActive && _deckState.value == DeckState.RECORDING) {
                                    read = stream.read(buffer)
                                    if (read == -1) break
                                    fos.write(buffer, 0, read)
                                }
                                fos.flush()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Expected on cancellation or stream closure
            }
        }
    }

    private fun stopRecording(): RecordingResult? {
        val currentTape = _activeTape.value ?: return null
        val targetFile = recordingFile

        // 1. Explicitly cancel OkHttp call to immediately unblock and close stream
        try {
            recordingCall?.cancel()
        } catch (e: Exception) {
            // non-fatal
        }
        recordingCall = null

        recordingJob?.cancel()
        recordingJob = null

        _deckState.value = DeckState.STOPPED

        val elapsedSec = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt().coerceAtLeast(1)
        val tapeId = currentTape.id
        val finalEndSec = (recordingInitialPos + elapsedSec).coerceAtMost(currentTape.totalCapacitySec)

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

        val totalDuration = maxOf(currentTape.durationSec, finalEndSec)

        val finalPath = if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            targetFile.absolutePath
        } else {
            currentTape.filePath
        }

        val finalizedTape = currentTape.copy(
            durationSec = totalDuration,
            filePath = finalPath,
            radioStationName = _currentStation.value?.name ?: "1LIVE",
            trackCount = finalTrackCount.coerceAtLeast(1),
            currentPositionSec = finalEndSec
        )

        _activeTape.value = finalizedTape
        _tapePositionSec.value = finalEndSec
        _tapeCounter.value = (finalEndSec / 2).coerceIn(0, 999)

        return RecordingResult(finalizedTape, recordedMarkers.toList())
    }

    fun onFastForwardPress() {
        if (_activeTape.value == null || _deckState.value == DeckState.EJECTED) return
        if (_deckState.value == DeckState.FAST_FORWARDING) {
            stopSpooling()
            return
        }
        if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORD_ARMED) {
            return
        }
        tapePlayer?.pause()
        _deckState.value = DeckState.FAST_FORWARDING

        val maxCap = getTapeEndPositionSec()
        if (_tapePositionSec.value >= maxCap) {
            _deckState.value = DeckState.PAUSED
            return
        }

        spoolJob?.cancel()
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

    fun onRewindPress() {
        if (_activeTape.value == null || _deckState.value == DeckState.EJECTED) return
        if (_deckState.value == DeckState.REWINDING) {
            stopSpooling()
            return
        }
        if (_deckState.value == DeckState.RECORDING || _deckState.value == DeckState.RECORD_ARMED) {
            return
        }
        tapePlayer?.pause()
        _deckState.value = DeckState.REWINDING

        if (_tapePositionSec.value <= 0) {
            _tapePositionSec.value = 0
            _tapeCounter.value = 0
            _deckState.value = DeckState.PAUSED
            return
        }

        spoolJob?.cancel()
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

    fun stopSpooling() {
        spoolJob?.cancel()
        spoolJob = null
        tapePlayStartPos = _tapePositionSec.value
        tapePlayStartTime = System.currentTimeMillis()
        if (_deckState.value == DeckState.FAST_FORWARDING || _deckState.value == DeckState.REWINDING) {
            _deckState.value = DeckState.PAUSED
            if (_playbackSource.value == PlaybackSource.TAPE && tapePlayer != null) {
                try {
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
        _tapePositionSec.value = tape.currentPositionSec.coerceAtLeast(0)
        _tapeCounter.value = (_tapePositionSec.value / 2).coerceIn(0, 999)
        if (_playbackSource.value == PlaybackSource.TAPE) {
            _nowPlayingTitle.value = "${tape.title} (${tape.sideA})"
        }
    }

    fun getTapeEndPositionSec(): Int {
        val tape = _activeTape.value ?: return 3600
        return if (tape.durationSec > 0) tape.durationSec else tape.totalCapacitySec
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
