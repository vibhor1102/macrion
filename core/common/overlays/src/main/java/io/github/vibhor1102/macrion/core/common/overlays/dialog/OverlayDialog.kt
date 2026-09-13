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
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import android.app.Dialog
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import io.github.vibhor1102.macrion.core.base.addDumpTabulationLvl
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager

import java.io.PrintWriter

/**
 * Controller for a dialog opened from a service as an overlay.
 *
 * This class ensure that all dialogs opened from a service will have the same behaviour. It provides basic lifecycle
 * alike methods to ease the view initialization/cleaning.
 */
abstract class OverlayDialog(@StyleRes theme: Int? = null) : BaseOverlay(theme, recreateOnRotation = true) {

    /** The Android InputMethodManger, for ensuring the keyboard dismiss on dialog dismiss. */
    private lateinit var inputMethodManager: InputMethodManager
    /** Touch listener hiding the software keyboard and propagating the touch event normally. */
    protected val hideSoftInputTouchListener = View.OnTouchListener { view, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            view.findFocus()?.clearFocus()
            hideSoftInput()
        }
        false
    }

    /** Tells if the dialog is visible. */
    private var isShown = false

    /**
     * The dialog currently displayed by this controller.
     * Null until [onDialogCreated] is called, or if it has been dismissed.
     */
    protected var dialog: Dialog? = null
        private set

    /**
     * Creates the dialog shown by this controller.
     * Note that the cancelable value and the dismiss listener will be overridden with internal values once, so any
     * values for them defined here will not be kept.
     *
     * @return the builder for the dialog to be created.
     */
    protected abstract fun onCreateView(): ViewGroup

    /**
     * Setup the dialog view.
     * Called once the dialog is created and first show, it allows the implementation to initialize the content views.
     *
     * @param dialog the newly created dialog.
     */
    open fun onDialogCreated(dialog: Dialog) = Unit

    final override fun onCreate() {
        inputMethodManager = context.getSystemService(InputMethodManager::class.java)

        val dialogTheme = theme ?: io.github.vibhor1102.macrion.core.ui.R.style.AppTheme
        val view = onCreateView()

        // WindowManager overlay roots don't inherit Activity view-tree owners. Install this
        // overlay's owners before attaching the view so Compose can create its recomposer safely.
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeViewModelStoreOwner(this)

        dialog = Dialog(context, dialogTheme).apply {
            setContentView(view)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@OverlayDialog.back()
                    true
                } else {
                    false
                }
            }
            create()

            window?.apply {
                decorView.setViewTreeLifecycleOwner(this@OverlayDialog)
                decorView.setViewTreeSavedStateRegistryOwner(this@OverlayDialog)
                decorView.setViewTreeViewModelStoreOwner(this@OverlayDialog)

                setType(OverlayManager.OVERLAY_WINDOW_TYPE)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM)
                setBackgroundDrawableResource(android.R.color.transparent)
                setDimAmount(0.6f)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN,
                )
                decorView.setOnTouchListener(hideSoftInputTouchListener)
            }
        }

        onDialogCreated(dialog!!)
    }

    @CallSuper
    override fun onStart() {
        if (isShown) return

        isShown = true
        dialog?.show()
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        hideSoftInput()
    }

    @CallSuper
    override fun onStop() {
        if (!isShown) return

        hideSoftInput()
        dialog?.hide()
        isShown = false
    }

    @CallSuper
    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }

    /** Hide automatically the software keyboard when the provided view lose the focus. */
    fun hideSoftInputOnFocusLoss(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (view.id == v.id && !hasFocus) {
                hideSoftInput()
            }
        }
    }

    /** Hide the software keyboard. */
    private fun hideSoftInput() {
        dialog?.let {
            inputMethodManager.hideSoftInputFromWindow(it.window!!.decorView.windowToken, 0)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.append(contentPrefix)
            .append("isDialogShown=$isShown; ")
            .println()
    }
}
