/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.scenarios.list

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton

internal class ScenarioListViews(context: Context, onCreateClicked: () -> Unit) {
    val appBarLayout = AppBarLayout(context)
    val topAppBar = MaterialToolbar(context)
    val list = RecyclerView(context)
    val add = FloatingActionButton(context)
    val emptyVisible: MutableState<Boolean> = mutableStateOf(false)
    val loadingVisible: MutableState<Boolean> = mutableStateOf(true)

    val root = CoordinatorLayout(context).apply {
        list.id = View.generateViewId()
        list.isVerticalScrollBarEnabled = true
        list.layoutManager = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(context)
        }

        topAppBar.apply {
            title = context.getString(R.string.activity_scenario_title)
            inflateMenu(R.menu.menu_scenario_fragment)
            layoutParams = AppBarLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
        }
        appBarLayout.apply {
            setLiftOnScroll(true)
            liftOnScrollTargetViewId = list.id
            addView(topAppBar)
        }
        addView(
            appBarLayout,
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        addView(
            list,
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                behavior = AppBarLayout.ScrollingViewBehavior()
                bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_vertical_large)
            },
        )

        addView(
            ComposeView(context).apply {
                setContent {
                    MacrionTheme {
                        ScenarioListOverlay(
                            landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
                            emptyVisible = emptyVisible.value,
                            loadingVisible = loadingVisible.value,
                            onCreateClicked = onCreateClicked,
                        )
                    }
                }
            },
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        add.apply {
            setImageResource(R.drawable.ic_add)
            contentDescription = context.getString(R.string.content_desc_add_scenario)
            visibility = View.GONE
        }
        addView(
            add,
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                anchorId = list.id
                anchorGravity = Gravity.BOTTOM or Gravity.END
                behavior = HideBottomViewOnScrollBehavior<FloatingActionButton>()
            },
        )

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val topInset = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
            ).top
            appBarLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset
            }
            windowInsets
        }
    }
}

@Composable
private fun ScenarioListOverlay(
    landscape: Boolean,
    emptyVisible: Boolean,
    loadingVisible: Boolean,
    onCreateClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (emptyVisible) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (landscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EmptyAnimation(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.message_empty_scenario_list),
                            modifier = Modifier.weight(1f).padding(end = 42.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        EmptyAnimation(Modifier)
                        Text(
                            text = stringResource(R.string.message_empty_scenario_list),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.margin_horizontal_default)),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Button(
                    onClick = onCreateClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dimensionResource(R.dimen.margin_horizontal_large),
                            end = dimensionResource(R.dimen.margin_horizontal_large),
                            bottom = dimensionResource(R.dimen.margin_vertical_large),
                        ),
                ) {
                    Text(stringResource(R.string.button_text_create_scenario))
                }
            }
        }

        if (loadingVisible) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun EmptyAnimation(modifier: Modifier) {
    AndroidView(
        factory = { context ->
            LottieAnimationView(context).apply {
                setAnimation(R.raw.lottie_empty)
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        },
        modifier = modifier,
        onRelease = { it.cancelAnimation() },
    )
}
