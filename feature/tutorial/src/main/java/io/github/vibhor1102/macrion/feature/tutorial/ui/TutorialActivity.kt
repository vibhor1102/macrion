/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.tutorial.ui

import android.graphics.Point
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import io.github.vibhor1102.macrion.core.common.permissions.ui.PermissionsHost
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.display.recorder.MediaProjectionRequest
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.errors.createNoMediaProjectionDialog
import io.github.vibhor1102.macrion.feature.tutorial.R
import io.github.vibhor1102.macrion.feature.tutorial.data.mapping.toTutorialSlideshow
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategory
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategoryUiItems
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialItem
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialSlideshow
import io.github.vibhor1102.macrion.feature.tutorial.ui.dialogs.createTutorialSuccessDialog
import io.github.vibhor1102.macrion.feature.tutorial.ui.game.clickcount.ClickCountGameScreen
import io.github.vibhor1102.macrion.feature.tutorial.ui.game.clickcount.ClickCountGameViewModel
import io.github.vibhor1102.macrion.feature.tutorial.ui.game.timing.TimingGameScreen
import io.github.vibhor1102.macrion.feature.tutorial.ui.game.timing.TimingGameViewModel
import io.github.vibhor1102.macrion.feature.tutorial.ui.list.TutorialListScreen
import io.github.vibhor1102.macrion.feature.tutorial.ui.list.TutorialListViewModel
import io.github.vibhor1102.macrion.feature.tutorial.ui.overlay.TutorialFullscreenOverlay
import io.github.vibhor1102.macrion.feature.tutorial.ui.slideshow.TutorialSlideshowContent
import io.github.vibhor1102.macrion.core.ui.R as CoreUiR

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TutorialActivity : ComponentActivity() {

    @Inject lateinit var overlayManager: OverlayManager

    private val tutorialListViewModel: TutorialListViewModel by viewModels()
    private val clickCountGameViewModel: ClickCountGameViewModel by viewModels()
    private val timingGameViewModel: TimingGameViewModel by viewModels()

    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaProjectionRequest.registerForActivityResult(this)

        setContent {
            MacrionTheme {
                TutorialActivityContent()
                PermissionsHost(tutorialListViewModel.permissionsController)
            }
        }
    }

    @Composable
    private fun TutorialActivityContent() {
        var nextId by remember { mutableIntStateOf(1) }
        val navigationStack = remember {
            mutableStateListOf<TutorialDestination>(
                TutorialDestination.Category(id = 0, categoryType = TutorialCategory.Type.ROOT),
            )
        }

        fun popBackStack(): Boolean {
            return if (navigationStack.size > 1) {
                navigationStack.removeAt(navigationStack.lastIndex)
                true
            } else {
                false
            }
        }

        BackHandler(enabled = navigationStack.size > 1) {
            popBackStack()
        }

        val currentDestination = navigationStack.last()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                TutorialToolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onBack = {
                        if (!popBackStack()) {
                            finish()
                        }
                    },
                )
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        if (targetState.id > initialState.id) {
                            (slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300)))
                        } else {
                            (slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300)))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "TutorialNavigationTransition",
                ) { destination ->
                    when (destination) {
                        is TutorialDestination.Category -> {
                            DisposableEffect(destination.categoryType) {
                                tutorialListViewModel.stopTutorial()
                                onDispose { }
                            }
                            TutorialListScreen(
                                uiStateFlow = tutorialListViewModel.uiState(destination.categoryType),
                                onItemClicked = { item ->
                                    when (item) {
                                        is TutorialCategoryUiItems.Item.Category -> {
                                            navigationStack.add(
                                                TutorialDestination.Category(nextId++, item.type),
                                            )
                                        }

                                        is TutorialCategoryUiItems.Item.Slideshow -> {
                                            navigationStack.add(
                                                TutorialDestination.Slideshow(nextId++, item.type),
                                            )
                                        }

                                        is TutorialCategoryUiItems.Item.Tutorial -> {
                                            startTutorialItem(item) { isTiming ->
                                                val nextDestination = if (isTiming) {
                                                    TutorialDestination.TimingGame(nextId++)
                                                } else {
                                                    TutorialDestination.ClickCountGame(nextId++)
                                                }
                                                navigationStack.add(nextDestination)
                                            }
                                        }
                                    }
                                },
                            )
                        }

                        is TutorialDestination.Slideshow -> {
                            TutorialSlideshowHost(
                                slideshowType = destination.slideshowType,
                                onClose = { popBackStack() },
                            )
                        }

                        is TutorialDestination.ClickCountGame -> {
                            ClickCountGameHost(
                                viewModel = clickCountGameViewModel,
                                overlayManager = overlayManager,
                                onNavigateBack = { popBackStack() },
                            )
                        }

                        is TutorialDestination.TimingGame -> {
                            TimingGameHost(
                                viewModel = timingGameViewModel,
                                overlayManager = overlayManager,
                                onNavigateBack = { popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startTutorialItem(
        item: TutorialCategoryUiItems.Item.Tutorial,
        onNavigateToGame: (isTiming: Boolean) -> Unit,
    ) {
        tutorialListViewModel.startPermissionFlowIfNeeded(
            activity = this,
            onAllGranted = {
                mediaProjectionRequest.showMediaProjectionWarning(
                    context = this,
                    forceEntireScreen = true,
                    onSuccess = { resultCode, data ->
                        tutorialListViewModel.startTutorial(item, resultCode, data)
                        val isTiming = item.type == TutorialItem.Type.TIMER_REACHED_CONDITION
                        onNavigateToGame(isTiming)
                    },
                    onFailure = {
                        Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        createNoMediaProjectionDialog { finish() }.show()
                    },
                )
            },
        )
    }

    @Composable
    private fun TutorialToolbar(
        modifier: Modifier = Modifier,
        onBack: () -> Unit,
    ) {
        Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_back),
                        contentDescription = null,
                    )
                }
                Text(
                    text = stringResource(R.string.activity_tutorial_name),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private sealed interface TutorialDestination {
    val id: Int

    data class Category(
        override val id: Int,
        val categoryType: TutorialCategory.Type,
    ) : TutorialDestination

    data class Slideshow(
        override val id: Int,
        val slideshowType: TutorialSlideshow.Type,
    ) : TutorialDestination

    data class ClickCountGame(
        override val id: Int,
    ) : TutorialDestination

    data class TimingGame(
        override val id: Int,
    ) : TutorialDestination
}

@Composable
private fun TutorialSlideshowHost(
    slideshowType: TutorialSlideshow.Type,
    onClose: () -> Unit,
) {
    val slideshow = slideshowType.toTutorialSlideshow()

    Surface(Modifier.fillMaxSize()) {
        TutorialSlideshowContent(
            pages = slideshow.slideshowItems,
            modifier = Modifier.fillMaxSize(),
            header = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .border(
                            width = 2.dp,
                            color = colorResource(R.color.tutorial_header_card_border),
                            shape = MaterialTheme.shapes.medium,
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_tutorial_slideshow),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(64.dp),
                        )
                        Text(
                            text = stringResource(slideshow.nameRes),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                        )
                    }
                }
            },
            onClose = onClose,
        )
    }
}

@Composable
private fun ClickCountGameHost(
    viewModel: ClickCountGameViewModel,
    overlayManager: OverlayManager,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showOverlayMenuPlaceholder by remember { mutableStateOf(true) }
    var overlayMenuPosition by remember { mutableStateOf<IntOffset?>(null) }
    var completionDialog by remember { mutableStateOf<AlertDialog?>(null) }

    fun lockMenuPosition(position: IntOffset) {
        overlayManager.lockMenuPosition(Point(position.x, position.y))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    overlayMenuPosition?.let(::lockMenuPosition)
                    if (viewModel.shouldDisplayFloatingUi.value && overlayManager.isOverlayStackHidden()) {
                        overlayManager.restoreVisibility()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    viewModel.stopDetection()
                    if (viewModel.shouldDisplayFloatingUi.value) {
                        overlayManager.hideAll()
                    }
                    overlayManager.removeTopOverlay()
                    overlayManager.unlockMenuPosition()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            completionDialog?.dismiss()
            completionDialog = null
            viewModel.stopDetection()
            if (viewModel.shouldDisplayFloatingUi.value) {
                overlayManager.hideAll()
            }
            overlayManager.removeTopOverlay()
            overlayManager.unlockMenuPosition()
            viewModel.stopTutorial()
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.shouldDisplayStepOverlay.collect { show ->
                    showOverlayMenuPlaceholder = !overlayManager.isOverlayStackVisible()
                    if (show) overlayManager.setTopOverlay(TutorialFullscreenOverlay())
                    else overlayManager.removeTopOverlay()
                }
            }
            launch {
                viewModel.shouldDisplayFloatingUi.collect { show ->
                    if (show) overlayManager.restoreVisibility()
                    else overlayManager.hideAll()
                }
            }
            launch {
                viewModel.shouldDisplayCompletionDialog.collect { show ->
                    if (!show || completionDialog?.isShowing == true) return@collect
                    completionDialog = context.createTutorialSuccessDialog {
                        onNavigateBack()
                    }.also { dialog ->
                        dialog.setOnDismissListener {
                            if (completionDialog === dialog) completionDialog = null
                        }
                        dialog.show()
                    }
                }
            }
            launch {
                viewModel.shouldStopGame.collect { shouldStop ->
                    if (shouldStop) onNavigateBack()
                }
            }
        }
    }

    ClickCountGameScreen(
        uiStateFlow = viewModel.uiState,
        showOverlayMenuPlaceholder = showOverlayMenuPlaceholder,
        onOverlayMenuPositioned = { position ->
            overlayMenuPosition = position
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lockMenuPosition(position)
            }
        },
        onTargetHit = viewModel::onTargetHit,
        onStartGame = viewModel::startGame,
    )
}

@Composable
private fun TimingGameHost(
    viewModel: TimingGameViewModel,
    overlayManager: OverlayManager,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showOverlayMenuPlaceholder by remember { mutableStateOf(true) }
    var overlayMenuPosition by remember { mutableStateOf<IntOffset?>(null) }
    var completionDialog by remember { mutableStateOf<AlertDialog?>(null) }

    fun lockMenuPosition(position: IntOffset) {
        overlayManager.lockMenuPosition(Point(position.x, position.y))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    overlayMenuPosition?.let(::lockMenuPosition)
                    if (viewModel.shouldDisplayFloatingUi.value && overlayManager.isOverlayStackHidden()) {
                        overlayManager.restoreVisibility()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    viewModel.stopDetection()
                    if (viewModel.shouldDisplayFloatingUi.value) {
                        overlayManager.hideAll()
                    }
                    overlayManager.removeTopOverlay()
                    overlayManager.unlockMenuPosition()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            completionDialog?.dismiss()
            completionDialog = null
            viewModel.stopDetection()
            if (viewModel.shouldDisplayFloatingUi.value) {
                overlayManager.hideAll()
            }
            overlayManager.removeTopOverlay()
            overlayManager.unlockMenuPosition()
            viewModel.stopTutorial()
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.shouldDisplayStepOverlay.collect { show ->
                    showOverlayMenuPlaceholder = !overlayManager.isOverlayStackVisible()
                    if (show) overlayManager.setTopOverlay(TutorialFullscreenOverlay())
                    else overlayManager.removeTopOverlay()
                }
            }
            launch {
                viewModel.shouldDisplayFloatingUi.collect { show ->
                    if (show) overlayManager.restoreVisibility()
                    else overlayManager.hideAll()
                }
            }
            launch {
                viewModel.shouldDisplayCompletionDialog.collect { show ->
                    if (!show || completionDialog?.isShowing == true) return@collect
                    completionDialog = context.createTutorialSuccessDialog {
                        onNavigateBack()
                    }.also { dialog ->
                        dialog.setOnDismissListener {
                            if (completionDialog === dialog) completionDialog = null
                        }
                        dialog.show()
                    }
                }
            }
            launch {
                viewModel.shouldStopGame.collect { shouldStop ->
                    if (shouldStop) onNavigateBack()
                }
            }
        }
    }

    TimingGameScreen(
        uiStateFlow = viewModel.uiState,
        showOverlayMenuPlaceholder = showOverlayMenuPlaceholder,
        onOverlayMenuPositioned = { position ->
            overlayMenuPosition = position
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lockMenuPosition(position)
            }
        },
        onTimingClick = viewModel::onTimingButtonHit,
        onRetryClick = viewModel::resetGame,
    )
}
