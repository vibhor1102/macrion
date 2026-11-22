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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.viewModels

import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.display.recorder.MediaProjectionRequest
import io.github.vibhor1102.macrion.core.ui.errors.createNoMediaProjectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.R

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RestartMediaProjectionActivity : ComponentActivity() {

    companion object {

        private const val EXTRA_REQUEST_ID = "projectionRequestId"

        internal fun getStartIntent(context: Context, requestId: String): Intent =
            Intent(context, RestartMediaProjectionActivity::class.java)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    private val viewModel: RestartMediaProjectionViewModel by viewModels()

    @Inject lateinit var overlayManager: OverlayManager

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()
    private var requestStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ownsRequest()) {
            finish()
            return
        }

        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) })

        mediaProjectionRequest.registerForActivityResult(this)
        onBackPressedDispatcher.addCallback(this) { finishActivity() }
        requestStarted = savedInstanceState?.getBoolean("projectionRequestStarted") == true
        val shouldLaunch = !requestStarted
        requestStarted = true
        mediaProjectionRequest.showMediaProjectionWarning(
            context = this,
            forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
            onSuccess = { resultCode, data -> restartScreenRecord(resultCode, data) },
            onFailure = { showScreenSharingNotStartedToast() },
            onError = { requestStarted = false; showUnsupportedDeviceDialog() },
            launchRequest = shouldLaunch,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("projectionRequestStarted", requestStarted)
        super.onSaveInstanceState(outState)
    }

    private fun ownsRequest(): Boolean =
        !isFinishing && overlayManager.ownsStarterRequest(intent.getStringExtra(EXTRA_REQUEST_ID))

    private fun finishActivity() {
        if (isFinishing) return
        if (ownsRequest()) overlayManager.navigateUp(this)
        finish()
    }

    private fun restartScreenRecord(resultCode: Int, data: Intent) {
        if (!ownsRequest()) {
            finish()
            return
        }

        Log.i(TAG, "Media projection permission granted, restart recording")
        viewModel.restartScreenRecord(resultCode, data)
        finishActivity()
    }

    private fun showScreenSharingNotStartedToast() {
        Log.i(TAG, "Media projection permission not granted")
        if (ownsRequest()) {
            Toast.makeText(this, R.string.toast_screen_sharing_not_started, Toast.LENGTH_SHORT).show()
        }
        finishActivity()
    }

    private fun showUnsupportedDeviceDialog() {
        if (ownsRequest()) createNoMediaProjectionDialog { finishActivity() }.show() else finish()
    }
}

private const val TAG = "RequestMediaProjectionActivity"
