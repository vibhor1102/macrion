/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.common.overlays.menu

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import kotlin.math.hypot
import kotlin.math.roundToInt

import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import io.github.vibhor1102.macrion.core.base.addDumpTabulationLvl
import io.github.vibhor1102.macrion.core.base.extensions.disableMoveAnimations
import io.github.vibhor1102.macrion.core.base.extensions.doWhenMeasured
import io.github.vibhor1102.macrion.core.base.extensions.enableMoveAnimations
import io.github.vibhor1102.macrion.core.base.extensions.safeAddView
import io.github.vibhor1102.macrion.core.base.extensions.safeRemoveView
import io.github.vibhor1102.macrion.core.base.extensions.safeUpdateViewLayout
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import io.github.vibhor1102.macrion.core.common.overlays.di.OverlaysEntryPoint
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common.OverlayDismissTargetController
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common.OverlayMenuAnimations
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common.OverlayMenuMoveTouchEventHandler
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import io.github.vibhor1102.macrion.core.common.overlays.scale.OverlayScaleProvider

import dagger.hilt.EntryPoints
import java.io.PrintWriter

/**
 * Controller for a menu displayed as an overlay shown from a service.
 *
 * This class ensure that all overlay menu opened from a service will have the same behaviour. It provides basic
 * lifecycle alike methods to ease the view initialization/cleaning, as well as a menu item enabling/disabling
 * management and the moving of the menu by pressing the move item. It also provides the management of an overlay view,
 * a view that can be shown/hide as an overlay over the currently displayed activity.
 *
 * Create the menu with [createOverlayMenuLayout]. Its registry supplies stable interaction and
 * tutorial anchors before the Compose hierarchy attaches. Compose owns layout and resizing;
 * this controller owns the platform windows, lifecycle and saved position.
 *
 * Two menu items are supported by default and are not mandatory (if you don't need it, don't declare it in your layout).
 * Include these items in the menu's button definitions to enable their behavior:
 * - [R.id.btn_move]: the button allowing the move the overlay menu when drag and drop by the user.
 * - [R.id.btn_hide_overlay]: the button allowing to show/hide the overlay view on the screen. When hidden, the user can
 * click on the activity overlaid.
 *
 * The overlay view is created by the abstract method [onCreateOverlayView]. This view can be shown/hidden on a press by
 * the user on the [R.id.btn_hide_overlay] button.
 *
 * The position of the menu is saved in the [android.content.SharedPreferences] for each orientation.
 */
abstract class OverlayMenu(
    @StyleRes theme: Int? = null,
    private val recreateOverlayViewOnRotation: Boolean = false,
) : BaseOverlay(theme = theme, recreateOnRotation = false) {

    /** The base layout parameters of the menu layout & overlay view. */
    private val baseLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        OverlayManager.OVERLAY_WINDOW_TYPE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        disableMoveAnimations()
    }

    /** The layout parameters of the menu layout. */
    private val menuLayoutParams: WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply { copyFrom(baseLayoutParams) }

    private val animations: OverlayMenuAnimations = OverlayMenuAnimations()
    private val overlayScaleProvider: OverlayScaleProvider by lazy {
        EntryPoints.get(context.applicationContext, OverlaysEntryPoint::class.java)
            .overlayScaleProvider()
    }

    private val scaledDensity: Float
        get() {
            val scale = try {
                overlayScaleProvider.getScale()
            } catch (_: Exception) {
                1f
            }
            return context.resources.displayMetrics.density * scale
        }

    /** Tracks whether an orientation change occurred while this overlay menu was not started/visible. */
    private var pendingOrientationChange: Boolean = false
    private var lastAppliedOrientation: Int = Configuration.ORIENTATION_UNDEFINED

    internal var resumeOnceShown: Boolean = false
        private set
    internal var destroyOnceHidden: Boolean = false
        private set

    /** The Android window manager. Used to add/remove the overlay menu and view. */
    private lateinit var windowManager: WindowManager

    /** The root view of the menu overlay. Retrieved from [onCreateMenu] implementation. */
    private lateinit var menuLayout: ViewGroup
    /** The view displaying the background of the overlay. */
    private lateinit var menuBackground: ViewGroup
    /** Handles the touch events on the move button. */
    private lateinit var moveTouchEventHandler: OverlayMenuMoveTouchEventHandler

    /** Handles the save/load of the position of the menus. */
    private val positionDataSource: OverlayMenuPositionDataSource by lazy {
        EntryPoints.get(context.applicationContext, OverlaysEntryPoint::class.java)
            .overlayMenuPositionDataSource()
    }

    /** Value of the alpha for a disabled item view in the menu. */
    private var disabledItemAlpha: Float = 1f

    /** The hide overlay button, if provided. */
    private var hideOverlayButton: OverlayMenuButtonView? = null
    /** The move button, if provided. */
    private var moveButton: View? = null

    var isMenuTucked: Boolean = false
        private set
    private var wasTuckedBeforeStop: Boolean = false

    protected open fun onMenuTuckedChanged(isTucked: Boolean) {}
    protected open fun onUserInteraction() {}

    /** Whether dragging the tucked overlay allows dismissing the scenario. */
    protected open val isDragToDismissEnabled: Boolean = false

    /** Called when the tucked overlay is dragged into the dismiss target and released. */
    protected open fun onTuckedDismiss() {}

    /**
     * Whether the overlay view is intended to be visible by the user (toggled via the hide-overlay button).
     * Preserved across rotation and temporary overlay hiding.
     */
    protected var isUserOverlayVisible: Boolean = true
        private set

    /**
     * The view to be displayed between the current activity and the overlay menu.
     * It can be shown/hidden by pressing on the menu item with the id [R.id.btn_hide_overlay]. If null, pressing this
     * button will have no effect.
     */
    protected var screenOverlayView: View? = null
    /** The layout parameters of the overlay view. */
    private lateinit var overlayLayoutParams: WindowManager.LayoutParams

    private val onLockedPositionChangedListener: (Point?) -> Unit = ::onLockedPositionChanged

    /**
     * Creates the root view of the menu overlay.
     *
     * @param layoutInflater the Android layout inflater.
     *
     * @return the window host returned by [createOverlayMenuLayout].
     */
    protected abstract fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup

    /** Optional exact width for menus whose content exceeds WindowManager's wrap-content limit. */
    protected open fun getMenuWindowWidth(): Int = WindowManager.LayoutParams.WRAP_CONTENT

    /**
     * Creates the view to be displayed between the current activity and the overlay menu.
     * It can be shown/hidden by pressing on the menu item with the id [R.id.btn_hide_overlay]. If null, pressing this
     * button will have no effect.
     *
     * @return the overlay view, or null if none is required.
     */
    protected open fun onCreateOverlayView(): View? = null

    /** Tells if the overlay view should be animated when shown/hidden. True by default. */
    protected open fun animateOverlayView(): Boolean = true

    /**
     * Creates the layout parameters for the [screenOverlayView].
     * Default implementation uses the same parameters as the floating menu, but in fullscreen.
     *
     * @return the layout parameters to apply to the overlay view.
     */
    protected open fun onCreateOverlayViewLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        copyFrom(baseLayoutParams)
        displayConfigManager.displayConfig.sizePx.let { size ->
            width = size.x
            height = size.y
        }
    }

    @CallSuper
    @SuppressLint("ResourceType")
    override fun onCreate() {
        windowManager = context.getSystemService(WindowManager::class.java)!!
        disabledItemAlpha = context.resources.getFraction(R.dimen.alpha_menu_item_disabled, 1, 1)

        // First, call implementation methods to check what we should display
        menuLayout = onCreateMenu(context.getSystemService(LayoutInflater::class.java))
        menuLayoutParams.width = getMenuWindowWidth()
        screenOverlayView = onCreateOverlayView()
        overlayLayoutParams = onCreateOverlayViewLayoutParams()

        // WindowManager overlay roots don't receive the Activity view-tree owners. Propagate this
        // overlay's lifecycle so ComposeView children can create and dispose their recomposer safely.
        menuLayout.installOverlayViewTreeOwners()
        screenOverlayView?.installOverlayViewTreeOwners()

        // Set the clicks listener on the menu items
        menuBackground = menuLayout.findOverlayView(R.id.menu_background)
        setupButtons()

        // Setup the touch event handler for the move button
        moveTouchEventHandler = OverlayMenuMoveTouchEventHandler(::updateMenuPosition)

        // Restore the last menu position, if any.
        menuLayoutParams.gravity = Gravity.TOP or Gravity.START
        overlayLayoutParams.gravity = Gravity.TOP or Gravity.START
        positionDataSource.addOnLockedPositionChangedListener(onLockedPositionChangedListener)
        lastAppliedOrientation = displayConfigManager.displayConfig.orientation
        loadMenuPosition(lastAppliedOrientation)
        moveButton?.isVisible = !positionDataSource.isPositionLocked()

        // Add the overlay, if any. It needs to be below the menu or user won't be able to click on the menu.
        screenOverlayView?.let {
            if (animateOverlayView()) it.visibility = View.GONE
            if (!windowManager.safeAddView(it, overlayLayoutParams)) {
                finish()
                return
            }
        }

        // Add the menu view to the window manager, but hidden
        // Keep the background measurable while attaching. Compose content needs a window in order
        // to obtain its recomposer during the maximum-size measurement below.
        if (animateOverlayView()) menuBackground.visibility = View.INVISIBLE
        if (!windowManager.safeAddView(menuLayout, menuLayoutParams)) {
            finish()
            return
        }

        // Compose measures and animates the content; WindowManager follows its wrap-content size.
        if (animateOverlayView()) menuBackground.visibility = View.GONE

        menuLayout.addOnLayoutChangeListener(onMenuLayoutChangeListener)
    }

    private val onMenuLayoutChangeListener = View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (isMenuTucked) return@OnLayoutChangeListener
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
            updateMenuPosition(Point(menuLayoutParams.x, menuLayoutParams.y))
        }
    }

    private fun View.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(this@OverlayMenu)
        setViewTreeSavedStateRegistryOwner(this@OverlayMenu)
        setViewTreeViewModelStoreOwner(this@OverlayMenu)
    }

    private fun setupButtons() {
        val host = menuLayout as ComposeOverlayMenuHost
        host.onUntuckRequested = {
            onUserInteraction()
            untuckMenu()
        }
        host.onTuckedTouch = ::handleTuckedTouchEvent
        host.buttons.forEach { view ->
            @SuppressLint("ClickableViewAccessibility") // View is only drag and drop, no click
            when (view.id) {
                R.id.btn_move -> {
                    moveButton = view
                    view.setOnTouchListener { _: View, event: MotionEvent -> onMoveTouched(event) }
                }
                R.id.btn_hide_overlay -> {
                    hideOverlayButton = view
                    setOverlayViewVisibility(isUserOverlayVisible)
                    view.setOnClickListener {
                        if (lifecycle.currentState != Lifecycle.State.RESUMED) return@setOnClickListener
                        onUserInteraction()
                        onToggleOverlayVisibilityClicked()
                    }
                }
                else -> {
                    val onClick: (View) -> Unit = { clickedView ->
                        onUserInteraction()
                        onMenuItemClicked(clickedView.id)
                    }
                    if (shouldDebounceMenuItemClick(view.id)) {
                        view.setDebouncedOnClickListener(onClick)
                    } else {
                        view.setOnClickListener { clickedView ->
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) onClick(clickedView)
                        }
                    }
                }
            }
        }
    }

    final override fun start() {
        if (lifecycle.currentState != Lifecycle.State.CREATED) return
        if (animations.showAnimationIsRunning) return

        // WindowManager.addView schedules attachment. A navigation request can call start before
        // that attachment is complete, while the show animation immediately measures its target.
        // Unattached ComposeView children cannot resolve a window recomposer during that measure.
        if (!menuLayout.isAttachedToWindow) {
            menuLayout.post(::start)
            return
        }

        val currentOrientation = displayConfigManager.displayConfig.orientation
        if (pendingOrientationChange || (lastAppliedOrientation != Configuration.ORIENTATION_UNDEFINED && lastAppliedOrientation != currentOrientation)) {
            pendingOrientationChange = false
            lastAppliedOrientation = currentOrientation
            loadMenuPosition(currentOrientation)
            applyOrientationChangeToViews()
        }

        super.start()
        loadMenuPosition(displayConfigManager.displayConfig.orientation)

        // Start the show animation for the menu
        Log.d(TAG, "Start show overlay ${hashCode()} animation...")

        val animatedOverlayView = if (animateOverlayView()) screenOverlayView else null
        menuLayout.visibility = View.VISIBLE
        menuBackground.visibility = View.VISIBLE
        animatedOverlayView?.visibility = View.VISIBLE
        animations.startShowAnimation(menuBackground, animatedOverlayView) {
            Log.d(TAG, "Show overlay ${hashCode()} animation ended")

            if (resumeOnceShown) {
                resumeOnceShown = false
                resume()
            }
        }
    }

    final override fun resume() {
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            start()

            // start() can be deferred until WindowManager attaches the menu. Preserve this resume
            // request so the posted start can complete the lifecycle transition after its show
            // animation. Without it, all debounced menu clicks are ignored in STARTED state.
            if (lifecycle.currentState == Lifecycle.State.CREATED) {
                resumeOnceShown = true
                return
            }
        }
        if (lifecycle.currentState != Lifecycle.State.STARTED) return

        if (animations.showAnimationIsRunning) {
            Log.d(TAG, "Show overlay ${hashCode()} animation is running, delaying resume...")
            resumeOnceShown = true
            return
        }

        forceWindowResize()

        super.resume()
    }

    final override fun stop() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        dismissTargetController?.destroy()
        dismissTargetController = null
        isTuckedHoveringDismiss = false
        isTuckedDragging = false
        if (animations.hideAnimationIsRunning) return
        val wasTucked = isMenuTucked
        wasTuckedBeforeStop = wasTucked
        if (lifecycle.currentState == Lifecycle.State.RESUMED) pause()

        if (!wasTucked) {
            saveMenuPosition(displayConfigManager.displayConfig.orientation)
        }

        // Start the hide animation for the menu
        Log.d(TAG, "Start overlay ${hashCode()} hide animation...")
        val animatedOverlayView = if (animateOverlayView()) screenOverlayView else null
        animations.startHideAnimation(menuBackground, animatedOverlayView) {
            Log.d(TAG, "Hide overlay ${hashCode()} animation ended")

            menuLayout.visibility = View.GONE
            menuBackground.visibility = View.GONE
            screenOverlayView?.visibility = View.GONE

            if (wasTucked) {
                isMenuTucked = false
                (menuLayout as? ComposeOverlayMenuHost)?.isTucked = false
                onMenuTuckedChanged(false)
            }

            super.stop()

            if (destroyOnceHidden) {
                destroyOnceHidden = false
                destroy()
            }
        }
    }

    final override fun destroy() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        dismissTargetController?.destroy()
        dismissTargetController = null
        isTuckedHoveringDismiss = false
        isTuckedDragging = false
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) stop()

        if (animations.hideAnimationIsRunning) {
            Log.d(TAG, "Hide overlay ${hashCode()} animation is running, delaying destroy...")
            destroyOnceHidden = true
            return
        }

        // Save last user position
        positionDataSource.removeOnLockedPositionChangedListener(onLockedPositionChangedListener)
        menuLayout.removeOnLayoutChangeListener(onMenuLayoutChangeListener)
        if (!isMenuTucked && !wasTuckedBeforeStop) {
            saveMenuPosition(displayConfigManager.displayConfig.orientation)
        }

        windowManager.safeRemoveView(menuLayout)
        screenOverlayView?.let { windowManager.safeRemoveView(it) }
        screenOverlayView = null

        animations.release()
        super@OverlayMenu.destroy()
    }

    /**
     * Handles the screen orientation changes.
     * It will save the menu position for the previous orientation and load and apply the correct position for the new
     * orientation.
     */
    override fun onOrientationChanged() {
        dismissTargetController?.destroy()
        dismissTargetController = null
        isTuckedHoveringDismiss = false
        isTuckedDragging = false
        val wasTucked = isMenuTucked
        val currentOrientation = displayConfigManager.displayConfig.orientation
        val previousOrientation =
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) Configuration.ORIENTATION_PORTRAIT
            else Configuration.ORIENTATION_LANDSCAPE

        if (!wasTucked) {
            saveMenuPosition(previousOrientation)
            loadMenuPosition(currentOrientation)
        } else {
            updateTuckedPositionForNewOrientation(currentOrientation)
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            applyOrientationChangeToViews()
            lastAppliedOrientation = currentOrientation
            pendingOrientationChange = false
        } else {
            Log.d(TAG, "Overlay menu ${hashCode()} is hidden (lifecycle=${lifecycle.currentState}), deferring orientation change")
            pendingOrientationChange = true
        }
    }

    private fun applyOrientationChangeToViews() {
        windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)

        val overlayView = screenOverlayView ?: return
        if (recreateOverlayViewOnRotation) {
            recreateOverlayViewForRotation(overlayView)
            return
        }

        displayConfigManager.displayConfig.sizePx.let { size ->
            overlayLayoutParams.width = size.x
            overlayLayoutParams.height = size.y
        }
        windowManager.safeUpdateViewLayout(overlayView, overlayLayoutParams)
    }

    /**
     * Recreates the overlay view after a screen rotation.
     * As the Z order is dependant to the addition index in the WindowManager, we need to remove
     * the menu and add it AFTER the new overlay view.
     *
     * @param oldOverlayView the overlay view before the rotation.
     */
    private fun recreateOverlayViewForRotation(oldOverlayView: View) {
        val previousState = lifecycle.currentState
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        screenOverlayView = onCreateOverlayView()?.apply {
            visibility = if (previousState.isAtLeast(Lifecycle.State.STARTED) && isUserOverlayVisible) View.VISIBLE else View.GONE
        }
        screenOverlayView?.installOverlayViewTreeOwners()
        overlayLayoutParams = onCreateOverlayViewLayoutParams().apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.apply {
            safeRemoveView(oldOverlayView)
            safeRemoveView(menuLayout)
            screenOverlayView?.let { overlayView ->
                if (!safeAddView(overlayView, overlayLayoutParams)) {
                    finish()
                    return
                }
            }

            if (!safeAddView(menuLayout, menuLayoutParams)) {
                finish()
                return
            }
        }

        lifecycleRegistry.currentState = previousState

        if (previousState.isAtLeast(Lifecycle.State.STARTED)) {
            setOverlayViewVisibility(isUserOverlayVisible)
        } else {
            menuLayout.visibility = View.GONE
            menuBackground.visibility = View.GONE
            screenOverlayView?.visibility = View.GONE
        }
    }

    /**
     * Called when an item (other than move/hide) in the menu have been pressed.
     * @param viewId the pressed view identifier.
     */
    protected open fun onMenuItemClicked(@IdRes viewId: Int): Unit = Unit

    /** Immediate, reversible toolbar toggles can opt out of the shared 500 ms action guard. */
    protected open fun shouldDebounceMenuItemClick(@IdRes viewId: Int): Boolean = true

    /**
     * Called when the visibility of the screen overlay have changed.
     * @param isVisible true if it has became visible, false if it became invisible.
     */
    protected open fun onScreenOverlayVisibilityChanged(isVisible: Boolean): Unit = Unit


    /**
     * Change the menu view visibility.
     * @param visibility the new visibility to apply.
     */
    protected fun setMenuVisibility(visibility: Int) {
        menuLayout.visibility = visibility
    }

    /**
     * Set the enabled state of a menu item.
     *
     * @param view the view of the menu item to change the state of.
     * @param enabled true to enable the view, false to disable it.
     * @param clickable true to keep the view clickable, false to ignore all clicks on the view. False by default.
     */
    protected fun setMenuItemViewEnabled(view: View, enabled: Boolean, clickable: Boolean = false) {
        view.apply {
            isEnabled = enabled || clickable
            alpha = if (enabled) 1.0f else disabledItemAlpha
        }
    }

    /**
     * Set the visibility of a menu item.
     *
     * @param view the view of the menu item to change the visibility of.
     * @param visible true for visible, false for gone.
     */
    protected fun setMenuItemVisibility(view: View, visible: Boolean) {
        Log.d(TAG, "setMenuItemVisibility for ${hashCode()}, $view to $visible")

        if (view.isVisible == visible) return
        view.isVisible = visible

        if (canResizeWindow()) forceWindowResize()
    }

    /**
     * Set the visibility of several menu items.
     * When changing multiple items visibility, use this method to recompute the window size only once.
     *
     * @param viewState map of the item views to their new visibility
     */
    protected fun setMenuItemsVisibility(viewState: Map<View, Boolean>) {
        Log.d(TAG, "setMenuItemVisibility for ${hashCode()}, $viewState")

        var haveChanged = false
        viewState.forEach { (view, isVisible) ->
            haveChanged = haveChanged || view.isVisible != isVisible
            view.isVisible = isVisible
        }

        if (!haveChanged) return
        if (canResizeWindow()) forceWindowResize()
    }

    /**
     * Applies a group of state changes. Compose coalesces them and animates the resulting size.
     *
     * @param layoutChanges the changes triggering a resize.
     */
    protected fun animateLayoutChanges(layoutChanges: () -> Unit) {
        layoutChanges()
    }

    private fun canResizeWindow(): Boolean =
        !animations.showAnimationIsRunning
                && !animations.hideAnimationIsRunning && menuBackground.width > 0

    private fun forceWindowResize() {
        Log.d(TAG, "Force window resize")
        menuLayout.requestLayout()
    }


    /**
     * Handle the click on the hide overlay button.
     * Toggle the visible state of the overlay view.
     */
    private fun onToggleOverlayVisibilityClicked() {
        isUserOverlayVisible = !isUserOverlayVisible
        setOverlayViewVisibility(isUserOverlayVisible)
    }

    /**
     * Change the overlay view visibility, allowing the user the click on the Activity bellow the overlays.
     * Updates the hide button state, if any.
     *
     * @param isOverlayVisible the new visibility to apply.
     */
    protected fun setOverlayViewVisibility(isOverlayVisible: Boolean) {
        Log.d(TAG, "setOverlayViewVisibility for ${this@OverlayMenu.hashCode()} with visibility $isOverlayVisible")

        screenOverlayView?.visibility = if (isOverlayVisible) View.VISIBLE else View.GONE
        hideOverlayButton?.setImageResource(
            if (isOverlayVisible) R.drawable.ic_visible_on else R.drawable.ic_visible_off
        )

        onScreenOverlayVisibilityChanged(isOverlayVisible)
    }

    /**
     * Called when the user touch the [R.id.btn_move] menu item.
     * Handle the long press and move on this button in order to drag and drop the overlay menu on the screen.
     *
     * @param event the touch event occurring on the menu item.
     *
     * @return true if the event is handled, false if not.
     */
    private fun onMoveTouched(event: MotionEvent) : Boolean {
        onUserInteraction()
        return moveTouchEventHandler.onTouchEvent(menuLayout, event)
    }

    fun tuckMenu() {
        if (isMenuTucked || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val host = menuLayout as? ComposeOverlayMenuHost ?: return
        val displaySize = displayConfigManager.displayConfig.sizePx
        val isLeft = (menuLayoutParams.x + menuLayout.width / 2) < (displaySize.x / 2)

        // Save current full toolbar position before tucking
        saveMenuPosition(displayConfigManager.displayConfig.orientation)

        wasTuckedBeforeStop = false
        isMenuTucked = true
        host.isDockedOnLeft = isLeft
        host.isTucked = true

        val density = scaledDensity
        val tabWidthPx = (24 * density).roundToInt()
        val tabHeightPx = (56 * density).roundToInt()

        menuLayoutParams.x = if (isLeft) 0 else (displaySize.x - tabWidthPx).coerceAtLeast(0)
        menuLayoutParams.y = menuLayoutParams.y.coerceIn(0, (displaySize.y - tabHeightPx).coerceAtLeast(0))

        menuLayoutParams.enableMoveAnimations()
        windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
        menuLayoutParams.disableMoveAnimations()

        onMenuTuckedChanged(true)
    }

    private fun updateTuckedPositionForNewOrientation(orientation: Int) {
        val host = menuLayout as? ComposeOverlayMenuHost ?: return
        val displaySize = displayConfigManager.displayConfig.sizePx
        val density = scaledDensity

        val visibleButtons = host.buttons.count { it.composeVisibility != View.GONE }.coerceAtLeast(1)
        val fullWidthPx = menuLayout.width.takeIf { it > (56 * density).toInt() } ?: (56 * density).roundToInt()
        val fullHeightPx = ((8 + 48 * visibleButtons) * density).roundToInt()

        val tabWidthPx = (24 * density).roundToInt()
        val tabHeightPx = (56 * density).roundToInt()

        val savedPosition = positionDataSource.loadMenuPosition(orientation)
        val isLeft: Boolean
        val fullY: Int

        if (savedPosition != null) {
            val fullX = savedPosition.x.coerceIn(0, (displaySize.x - fullWidthPx).coerceAtLeast(0))
            fullY = savedPosition.y.coerceIn(0, (displaySize.y - fullHeightPx).coerceAtLeast(0))
            isLeft = (fullX + fullWidthPx / 2) < (displaySize.x / 2)
        } else {
            fullY = ((displaySize.y / 2) - fullHeightPx).coerceIn(0, (displaySize.y - fullHeightPx).coerceAtLeast(0))
            isLeft = host.isDockedOnLeft
        }

        val tuckedX = if (isLeft) 0 else (displaySize.x - tabWidthPx).coerceAtLeast(0)
        val tuckedY = fullY.coerceIn(0, (displaySize.y - tabHeightPx).coerceAtLeast(0))

        host.isDockedOnLeft = isLeft
        host.isTucked = true
        isMenuTucked = true

        menuLayoutParams.x = tuckedX
        menuLayoutParams.y = tuckedY
        menuLayoutParams.disableMoveAnimations()

        onMenuTuckedChanged(true)
    }

    private var tuckedInitialTouchX = 0f
    private var tuckedInitialTouchY = 0f
    private var tuckedInitialWindowY = 0
    private var isTuckedDragging = false
    private var isTuckedHoveringDismiss = false
    private var dismissTargetController: OverlayDismissTargetController? = null

    private fun getOrCreateDismissTargetController(): OverlayDismissTargetController {
        return dismissTargetController ?: OverlayDismissTargetController(
            context = context,
            windowManager = windowManager,
            lifecycleOwner = this,
            savedStateRegistryOwner = this,
            viewModelStoreOwner = this,
        ).also { dismissTargetController = it }
    }

    private fun handleTuckedTouchEvent(event: MotionEvent): Boolean {
        val host = menuLayout as? ComposeOverlayMenuHost ?: return false
        val displaySize = displayConfigManager.displayConfig.sizePx
        val density = scaledDensity
        val tabWidthPx = (24 * density).roundToInt()
        val tabHeightPx = (56 * density).roundToInt()
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tuckedInitialTouchX = event.rawX
                tuckedInitialTouchY = event.rawY
                tuckedInitialWindowY = menuLayoutParams.y
                isTuckedDragging = false
                isTuckedHoveringDismiss = false
                onUserInteraction()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - tuckedInitialTouchX
                val dy = event.rawY - tuckedInitialTouchY
                if (!isTuckedDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                    isTuckedDragging = true
                    menuLayout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (isDragToDismissEnabled) {
                        getOrCreateDismissTargetController().show()
                    }
                }

                if (isTuckedDragging) {
                    val maxTabY = (displaySize.y - tabHeightPx).coerceAtLeast(0)
                    val dockedY = (tuckedInitialWindowY + dy).coerceIn(0f, maxTabY.toFloat())
                    val isLeft = event.rawX < displaySize.x / 2
                    host.isDockedOnLeft = isLeft
                    val edgeX = if (isLeft) 0f else (displaySize.x - tabWidthPx).coerceAtLeast(0).toFloat()

                    if (isDragToDismissEnabled) {
                        val targetCenter = OverlayDismissTargetController.getTargetCenter(displaySize, density)
                        val dist = hypot(event.rawX - targetCenter.x, event.rawY - targetCenter.y)
                        val attractRadiusPx = OverlayDismissTargetController.ATTRACTION_RADIUS_DP * density
                        val dismissRadiusPx = OverlayDismissTargetController.DISMISS_RADIUS_DP * density

                        if (dist < attractRadiusPx) {
                            val pullRatio = ((attractRadiusPx - dist) / (attractRadiusPx - dismissRadiusPx)).coerceIn(0f, 1f)
                            val pull = pullRatio * pullRatio * (3f - 2f * pullRatio)
                            val isHovered = dist <= dismissRadiusPx

                            if (isHovered && !isTuckedHoveringDismiss) {
                                menuLayout.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                            isTuckedHoveringDismiss = isHovered
                            getOrCreateDismissTargetController().updateHoverState(isHovered)

                            val centerTabX = targetCenter.x - (tabWidthPx / 2f)
                            val centerTabY = targetCenter.y - (tabHeightPx / 2f)

                            menuLayoutParams.x = (edgeX + (centerTabX - edgeX) * pull).roundToInt()
                            menuLayoutParams.y = (dockedY + (centerTabY - dockedY) * pull).roundToInt().coerceIn(0, maxTabY)
                        } else {
                            if (isTuckedHoveringDismiss) {
                                isTuckedHoveringDismiss = false
                            }
                            getOrCreateDismissTargetController().updateHoverState(false)
                            menuLayoutParams.x = edgeX.roundToInt()
                            menuLayoutParams.y = dockedY.roundToInt()
                        }
                    } else {
                        menuLayoutParams.x = edgeX.roundToInt()
                        menuLayoutParams.y = dockedY.roundToInt()
                    }

                    windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
                    onUserInteraction()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isTuckedDragging) {
                    host.performClick()
                } else {
                    val wasDismissHovered = isDragToDismissEnabled && isTuckedHoveringDismiss
                    isTuckedDragging = false
                    isTuckedHoveringDismiss = false
                    dismissTargetController?.hide()

                    if (wasDismissHovered) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            menuLayout.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            menuLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        onTuckedDismiss()
                    } else {
                        val isLeft = host.isDockedOnLeft
                        menuLayoutParams.x = if (isLeft) 0 else (displaySize.x - tabWidthPx).coerceAtLeast(0)
                        windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
                        onUserInteraction()
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isTuckedDragging = false
                isTuckedHoveringDismiss = false
                dismissTargetController?.hide()
                return true
            }
        }
        return false
    }

    fun untuckMenu() {
        if (!isMenuTucked || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val host = menuLayout as? ComposeOverlayMenuHost ?: return
        val displaySize = displayConfigManager.displayConfig.sizePx
        val currentOrientation = displayConfigManager.displayConfig.orientation

        isMenuTucked = false
        wasTuckedBeforeStop = false
        host.isTucked = false

        val density = scaledDensity
        val visibleButtons = host.buttons.count { it.composeVisibility != View.GONE }.coerceAtLeast(1)
        val fullWidthPx = menuLayout.width.takeIf { it > (56 * density).toInt() } ?: (56 * density).roundToInt()
        val fullHeightPx = ((8 + 48 * visibleButtons) * density).roundToInt()

        val savedPosition = positionDataSource.loadMenuPosition(currentOrientation)
        menuLayoutParams.x = (savedPosition?.x ?: ((displaySize.x - fullWidthPx) / 2))
            .coerceIn(0, (displaySize.x - fullWidthPx).coerceAtLeast(0))
        menuLayoutParams.y = (savedPosition?.y ?: ((displaySize.y / 2) - fullHeightPx))
            .coerceIn(0, (displaySize.y - fullHeightPx).coerceAtLeast(0))

        menuLayoutParams.enableMoveAnimations()
        windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
        menuLayoutParams.disableMoveAnimations()

        onMenuTuckedChanged(false)
    }

    /** Safe setter for the position of the overlay menu ensuring it will not be displayed outside the screen. */
    private fun updateMenuPosition(position: Point) {
        val displaySize = displayConfigManager.displayConfig.sizePx
        if (displaySize.x < menuLayout.width || displaySize.y < menuLayout.height) return

        val newX = position.x.coerceIn(0, displaySize.x - menuLayout.width)
        val newY = position.y.coerceIn(0, displaySize.y - menuLayout.height)

        if (newX != menuLayoutParams.x || newY != menuLayoutParams.y) {
            menuLayoutParams.x = newX
            menuLayoutParams.y = newY

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                Log.d(TAG, "Updating menu window position: ${menuLayoutParams.x}/${menuLayoutParams.y}")
                windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
            }
        }
    }

    private fun loadMenuPosition(orientation: Int) {
        val savedPosition = positionDataSource.loadMenuPosition(orientation)
        if (savedPosition != null) {
            updateMenuPosition(savedPosition)
        } else {
            menuLayout.doWhenMeasured {
                updateMenuPosition(
                    Point(
                        (displayConfigManager.displayConfig.sizePx.x - menuLayout.width) / 2,
                        (displayConfigManager.displayConfig.sizePx.y / 2) - menuLayout.height,
                    )
                )
            }
        }
    }

    private fun saveMenuPosition(orientation: Int) {
        positionDataSource.saveMenuPosition(
            position = Point(menuLayoutParams.x, menuLayoutParams.y),
            orientation = orientation,
        )
    }

    private fun onLockedPositionChanged(lockedPosition: Point?) {
        if (lockedPosition != null) {
            Log.d(TAG, "Locking menu position of overlay ${hashCode()}")
            moveButton?.let { setMenuItemVisibility(it, false) }
            saveMenuPosition(displayConfigManager.displayConfig.orientation)
            updateMenuPosition(lockedPosition)
        } else {
            Log.d(TAG, "Unlocking menu position of overlay ${hashCode()}")
            moveButton?.let { setMenuItemVisibility(it, true) }
            loadMenuPosition(displayConfigManager.displayConfig.orientation)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(contentPrefix)
                .append("resumeOnceShown=$resumeOnceShown; ")
                .append("destroyOnceHidden=$destroyOnceHidden; ")
                .println()

            animations.dump(writer, contentPrefix)
            positionDataSource.dump(writer, contentPrefix)
        }
    }
}

/** Tag for logs */
private const val TAG = "OverlayMenu"
