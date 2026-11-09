package de.tedoki.mixtape

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.tedoki.mixtape.audio.AudioEngine
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.model.DeckState
import de.tedoki.mixtape.model.PlaybackSource
import de.tedoki.mixtape.model.RadioStation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioEngineLogicTest {

    private lateinit var context: Context
    private lateinit var audioEngine: AudioEngine
    private val testStation = RadioStation(
        stationuuid = "uuid-1live",
        name = "1LIVE",
        streamUrl = "https://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
        dialFrequencyMhz = 105.5f
    )
    private val testTape = Cassette(
        id = "test_tape_1",
        title = "Mein Mixtape",
        sideA = "Seite A",
        durationSec = 120,
        currentPositionSec = 120
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioEngine = AudioEngine(context)
        audioEngine.setStation(testStation)
    }

    @Test
    fun testAcceptanceCriteria_1_To_13() {
        // 1. App starten: TAPE / OFF, Radio aus.
        assertEquals(PlaybackSource.TAPE, audioEngine.playbackSource.value)
        assertFalse(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // Insert test tape
        audioEngine.insertTape(testTape)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertEquals(testTape.id, audioEngine.activeTape.value?.id)

        // 2. PLAY drücken: Kassette spielt.
        audioEngine.onPlayPress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)
        assertFalse(audioEngine.isRadioPlaying.value)

        // 3. RADIO wählen: Kassette stoppt, Radio startet automatisch.
        audioEngine.setPlaybackSource(PlaybackSource.RADIO)
        assertEquals(PlaybackSource.RADIO, audioEngine.playbackSource.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 4. Im RADIO-Modus PLAY drücken: Radio läuft unverändert weiter.
        audioEngine.onPlayPress()
        assertTrue(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // 5. Im RADIO-Modus PAUSE drücken: Radio läuft unverändert weiter.
        audioEngine.onPausePress()
        assertTrue(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // 6. Im RADIO-Modus STOP/EJECT einmal drücken: Radio läuft weiter; Kassette bleibt gestoppt eingelegt.
        audioEngine.onStopEjectPress()
        assertTrue(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertNotNull(audioEngine.activeTape.value)

        // 7. RECORD allein drücken: keine Aufnahme beginnt.
        audioEngine.onRecordPress()
        assertEquals(DeckState.RECORD_ARMED, audioEngine.deckState.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 8. RECORD + PLAY drücken: Radiostream wird tatsächlich auf die Kassette aufgenommen.
        audioEngine.onPlayPress()
        assertEquals(DeckState.RECORDING, audioEngine.deckState.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 9. Während der Aufnahme PAUSE drücken: keine Wirkung; die Aufnahme läuft weiter.
        audioEngine.onPausePress()
        assertEquals(DeckState.RECORDING, audioEngine.deckState.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 10. STOP/EJECT einmal drücken: Aufnahme endet sicher, Radio läuft weiter.
        audioEngine.onStopEjectPress()
        assertTrue(audioEngine.isRadioPlaying.value)
        val recordedTape = audioEngine.activeTape.value!!
        assertTrue(recordedTape.durationSec >= testTape.durationSec)

        // 11. STOP/EJECT erneut drücken: Kassette wird ausgeworfen.
        audioEngine.onStopEjectPress()
        assertEquals(DeckState.EJECTED, audioEngine.deckState.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 12. Eine vorhandene Kassette erneut einlegen und weiter bespielen: alte Aufnahmen bleiben erhalten, neue Aufnahme wird korrekt angehängt.
        audioEngine.insertTape(recordedTape)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        audioEngine.onRecordPress()
        assertEquals(DeckState.RECORD_ARMED, audioEngine.deckState.value)
        audioEngine.onPlayPress()
        assertEquals(DeckState.RECORDING, audioEngine.deckState.value)
        audioEngine.onStopEjectPress()
        val secondTape = audioEngine.activeTape.value!!
        assertTrue(secondTape.durationSec >= recordedTape.durationSec)

        // 13. RADIO verlassen: Radio stoppt. Im TAPE / OFF-Modus steuern die Transporttasten ausschließlich die Kassette.
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        assertEquals(PlaybackSource.TAPE, audioEngine.playbackSource.value)
        assertFalse(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // In TAPE / OFF mode: PLAY plays cassette
        audioEngine.onPlayPress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)
        assertFalse(audioEngine.isRadioPlaying.value)

        // PAUSE pauses cassette
        audioEngine.onPausePress()
        assertEquals(DeckState.PAUSED, audioEngine.deckState.value)
        assertFalse(audioEngine.isRadioPlaying.value)

        // STOP stops cassette (1st press)
        audioEngine.onStopEjectPress()
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // STOP ejects cassette (2nd press)
        audioEngine.onStopEjectPress()
        assertEquals(DeckState.EJECTED, audioEngine.deckState.value)
    }

    @Test
    fun testStopEjectTwoStageBehavior() {
        // Test cassette playback stop & eject two-stage cycle
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        audioEngine.insertTape(testTape)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // 1. Play cassette
        audioEngine.onPlayPress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)

        // 2. Press STOP/EJECT -> Wiedergabe gestoppt, Kassette bleibt eingelegt
        audioEngine.onStopEjectPress()
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertEquals(testTape.id, audioEngine.activeTape.value?.id)

        // 3. Press STOP/EJECT again -> Kassette wird ausgeworfen (EJECT)
        audioEngine.onStopEjectPress()
        assertEquals(DeckState.EJECTED, audioEngine.deckState.value)

        // Pause behavior stays pause
        audioEngine.insertTape(testTape)
        audioEngine.onPlayPress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)
        audioEngine.onPausePress()
        assertEquals(DeckState.PAUSED, audioEngine.deckState.value)
        audioEngine.onPausePress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)
    }

    @Test
    fun testRadioPlaybackControlsAndStationSelection() {
        // 1. Initial: TAPE mode, Radio off
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        assertFalse(audioEngine.isRadioPlaying.value)

        // 2. Flip switch to RADIO: Radio starts automatically
        audioEngine.setPlaybackSource(PlaybackSource.RADIO)
        assertEquals(PlaybackSource.RADIO, audioEngine.playbackSource.value)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 3. User clicks Stop/Play button on radio panel: Radio stops
        audioEngine.toggleRadioPlayback()
        assertFalse(audioEngine.isRadioPlaying.value)

        // 4. User clicks Stop/Play button on radio panel again: Radio starts playing
        audioEngine.toggleRadioPlayback()
        assertTrue(audioEngine.isRadioPlaying.value)

        // 5. User selects another station: Radio plays the new station
        val newStation = RadioStation(
            stationuuid = "wdr2",
            name = "WDR 2",
            streamUrl = "https://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
            dialFrequencyMhz = 100.4f
        )
        audioEngine.playStation(newStation)
        assertEquals("WDR 2", audioEngine.currentStation.value?.name)
        assertTrue(audioEngine.isRadioPlaying.value)

        // 6. Toggling radio while in TAPE mode switches to RADIO and starts playing
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        assertFalse(audioEngine.isRadioPlaying.value)
        audioEngine.toggleRadioPlayback()
        assertEquals(PlaybackSource.RADIO, audioEngine.playbackSource.value)
        assertTrue(audioEngine.isRadioPlaying.value)
    }

    @Test
    fun testServiceStopDoesNotEjectTapeOrTriggerTwoStageEject() {
        // Cassette is playing in TAPE mode
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        audioEngine.insertTape(testTape)
        audioEngine.onPlayPress()
        assertEquals(DeckState.PLAYING, audioEngine.deckState.value)

        // Notification/Service Stop occurs
        audioEngine.stopActiveAudioPlayback()
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertNotNull(audioEngine.activeTape.value)
        assertNotEquals(DeckState.EJECTED, audioEngine.deckState.value)

        // After service stop, mechanical STOP/EJECT should still require two presses to eject
        audioEngine.onStopEjectPress() // 1st press: confirms stopped state
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        audioEngine.onStopEjectPress() // 2nd press: ejects tape
        assertEquals(DeckState.EJECTED, audioEngine.deckState.value)
    }

    @Test
    fun testMissingAudioFileDoesNotPlayOrKeepPlayingState() {
        val nonExistentTape = Cassette(
            id = "tape_missing_file",
            title = "Missing Audio Tape",
            filePath = "/data/user/0/de.tedoki.mixtape/non_existent_audio.mp3",
            durationSec = 180
        )
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        audioEngine.insertTape(nonExistentTape)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)

        // Trying to play a tape with non-existent audio file must NOT enter or stay in PLAYING
        audioEngine.onPlayPress()
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
        assertEquals("Audiodatei nicht gefunden", audioEngine.nowPlayingTitle.value)
    }

    @Test
    fun testSwitchingFromRadioToTapeWhileRecordingStopsCleanly() {
        audioEngine.setPlaybackSource(PlaybackSource.RADIO)
        audioEngine.insertTape(testTape)
        audioEngine.onRecordPress()
        audioEngine.onPlayPress()
        assertEquals(DeckState.RECORDING, audioEngine.deckState.value)

        // Switching to TAPE must finalize recording and set deck state to STOPPED
        audioEngine.setPlaybackSource(PlaybackSource.TAPE)
        assertEquals(PlaybackSource.TAPE, audioEngine.playbackSource.value)
        assertFalse(audioEngine.isRadioPlaying.value)
        assertEquals(DeckState.STOPPED, audioEngine.deckState.value)
    }
}
