package io.github.vibhor1102.macrion.core.dumb.engine

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DumbEngineStartTest {
    private val action = DumbAction.DumbPause(Identifier(databaseId = 2), Identifier(databaseId = 1), "Pause", pauseDurationMs = 100)
    private val scenario = DumbScenario(Identifier(databaseId = 1), "Test", listOf(action), 1, false, 1, true, false)

    @Test fun stopWhileLoadingPreventsLaterExecution() = runTest {
        val repository = mock<IDumbRepository>()
        val executor = mock<DumbActionExecutor>()
        val loaded = CompletableDeferred<DumbScenario?>()
        whenever(repository.getDumbScenario(1)).doSuspendableAnswer { loaded.await() }
        val engine = DumbEngine(repository, executor, mock<SettingsRepository>(), StandardTestDispatcher(testScheduler))
        engine.init(scenario)
        engine.startDumbScenario()
        assertTrue(engine.isRunning.value)
        runCurrent()
        engine.stopDumbScenario()
        assertFalse(engine.isRunning.value)
        loaded.complete(scenario)
        runCurrent()
        verify(executor, never()).executeDumbAction(any(), any())
        engine.release()
    }

    @Test fun repeatedPlayWhileLoadingPerformsOnlyOneLookup() = runTest {
        val repository = mock<IDumbRepository>()
        val loaded = CompletableDeferred<DumbScenario?>()
        whenever(repository.getDumbScenario(1)).doSuspendableAnswer { loaded.await() }
        val engine = DumbEngine(repository, mock<DumbActionExecutor>(), mock<SettingsRepository>(), StandardTestDispatcher(testScheduler))
        engine.init(scenario)
        engine.startDumbScenario()
        engine.startDumbScenario()
        runCurrent()
        verify(repository, times(1)).getDumbScenario(1)
        engine.release()
        loaded.complete(scenario)
        runCurrent()
        assertFalse(engine.isRunning.value)
    }

    @Test fun freshPlayWorksAfterCancellingPendingStart() = runTest {
        val repository = mock<IDumbRepository>()
        val loaded = CompletableDeferred<DumbScenario?>()
        var reads = 0
        whenever(repository.getDumbScenario(1)).doSuspendableAnswer {
            if (++reads == 1) loaded.await() else scenario
        }
        val executor = mock<DumbActionExecutor>()
        val engine = DumbEngine(repository, executor, mock<SettingsRepository>(), StandardTestDispatcher(testScheduler))
        engine.init(scenario)
        engine.startDumbScenario()
        runCurrent()
        engine.stopDumbScenario()
        engine.startDumbScenario()
        runCurrent()
        loaded.complete(scenario)
        runCurrent()
        verify(executor, times(1)).executeDumbAction(action, false)
        assertFalse(engine.isRunning.value)
        engine.release()
    }

    @Test fun missingScenarioReleasesStartingState() = runTest {
        val repository = mock<IDumbRepository>()
        val engine = DumbEngine(repository, mock<DumbActionExecutor>(), mock<SettingsRepository>(), StandardTestDispatcher(testScheduler))
        engine.init(scenario)
        engine.startDumbScenario()
        runCurrent()
        assertFalse(engine.isRunning.value)
        engine.release()
    }
}
