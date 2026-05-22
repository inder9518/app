package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.NewsApp
import com.example.ui.NewsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class NewsAppLaunchTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun app_launch_test() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = NewsViewModel(application)
        
        composeTestRule.setContent {
            MyApplicationTheme {
                NewsApp(viewModel = viewModel)
            }
        }

        // 1. Capture feed section (News tab)
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/news_app_launch_feed.png")

        // 2. Navigate and capture generator section (Topics tab)
        composeTestRule.onNodeWithTag("tab_generator").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/news_app_launch_generator.png")

        // 3. Navigate and capture game section (Saved tab)
        composeTestRule.onNodeWithTag("tab_game").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/news_app_launch_game.png")
    }
}

