/*
 * Copyright (C) 2026 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar

import android.app.Dialog
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.DialogNavigation
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.NavBarDialogScaffold
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.DialogNavigationButton
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.TopBarNavigationView
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.FloatingActionButtonsView

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf

/** The Compose-native description of a page in a navigation dialog. */
data class DialogNavigationItem(
    val id: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
)

abstract class NavBarDialog(@StyleRes theme: Int) : OverlayDialog(theme) {

    /** Map of navigation bar item id to their content view. */
    private val contentMap: MutableMap<Int, NavBarDialogContent> = mutableMapOf()

    private lateinit var persistentHeader: FrameLayout
    private lateinit var contentContainer: FrameLayout
    private lateinit var navigationHost: NavigationHostView
    private val missingInputBadges = mutableStateMapOf<Int, Boolean>()
    private var selectedNavigationItemId = mutableIntStateOf(View.NO_ID)
    lateinit var floatingActionButtons: FloatingActionButtonsView
    lateinit var topBarBinding: TopBarNavigationView

    /** Navigation pages are data rather than an Android menu so the visible surface stays Compose-native. */
    abstract fun navigationItems(): List<DialogNavigationItem>

    abstract fun onCreateContent(navItemId: Int): NavBarDialogContent

    abstract fun onDialogButtonPressed(buttonType: DialogNavigationButton)

    open fun onContentViewChanged(navItemId: Int) = Unit

    override fun onCreateView(): ViewGroup {
        topBarBinding = TopBarNavigationView(context).apply {
            setButtonClickListener(DialogNavigationButton.SAVE) { debounceUserInteraction { handleButtonClick(DialogNavigationButton.SAVE) } }
            setButtonClickListener(DialogNavigationButton.DISMISS) { debounceUserInteraction { handleButtonClick(DialogNavigationButton.DISMISS) } }
            setButtonClickListener(DialogNavigationButton.DELETE) { debounceUserInteraction { handleButtonClick(DialogNavigationButton.DELETE) } }
        }
        persistentHeader = FrameLayout(context).apply {
            id = View.generateViewId()
            visibility = View.GONE
        }
        contentContainer = FrameLayout(context).apply { id = View.generateViewId() }

        // The navigation bar is pinned at the bottom in portrait and as a rail at the start in landscape,
        // hosted directly within Compose's NavBarDialogScaffold.
        val navigationItems = navigationItems()
        require(navigationItems.isNotEmpty()) { "A navigation dialog must expose at least one page" }
        selectedNavigationItemId.intValue = navigationItems.first().id
        val isPortrait = displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        navigationHost = NavigationHostView(context, isPortrait).apply {
            id = View.generateViewId()
            if (isPortrait) translationZ = 100 * resources.displayMetrics.density
            setContent {
                MacrionTheme {
                    DialogNavigation(
                        items = navigationItems,
                        selectedItemId = selectedNavigationItemId.intValue,
                        missingInputBadges = missingInputBadges,
                        isPortrait = isPortrait,
                        onItemSelected = ::updateContentView,
                        itemModifier = ::navigationItemModifier,
                    )
                }
            }
        }
        floatingActionButtons = FloatingActionButtonsView(context)

        return ComposeView(context).apply {
            setContent {
                MacrionTheme {
                    NavBarDialogScaffold(
                        topBar = topBarBinding.root,
                        persistentHeader = persistentHeader,
                        content = contentContainer,
                        navBar = navigationHost,
                        floatingActions = floatingActionButtons.root,
                        isPortrait = isPortrait,
                    )
                }
            }
        }
    }

    @CallSuper
    override fun onDialogCreated(dialog: Dialog) {
        updateContentView(
            itemId = selectedNavigationItemId.intValue,
            forceUpdate = true,
        )
    }

    override fun onStart() {
        super.onStart()
        contentMap[selectedNavigationItemId.intValue]?.resume()
    }

    override fun onStop() {
        super.onStop()
        contentMap[selectedNavigationItemId.intValue]?.pause()
    }

    override fun onDestroy() {
        contentMap.values.forEach { content ->
            content.destroy()
        }
        contentMap.clear()
        super.onDestroy()
    }

    protected fun setMissingInputBadge(navItemId: Int, haveMissingInput: Boolean) {
        missingInputBadges[navItemId] = haveMissingInput
    }

    protected fun selectNavigationItem(itemId: Int) {
        updateContentView(itemId)
    }

    /** Compose modifier hook for attaching tutorial anchors to navigation bar/rail items. */
    @androidx.compose.runtime.Composable
    open fun navigationItemModifier(item: DialogNavigationItem): Modifier = Modifier

    /** Adds content that remains visible above every navigation page. */
    protected fun setPersistentHeader(view: View) {
        persistentHeader.apply {
            removeAllViews()
            addView(view)
            visibility = View.VISIBLE
        }
    }

    private fun createContentView(itemId: Int): NavBarDialogContent =
        onCreateContent(itemId).apply {
            create(this@NavBarDialog, contentContainer, itemId)
        }

    private fun updateContentView(itemId: Int, forceUpdate: Boolean = false) {
        if (!forceUpdate && selectedNavigationItemId.intValue == itemId) return

        // Get the current content and stop it, if any.
        contentMap[selectedNavigationItemId.intValue]?.apply {
            pause()
            stop()
        }

        // Get new content. If it does not exist yet, create it.
        var content = contentMap[itemId]
        if (content == null) {
            content = createContentView(itemId)
            contentMap[itemId] = content
        }

        content.start()
        selectedNavigationItemId.intValue = itemId
        onContentViewChanged(itemId)

        floatingActionButtons.root.visibility =
            if (content.floatingActionButtonsAreAvailable()) View.VISIBLE
            else View.GONE

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) content.resume()
    }

    internal fun debounceInteraction(interaction: () -> Unit) {
        debounceUserInteraction(interaction)
    }

    private fun handleButtonClick(buttonType: DialogNavigationButton) {
        // First notify the contents.
        contentMap.values.forEach { contentInfo ->
            contentInfo.onDialogButtonClicked(buttonType)
        }

        // Then, notify the dialog
        onDialogButtonPressed(buttonType)
    }
}
