/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FolderExpansionPreferencesTest {
    @Test
    fun `reopening restores each scenario independently and rapid writes retain the latest state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = FolderExpansionPreferences(context, Dispatchers.IO)
        assertEquals(emptySet<String>(), preferences.observe(101).first())
        preferences.save(101, setOf("Combat"))
        preferences.save(202, setOf("Navigation"))
        preferences.save(101, setOf("Combat", "Loot"))
        withTimeout(5000) {
            assertEquals(setOf("Combat", "Loot"), preferences.observe(101).first { it.size == 2 })
            assertEquals(setOf("Navigation"), preferences.observe(202).first { it.isNotEmpty() })
        }
        // A new observer represents a reopened dialog; expanding everything removes the preference.
        assertEquals(setOf("Combat", "Loot"), preferences.observe(101).first())
        preferences.save(101, emptySet())
        withTimeout(5000) { assertEquals(emptySet<String>(), preferences.observe(101).first { it.isEmpty() }) }
        assertEquals(setOf("Navigation"), preferences.observe(202).first())
    }
}
