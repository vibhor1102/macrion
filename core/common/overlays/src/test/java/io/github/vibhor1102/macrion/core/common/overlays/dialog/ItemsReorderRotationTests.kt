/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ItemsReorderDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ReorderItemDescriptor
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ItemsReorderRotationTests {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun viewRecreationPreservesPendingOrderAndDiscardState() {
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
            // The rule advances Compose's clock and drains layout/recomposition work.
            // A wall-clock deadline with manual Choreographer pumping races this work in CI.
            compose.runOnIdle {
                assertEquals(original, dialog.displayedItems)
                assertFalse(dialog.hasUnsavedChanges)
                dialog.moveItem(0, 2)
            }
            compose.runOnIdle {
                assertTrue(dialog.hasUnsavedChanges)
                host?.disposeComposition()
                host = createView.invoke(dialog) as ComposeView
                activity.setContentView(host)
            }
            compose.runOnIdle {
                assertTrue(dialog.hasUnsavedChanges)
                assertEquals(listOf(original[1], original[2], original[0]), dialog.displayedItems)
                assertTrue("Reordering remains pending until Save", saved.isEmpty())
            }
            val labels = compose.onAllNodes(
                hasText("first") or hasText("second") or hasText("third"),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().flatMap { node ->
                node.config[SemanticsProperties.Text].map { it.text }
            }
            assertEquals(listOf("second", "third", "first"), labels)
        } finally {
            host?.disposeComposition()
            controller.pause().stop().destroy()
        }
    }
}
