package com.auralis.player

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.auralis.player.home.HomeSettings
import com.auralis.player.music.MusicType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {
    @Test
    fun testLaunchMainActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
                println("MainActivity launched and loaded successfully!")
            }
        }
    }

    @Test
    fun testLaunchWithPreferredMode() {
        val app = ApplicationProvider.getApplicationContext<Auralis>()
        app.homeSettings.preferredMode = MusicType.SONGS
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
                println("MainActivity with preferredMode launched successfully!")
            }
        }
    }

    @Test
    fun testStartAuralisService() {
        val controller = Robolectric.buildService(AuralisService::class.java)
        controller.create()
        controller.startCommand(0, 0)
        controller.destroy()
    }
}
