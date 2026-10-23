/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ItemsReorderDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ReorderItemDescriptor
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ItemsReorderRotationTests {
    @Test fun viewRecreationPreservesPendingOrderAndDiscardState() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val original = listOf("first", "second", "third").mapIndexed { index, label ->
            ItemBrief(Identifier(databaseId = index + 1L), label)
        }
        val saved = mutableListOf<List<ItemBrief>>()
        val dialog = ItemsReorderDialog(
            theme = io.github.vibhor1102.macrion.core.ui.R.style.AppTheme,
            titleRes = io.github.vibhor1102.macrion.core.common.overlays.R.string.dialog_reorder_empty,
            itemsFlow = flowOf(original),
            itemDescriptor = { ReorderItemDescriptor(it.data as String) },
            onSaveOrder = { saved.add(it) },
        ).apply { context = activity }
        val createView = ItemsReorderDialog::class.java.getDeclaredMethod("onCreateView").apply { isAccessible = true }
        var host: ComposeView? = null
        try {
            host = createView.invoke(dialog) as ComposeView
            activity.setContentView(host)
            controller.visible()
            pumpUntil { dialog.initialItems != null }
            assertEquals(original, dialog.displayedItems)
            assertFalse(dialog.hasUnsavedChanges)
            dialog.moveItem(0, 2)
            pumpFrames()
            assertTrue(dialog.hasUnsavedChanges)
            host.disposeComposition()
            host = createView.invoke(dialog) as ComposeView
            activity.setContentView(host)
            pumpFrames()
            assertTrue(dialog.hasUnsavedChanges)
            assertEquals(listOf(original[1], original[2], original[0]), dialog.displayedItems)
            assertTrue("Reordering remains pending until Save", saved.isEmpty())
            val ownerView = descendants(host).first { view -> view.javaClass.methods.any { it.name == "getSemanticsOwner" } }
            val owner = ownerView.javaClass.getMethod("getSemanticsOwner").invoke(ownerView) as SemanticsOwner
            val labels = semantics(owner.rootSemanticsNode).flatMap {
                it.config.getOrNull(SemanticsProperties.Text).orEmpty().asSequence()
            }.map { it.text }.filter { it in listOf("first", "second", "third") }.toList()
            assertEquals(listOf("second", "third", "first"), labels)
        } finally {
            host?.disposeComposition()
            controller.pause().stop().destroy()
        }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }
    private fun semantics(node: SemanticsNode): Sequence<SemanticsNode> = sequence {
        yield(node)
        node.children.forEach { yieldAll(semantics(it)) }
    }
    private fun pumpUntil(ready: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000L
        do {
            pumpFrames()
            if (ready()) return
            Thread.sleep(10) // Snapshot apply notifications can arrive from the background dispatcher.
        } while (System.nanoTime() < deadline)
        assertTrue("The initial list must load before a drag can start", ready())
    }

    private fun pumpFrames() {
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        repeat(10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)) }
    }
}
