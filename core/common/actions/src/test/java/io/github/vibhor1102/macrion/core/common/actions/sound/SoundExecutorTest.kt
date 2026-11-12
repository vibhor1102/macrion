package io.github.vibhor1102.macrion.core.common.actions.sound

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SoundExecutorTest {
    @Test fun stopDuringPreparationReturnsWithoutStartingStaleSound() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        whenever(player.prepare()).thenAnswer { executor.stopSound(); null }
        executor.playSound("content://sound/one")
        runCurrent()
        verify(player, never()).start()
        verify(player).release()
    }

    @Test fun queuedSoundIsDiscardedWhenStoppedBeforeWorkerRuns() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { executor.playSound("content://sound/one") }
        executor.stopSound()
        runCurrent()
        pending.await()
        verifyNoInteractions(player)
    }

    @Test fun staleStopDoesNotReleaseNewerSoundAndLateCompletionIsIgnored() = runTest {
        val first = mock<MediaPlayer>()
        val second = mock<MediaPlayer>()
        val players = ArrayDeque(listOf(first, second))
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { players.removeFirst() }
        executor.playSound("content://sound/one")
        runCurrent()
        val completion = argumentCaptor<MediaPlayer.OnCompletionListener>()
        verify(first).setOnCompletionListener(completion.capture())
        executor.stopSound()
        val replacement = async(start = CoroutineStart.UNDISPATCHED) { executor.playSound("content://sound/two") }
        runCurrent()
        replacement.await()
        completion.firstValue.onCompletion(first)
        runCurrent()
        verify(first, times(1)).release()
        verify(second).start()
        verify(second, never()).release()
        executor.stopSound()
        runCurrent()
        verify(second).release()
    }

    @Test fun preparationFailureReleasesCandidateWithoutStarting() = runTest {
        val player = mock<MediaPlayer>()
        whenever(player.setDataSource(any<Context>(), any<Uri>())).thenThrow(IllegalArgumentException("missing"))
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        executor.playSound("content://sound/missing")
        runCurrent()
        verify(player).release()
        verify(player, never()).start()
    }
    @Test fun scenarioWaitsForStartButNotForAudioCompletion() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        var nextActionRan = false
        whenever(player.prepare()).thenAnswer {
            assertFalse(nextActionRan)
            verify(player, never()).start()
            null
        }
        whenever(player.start()).thenAnswer { assertFalse(nextActionRan); null }
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            executor.playSound("content://sound/one")
            nextActionRan = true
        }
        assertFalse(nextActionRan)
        runCurrent()
        pending.await()
        assertTrue(nextActionRan)
        verify(player).start()
        // No completion callback was delivered; the sound can keep playing during later actions.
        verify(player, never()).release()
        executor.stopSound()
        runCurrent()
    }

    @Test fun callerCancellationDuringPreparationPreventsStartAndNextAction() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        lateinit var pending: Deferred<Unit>
        var nextActionRan = false
        whenever(player.prepare()).thenAnswer { pending.cancel(); null }
        pending = async(start = CoroutineStart.UNDISPATCHED) {
            executor.playSound("content://sound/one")
            nextActionRan = true
        }
        runCurrent()
        assertTrue(pending.isCancelled)
        assertFalse(nextActionRan)
        verify(player, never()).start()
        verify(player).release()
    }

    @Test fun callerCancellationBeforeWorkerRunsDiscardsRequest() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { executor.playSound("content://sound/one") }
        pending.cancel()
        runCurrent()
        assertTrue(pending.isCancelled)
        verifyNoInteractions(player)
    }

    @Test fun cancellingSupersededCallerDoesNotCancelNewerSound() = runTest {
        val player = mock<MediaPlayer>()
        val executor = SoundExecutor(mock<Context>(), StandardTestDispatcher(testScheduler)) { player }
        val old = async(start = CoroutineStart.UNDISPATCHED) { executor.playSound("content://sound/old") }
        val replacement = async(start = CoroutineStart.UNDISPATCHED) { executor.playSound("content://sound/new") }
        old.cancel()
        runCurrent()
        replacement.await()
        verify(player, times(1)).start()
        verify(player, never()).release()
        executor.stopSound()
        runCurrent()
        verify(player).release()
    }

}
