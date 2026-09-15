/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.overlays

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vibhor1102.macrion.core.common.overlays.scale.OverlayScaleProvider
import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOverlayScaleProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tutorialRepository: TutorialRepository,
) : OverlayScaleProvider {

    override val scaleFlow: Flow<Float> = combine(
        settingsRepository.toolbarScalePercentFlow,
        tutorialRepository.tutorialState,
    ) { percent, _ ->
        if (tutorialRepository.isTutorialStarted()) {
            1.0f
        } else {
            percent / 100f
        }
    }

    override fun getScale(): Float =
        if (tutorialRepository.isTutorialStarted()) {
            1.0f
        } else {
            settingsRepository.getToolbarScalePercent() / 100f
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OverlayScaleModule {
    @Binds
    abstract fun bindOverlayScaleProvider(impl: AppOverlayScaleProvider): OverlayScaleProvider
}
