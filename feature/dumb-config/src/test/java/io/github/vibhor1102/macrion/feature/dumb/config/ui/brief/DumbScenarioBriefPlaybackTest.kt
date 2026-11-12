package io.github.vibhor1102.macrion.feature.dumb.config.ui.brief

import android.content.Context
import androidx.lifecycle.ViewModelStore
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.dumb.engine.DumbEngine
import io.github.vibhor1102.macrion.feature.dumb.config.domain.DumbEditionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DumbScenarioBriefPlaybackTest {
    private val action = DumbAction.DumbPause(Identifier(databaseId = 2), Identifier(databaseId = 1), "Pause", pauseDurationMs = 100)
    private lateinit var engine: DumbEngine
    private lateinit var viewModel: DumbScenarioBriefViewModel
    private lateinit var store: ViewModelStore

    @Before fun setup() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        engine = mock { on { isRunning } doReturn MutableStateFlow(false) }
        val scenario = DumbScenario(Identifier(databaseId = 1), "Test", listOf(action), 1, false, 1, true, false)
        val edition = mock<DumbEditionRepository> {
            on { editedDumbScenario } doReturn MutableStateFlow(scenario)
        }
        viewModel = DumbScenarioBriefViewModel(mock<Context>(), dispatcher, edition, engine)
        store = ViewModelStore().apply { put("test", viewModel) }
    }

    @After fun teardown() { store.clear(); Dispatchers.resetMain() }

    @Test fun volumeStopDuringPreparationCancelsPendingTryAndResetsUi() = runTest {
        var completed = 0
        viewModel.playAction(0) { completed++ }
        advanceTimeBy(200)
        assertTrue(viewModel.stopAction())
        advanceTimeBy(1_000)
        runCurrent()
        verify(engine, never()).tryDumbAction(any(), any())
        assertEquals(1, completed)
        assertFalse(viewModel.stopAction())
    }

    @Test fun destroyingMenuCancelsDelayedTry() = runTest {
        viewModel.playAction(0) {}
        advanceTimeBy(200)
        store.clear()
        advanceTimeBy(1_000)
        runCurrent()
        verify(engine, never()).tryDumbAction(any(), any())
    }

    @Test fun staleCompletionCannotFinishNewTry() = runTest {
        whenever(engine.tryDumbAction(any(), any())).thenReturn(true)
        var completed = 0
        viewModel.playAction(0) { completed++ }
        advanceTimeBy(501)
        runCurrent()
        val callback = argumentCaptor<() -> Unit>()
        verify(engine).tryDumbAction(eq(action), callback.capture())
        val oldCallback = callback.firstValue
        viewModel.stopAction()
        viewModel.playAction(0) { completed++ }
        oldCallback()
        runCurrent()
        assertEquals(1, completed)
        assertTrue(viewModel.stopAction())
        assertEquals(2, completed)
    }
}
