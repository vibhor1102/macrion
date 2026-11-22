/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.feature.externallaunch.qstile.ui

import android.content.Intent
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QSTileLauncherViewModelTest {
    @Test
    fun captureGrantRemainsAvailableWhenTheFirstActivityStopsDuringLookup() = runTest {
        val scenarios = mockk<IRepository>()
        val launches = mockk<ExternalLaunchRepository>(relaxed = true)
        val viewModel = QSTileLauncherViewModel(
            ioDispatcher = StandardTestDispatcher(testScheduler),
            qsTileRepository = launches,
            permissionController = mockk(),
            smartRepository = scenarios,
            dumbRepository = mockk(),
            settingsRepository = mockk(),
            appComponentsProvider = mockk(),
        )
        val data = mockk<Intent>()
        val scenario = mockk<Scenario>()
        val lookupStarted = CompletableDeferred<Unit>()
        val releaseLookup = CompletableDeferred<Unit>()
        coEvery { scenarios.getScenario(42L) } coAnswers {
            lookupStarted.complete(Unit)
            releaseLookup.await()
            scenario
        }

        viewModel.storeProjectionGrant(-1, data, 42L)
        val grant = requireNotNull(viewModel.projectionGrant.value)
        val firstActivity = launch { viewModel.launchGrantedScenario(grant) }
        lookupStarted.await()
        firstActivity.cancelAndJoin()

        assertSame(grant, viewModel.projectionGrant.value)
        verify(exactly = 0) { launches.launchSmartScenario(any(), any(), any()) }

        releaseLookup.complete(Unit)
        assertTrue(viewModel.launchGrantedScenario(grant))
        assertNull(viewModel.projectionGrant.value)
        assertFalse(viewModel.launchGrantedScenario(grant))
        verify(exactly = 1) { launches.launchSmartScenario(-1, data, scenario) }
    }
}
