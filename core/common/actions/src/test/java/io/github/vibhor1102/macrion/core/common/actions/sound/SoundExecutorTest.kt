package io.github.vibhor1102.macrion.core.common.actions.sound

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Test
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
        executor.playSound("content://sound/one")
        executor.stopSound()
        runCurrent()
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
        executor.playSound("content://sound/two")
        runCurrent()
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
}
