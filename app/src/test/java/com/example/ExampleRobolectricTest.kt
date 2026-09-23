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
    assertEquals("Smart Lecture Notes", appName)
  }

  @Test
  fun `verify theme preferences male and female switching`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val themePrefs = com.example.data.local.ThemePreferences(context)
    
    // Test male to female transition
    themePrefs.setGender(com.example.data.local.UserGender.FEMALE)
    assertEquals(com.example.data.local.UserGender.FEMALE, themePrefs.gender.value)

    // Test female to male transition
    themePrefs.setGender(com.example.data.local.UserGender.MALE)
    assertEquals(com.example.data.local.UserGender.MALE, themePrefs.gender.value)

    // Test day/night modes
    themePrefs.setThemeMode(com.example.data.local.ThemeMode.DARK)
    assertEquals(com.example.data.local.ThemeMode.DARK, themePrefs.themeMode.value)

    themePrefs.setThemeMode(com.example.data.local.ThemeMode.LIGHT)
    assertEquals(com.example.data.local.ThemeMode.LIGHT, themePrefs.themeMode.value)
  }
}
