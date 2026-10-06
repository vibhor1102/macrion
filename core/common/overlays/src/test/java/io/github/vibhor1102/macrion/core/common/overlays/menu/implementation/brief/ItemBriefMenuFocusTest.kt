/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief

import android.content.res.Configuration
import android.graphics.Point
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import dagger.hilt.EntryPoints
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.display.di.DisplayEntryPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ItemBriefMenuFocusTest {
    private val binding = mockk<ItemsBriefOverlayViewBinding>(relaxed = true)
    private lateinit var controls: ItemBriefControlsState
    private val a = item(1)
    private val b = item(2)
    private val c = item(3)
    private val d = item(4)

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        val displayManager = mockk<DisplayConfigManager>()
        every { displayManager.displayConfig } returns DisplayConfig(
            sizePx = Point(1080, 1920),
            orientation = Configuration.ORIENTATION_PORTRAIT,
            safeInsetTopPx = 0,
            roundedCorners = emptyMap(),
        )
        val entryPoint = mockk<DisplayEntryPoint>()
        every { entryPoint.displayMetrics() } returns displayManager
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(app, DisplayEntryPoint::class.java) } returns entryPoint
        every { binding.updateControls(any()) } answers { controls = firstArg() }
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun firstActionInsertsAtZeroBeforeAnyPagerCallback() {
        val menu = menu()
        assertEquals(-1, menu.focusedIndex())
        assertEquals(0, menu.insertionIndex())
        menu.update(emptyList())
        assertEquals(0, menu.insertionIndex())
        assertNull(menu.focusedItem())
        assertFalse(controls.canPlay)
        assertFalse(controls.canDelete)

        // Exercise the same insert-after-focus calculation used by the simple action save callback.
        val actions = mutableListOf<ItemBrief>()
        actions.add(menu.insertionIndex(), a)
        menu.update(actions)
        assertEquals(a, menu.focusedItem())
        assertEquals(listOf(0), menu.focusNotifications)
        assertTrue(controls.canPlay)
    }

    @Test
    fun deletingLastActionAllowsAnotherFirstAction() {
        val menu = menu()
        menu.update(listOf(a))
        menu.update(emptyList())
        assertEquals(-1, menu.focusedIndex())
        assertEquals(0, menu.insertionIndex())
        assertNull(menu.focusedItem())
        assertFalse(controls.canDelete)
        assertFalse(controls.canPlay)
        menu.update(listOf(b))
        assertEquals(b, menu.focusedItem())
        assertEquals(listOf(0, -1, 0), menu.focusNotifications)
    }

    @Test
    fun insertionAfterMiddleCardFocusesNewCardWithoutPager() {
        val menu = menu(initialIndex = 1)
        menu.update(listOf(a, b, c))
        val actions = mutableListOf(a, b, c)
        actions.add(menu.insertionIndex(), d)
        menu.update(actions)
        assertEquals(listOf(a, b, d, c), actions)
        assertEquals(d, menu.focusedItem())
        assertEquals(listOf(1, 2), menu.focusNotifications)
    }

    @Test
    fun insertionAfterLastCardAppendsAndFocusesNewCard() {
        val menu = menu(initialIndex = 2)
        menu.update(listOf(a, b, c))
        assertEquals(3, menu.insertionIndex())
        menu.update(listOf(a, b, c, d))
        assertEquals(d, menu.focusedItem())
        assertEquals(listOf(2, 3), menu.focusNotifications)
    }

    @Test
    fun appendWithoutIndexFocusesNewCardWhilePanelIsAbsent() {
        val menu = menu()
        menu.update(listOf(a, b))
        menu.update(listOf(a, b, c))
        assertEquals(c, menu.focusedItem())
        assertEquals(listOf(0, 2), menu.focusNotifications)
    }

    @Test
    fun deletionBeforeFocusPreservesSelectedItemAndNotifiesPreview() {
        val menu = menu(initialIndex = 2)
        menu.update(listOf(a, b, c))
        menu.update(listOf(b, c))
        assertEquals(c, menu.focusedItem())
        assertEquals(listOf(2, 1), menu.focusNotifications)
    }

    @Test
    fun deletingFocusedLastCardSelectsPreviousCardWithoutPager() {
        val menu = menu(initialIndex = 2)
        menu.update(listOf(a, b, c))
        menu.update(listOf(a, b))
        assertEquals(b, menu.focusedItem())
        assertEquals(listOf(2, 1), menu.focusNotifications)
    }

    @Test
    fun deletingFocusedMiddleCardSelectsNextCardAtSamePosition() {
        val menu = menu(initialIndex = 1)
        menu.update(listOf(a, b, c))
        menu.update(listOf(a, c))
        assertEquals(1, menu.focusedIndex())
        assertEquals(c, menu.focusedItem())
    }

    @Test
    fun reorderRetainsCardPosition() {
        val menu = menu(initialIndex = 1)
        menu.update(listOf(a, b, c))
        menu.update(listOf(b, c, a))
        assertEquals(1, menu.focusedIndex())
        assertEquals(c, menu.focusedItem())
    }

    @Test
    fun contentOnlyUpdateDoesNotResetFocusOrRecordingState() {
        val menu = menu(initialIndex = 1)
        menu.update(listOf(a, b, c))
        menu.focusNotifications.clear()
        menu.update(listOf(a, b.copy(data = "Updated details"), c))
        assertEquals(1, menu.focusedIndex())
        assertTrue(menu.focusNotifications.isEmpty())
    }

    @Test
    fun initialFocusIsClampedToAvailableCards() {
        val menu = menu(initialIndex = 20)
        menu.update(listOf(a, b))
        assertEquals(b, menu.focusedItem())
        assertEquals(listOf(1), menu.focusNotifications)
    }

    private fun menu(initialIndex: Int = 0) = TestMenu(binding, initialIndex).apply {
        context = RuntimeEnvironment.getApplication()
    }

    private fun item(id: Long) = ItemBrief(Identifier(databaseId = id), "Action $id")

    // No Compose pager is attached: list updates must synchronize focus on their own.
    private class TestMenu(binding: ItemsBriefOverlayViewBinding, initialIndex: Int) : ItemBriefMenu(
        noItemText = R.string.item_brief_items_count_port,
        initialItemIndex = initialIndex,
    ) {
        val focusNotifications = mutableListOf<Int>()
        init { briefViewBinding = binding }
        fun update(items: List<ItemBrief>) = updateItemList(items)
        fun focusedIndex() = getFocusedItemIndex()
        fun focusedItem() = getFocusedItemBrief()
        fun insertionIndex() = getFocusedItemIndex() + 1
        override fun onFocusedItemChanged(index: Int) {
            super.onFocusedItemChanged(index)
            focusNotifications.add(index)
        }
        @Composable override fun ItemBriefContent(item: ItemBrief, orientation: Int, onClick: () -> Unit) = Unit
        override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup = error("No menu view needed")
        override fun onPlayItemClicked(index: Int) = Unit
        override fun onDeleteItemClicked(index: Int) = Unit
    }
}
