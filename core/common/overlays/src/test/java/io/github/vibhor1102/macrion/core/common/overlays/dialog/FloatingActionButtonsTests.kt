/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.getOrNull
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.FloatingActionButtonsView
import org.junit.Assert.assertEquals
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
class FloatingActionButtonsTests {
    @Test fun tabSwitchWithUnchangedAppearanceUsesCurrentAddAndCopyHandlers() {
        val calls = mutableListOf<String>()
        withRenderedButtons(
            configure = { buttons ->
                buttons.configure(R.drawable.ic_add, R.drawable.ic_copy,
                    { calls.add("image add") }, { calls.add("image copy") })
                buttons.setSecondaryVisible(true)
            },
        ) { buttons, clickAll ->
            clickAll(2)
            assertEquals(setOf("image add", "image copy"), calls.toSet())
            calls.clear()

            // Both tabs have the same icons and Copy visibility once triggers exist.
            buttons.configure(R.drawable.ic_add, R.drawable.ic_copy,
                { calls.add("trigger add") }, { calls.add("trigger copy") })
            buttons.setSecondaryVisible(true)
            pumpFrames()
            clickAll(2)
            assertEquals(setOf("trigger add", "trigger copy"), calls.toSet())
        }
    }

    @Test fun unchangedTertiaryButtonUsesItsReplacementHandler() {
        val calls = mutableListOf<String>()
        withRenderedButtons(
            configure = { buttons ->
                buttons.setTertiary(R.drawable.ic_create_new_folder, true) { calls.add("old") }
            },
        ) { buttons, clickAll ->
            clickAll(2) // The primary button is also rendered, with its default no-op handler.
            assertEquals(listOf("old"), calls)
            calls.clear()
            buttons.setTertiary(R.drawable.ic_create_new_folder, true) { calls.add("new") }
            pumpFrames()
            clickAll(2)
            assertEquals(listOf("new"), calls)
        }
    }

    private fun withRenderedButtons(
        configure: (FloatingActionButtonsView) -> Unit,
        check: (FloatingActionButtonsView, (Int) -> Unit) -> Unit,
    ) {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val buttons = FloatingActionButtonsView(activity).apply {
            configure(this)
            isVisible = true
        }
        val host = ComposeView(activity).apply { setContent { buttons.Content() } }
        try {
            activity.setContentView(host)
            controller.visible()
            pumpFrames()
            // Read the rendered click actions without adding a Compose UI test dependency.
            val ownerView = descendants(host).first { view ->
                view.javaClass.methods.any { it.name == "getSemanticsOwner" }
            }
            val owner = ownerView.javaClass.getMethod("getSemanticsOwner").invoke(ownerView) as SemanticsOwner
            check(buttons) { expectedCount ->
                val actions = semantics(owner.rootSemanticsNode).mapNotNull {
                    it.config.getOrNull(SemanticsActions.OnClick)?.action
                }.toList()
                assertEquals(expectedCount, actions.size)
                actions.forEach { it() }
            }
        } finally {
            host.disposeComposition()
            controller.pause().stop().destroy()
        }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
        }
    }

    private fun semantics(node: SemanticsNode): Sequence<SemanticsNode> = sequence {
        yield(node)
        node.children.forEach { yieldAll(semantics(it)) }
    }

    private fun pumpFrames() {
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        repeat(10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)) }
    }
}
