package de.tedoki.mixtape

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import de.tedoki.mixtape.model.Cassette
import de.tedoki.mixtape.util.AudioFileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioFileInfoTest {

    private lateinit var context: Context
    private lateinit var recordingsDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
    }

    @Test
    fun testFormatFileSize_zeroAndSmallAndLarge() {
        // Zero bytes
        assertEquals("0 Bytes", AudioFileUtils.formatFileSize(0L))

        // Small file (e.g. 500,000 bytes = 500.0 KB decimal, ~488.3 KiB binary)
        val formattedSmall = AudioFileUtils.formatFileSize(500000L)
        assertTrue("Expected KB format, got: $formattedSmall", formattedSmall.contains("KB"))
        assertTrue("Expected KiB format, got: $formattedSmall", formattedSmall.contains("KiB"))
        assertTrue("Expected exact bytes, got: $formattedSmall", formattedSmall.contains("500.000 Bytes") || formattedSmall.contains("500,000 Bytes"))

        // Large file (e.g. 2,500,000 bytes = 2.50 MB decimal, ~2.38 MiB binary)
        val formattedLarge = AudioFileUtils.formatFileSize(2500000L)
        assertTrue("Expected MB format, got: $formattedLarge", formattedLarge.contains("MB"))
        assertTrue("Expected MiB format, got: $formattedLarge", formattedLarge.contains("MiB"))
        assertTrue("Expected exact bytes, got: $formattedLarge", formattedLarge.contains("2.500.000 Bytes") || formattedLarge.contains("2,500,000 Bytes"))
    }

    @Test
    fun testDetermineMimeType() {
        assertEquals("audio/mpeg", AudioFileUtils.determineMimeType(File("test.mp3")))
        assertEquals("audio/aac", AudioFileUtils.determineMimeType(File("test.aac")))
        assertEquals("audio/mp4", AudioFileUtils.determineMimeType(File("test.m4a")))
        assertEquals("audio/wav", AudioFileUtils.determineMimeType(File("test.wav")))
        assertEquals("audio/ogg", AudioFileUtils.determineMimeType(File("test.ogg")))
        assertEquals("audio/mpeg", AudioFileUtils.determineMimeType(File("test.unknown")))
    }

    @Test
    fun testInspectAudioFile_nonExistent() {
        val nonExistentTape = Cassette(
            id = "tape_missing",
            title = "Fehlende Kassette",
            filePath = File(recordingsDir, "tape_missing.mp3").absolutePath
        )

        val info = AudioFileUtils.inspectAudioFile(context, nonExistentTape)
        assertFalse("File should not exist", info.exists)
        assertFalse("File should not be valid for playback", info.isValidAudioFile)
        assertEquals(0L, info.sizeBytes)
        assertEquals("0 Bytes", info.formattedSize)
        assertTrue(info.statusText.contains("nicht gefunden") || info.statusText.contains("fehlt"))
        assertTrue("Should detect internal storage path", info.isInternalStorage)
    }

    @Test
    fun testInspectAudioFile_emptyFile() {
        val emptyFile = File(recordingsDir, "tape_empty.mp3")
        emptyFile.writeBytes(ByteArray(0))

        val emptyTape = Cassette(
            id = "tape_empty",
            title = "Leere Kassette",
            filePath = emptyFile.absolutePath
        )

        val info = AudioFileUtils.inspectAudioFile(context, emptyTape)
        assertTrue("File should exist", info.exists)
        assertEquals(0L, info.sizeBytes)
        assertFalse("Empty file should not be valid for playback", info.isValidAudioFile)
        assertTrue("Status text should mention 0 Bytes or leer", info.statusText.contains("0 Bytes") || info.statusText.contains("leer"))
    }

    @Test
    fun testInspectAudioFile_validRecordedFile() {
        val testData = ByteArray(128 * 1024) { (it % 255).toByte() } // 128 KiB
        val recordedFile = File(recordingsDir, "tape_valid.mp3")
        recordedFile.writeBytes(testData)

        val validTape = Cassette(
            id = "tape_valid",
            title = "Gültiges Mixtape",
            filePath = recordedFile.absolutePath
        )

        val info = AudioFileUtils.inspectAudioFile(context, validTape)
        assertTrue("File should exist", info.exists)
        assertTrue("File should be readable", info.canRead)
        assertTrue("File should be valid", info.isValidAudioFile)
        assertEquals(testData.size.toLong(), info.sizeBytes)
        assertEquals("tape_valid.mp3", info.fileName)
        assertEquals("audio/mpeg", info.mimeType)
        assertTrue("Status text should be Vorhanden & lesbar", info.statusText.contains("Vorhanden"))
        assertTrue(info.isInternalStorage)
    }

    @Test
    fun testOpenInExternalPlayer_withValidFile_createsChooserIntentWithFileProvider() {
        val testData = "Fake MP3 Audio Header & Bytes".toByteArray()
        val recordedFile = File(recordingsDir, "tape_intent.mp3")
        recordedFile.writeBytes(testData)

        val validTape = Cassette(
            id = "tape_intent",
            title = "Intent Test Mixtape",
            filePath = recordedFile.absolutePath
        )

        val result = AudioFileUtils.openInExternalPlayer(context, validTape)
        assertTrue("Result should be Success", result is AudioFileUtils.OpenResult.Success)

        // Verify with Robolectric shadow application
        val shadowApp = shadowOf(context as android.app.Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("An activity intent must have been launched", startedIntent)

        // It should be a chooser intent
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val targetIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Target intent in chooser must exist", targetIntent)

        assertEquals(Intent.ACTION_VIEW, targetIntent?.action)
        assertEquals("audio/mpeg", targetIntent?.type)
        val uri = targetIntent?.data
        assertNotNull("URI must be present", uri)
        assertEquals("content", uri?.scheme)
        assertTrue("URI authority must match fileprovider", uri?.authority?.endsWith(".fileprovider") == true)
        assertTrue("FLAG_GRANT_READ_URI_PERMISSION must be set", (targetIntent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun testOpenInExternalPlayer_withMissingFile_returnsError() {
        val missingTape = Cassette(
            id = "tape_does_not_exist",
            title = "Fehlend",
            filePath = File(recordingsDir, "non_existent.mp3").absolutePath
        )

        val result = AudioFileUtils.openInExternalPlayer(context, missingTape)
        assertTrue("Result should be Error", result is AudioFileUtils.OpenResult.Error)
        val errorMsg = (result as AudioFileUtils.OpenResult.Error).message
        assertTrue("Error message should mention missing file", errorMsg.contains("nicht gefunden"))
    }
}
