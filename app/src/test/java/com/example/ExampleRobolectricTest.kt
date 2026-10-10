package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tedoki Mixtape", appName)
  }

  @Test
  fun `test audioEngine radio initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val audioEngine = com.example.audio.AudioEngine(context)
    println("AudioEngine created, playback source: ${audioEngine.playbackSource.value}")
    audioEngine.setPlaybackSource(com.example.model.PlaybackSource.RADIO)
    println("Switched to RADIO, isRadioPlaying: ${audioEngine.isRadioPlaying.value}, station: ${audioEngine.currentStation.value?.name}")
    audioEngine.release()
  }
}
