/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import io.github.vibhor1102.macrion.core.common.overlays.di.OverlaysEntryPoint
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet.required.RequiredAlphabetViewModel
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet.selection.AlphabetSelectionViewModel

/** Foreground Activity required by Play Asset Delivery, with an Activity-owned Compose sheet. */
@AndroidEntryPoint
class AlphabetActivity : AppCompatActivity() {

    companion object {
        const val MODE_SELECTION = 1
        const val MODE_REQUIRED = 2
        private const val EXTRA_MODE = "io.github.vibhor1102.macrion.feature.smart.config.ui.EXTRA_OCR_MODE"

        fun getStartIntent(context: Context, mode: Int): Intent =
            Intent(context, AlphabetActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_MODE, mode)
    }

    private val selectionViewModel: AlphabetSelectionViewModel by viewModels()
    private val requiredViewModel: RequiredAlphabetViewModel by viewModels()
    private val overlayManager: OverlayManager by lazy {
        EntryPoints.get(applicationContext, OverlaysEntryPoint::class.java).overlayManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayManager.hideAll()
        val mode = intent?.getIntExtra(EXTRA_MODE, 0) ?: 0
        if (mode != MODE_SELECTION && mode != MODE_REQUIRED) {
            Log.e(TAG, "Invalid alphabet mode $mode")
            finish()
            return
        }
        setContentView(ComposeView(this).apply {
            setContent {
                MacrionTheme {
                    Surface(Modifier.fillMaxSize(), color = Color.Transparent) { AlphabetSheet(mode) }
                }
            }
        })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AlphabetSheet(mode: Int) {
        ModalBottomSheet(
            onDismissRequest = ::finish,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            sheetGesturesEnabled = false,
            dragHandle = null,
        ) {
            if (mode == MODE_SELECTION) SelectionContent() else RequiredContent()
        }
    }

    @Composable
    private fun SelectionContent() {
        val items = selectionViewModel.items.collectAsStateWithLifecycle(initialValue = emptyList()).value
        val isEditing = selectionViewModel.isEditingCondition.collectAsStateWithLifecycle(initialValue = true).value
        LaunchedEffect(isEditing) {
            if (!isEditing) {
                Log.e(TAG, "Closing alphabet selection because there is no condition edited")
                finish()
            }
        }
        AlphabetModelSheet(items, false, true, ::finish) { item ->
            if (item !is AlphabetSelectionItem.Alphabet) return@AlphabetModelSheet
            when (item.downloadState) {
                AlphabetDownloadUiState.Downloaded -> selectionViewModel.selectModel(item.alphabet)
                AlphabetDownloadUiState.NotDownloaded -> selectionViewModel.downloadModel(item.alphabet)
                is AlphabetDownloadUiState.Downloading, AlphabetDownloadUiState.Error -> Unit
            }
        }
    }

    @Composable
    private fun RequiredContent() {
        val items = requiredViewModel.items.collectAsStateWithLifecycle(initialValue = emptyList()).value
        val canContinue = requiredViewModel.canContinue.collectAsStateWithLifecycle(initialValue = false).value
        AlphabetModelSheet(items, true, canContinue, ::finish) { item ->
            if (item is AlphabetSelectionItem.Alphabet && item.downloadState == AlphabetDownloadUiState.NotDownloaded) {
                requiredViewModel.downloadModel(item.alphabet)
            }
        }
    }

    override fun onDestroy() {
        overlayManager.restoreVisibility()
        super.onDestroy()
    }
}

private const val TAG = "AlphabetActivity"
