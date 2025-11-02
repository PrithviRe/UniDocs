package com.example.unidocs

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Assert.*

/**
 * Instrumented tests for UniDocs application.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.unidocs", appContext.packageName)
    }
    
    @Test
    fun appNameIsCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appName = appContext.getString(R.string.app_name)
        assertEquals("UniDocs", appName)
    }
}