package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.VpnSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app name matches NOVA VPN`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NOVA VPN", appName)
  }

  @Test
  fun `navigation tabs render and switch to locations and settings`() {
    composeTestRule.setContent {
      NovaVpnRoot()
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("connect_orb_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("connection_status_badge").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_locations").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("locations_search_input").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lang_english_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lang_arabic_chip").assertIsDisplayed()
  }
}
