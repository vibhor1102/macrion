package io.github.vibhor1102.macrion.core.common.overlays.manager

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeyguardStateDataSourceTests {
    @Test fun screenOffScreenOnAndUnlockUseTheActualDeviceState() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyguard = shadowOf(context.getSystemService(KeyguardManager::class.java))
        val power = shadowOf(context.getSystemService(PowerManager::class.java))
        power.setIsInteractive(true)
        keyguard.setKeyguardLocked(false)
        val source = KeyguardStateDataSource(context)
        val states = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.isKeyguardVisibleFlow.collect { states += it } }
        runCurrent()
        assertFalse(states.last())

        fun broadcast(action: String) {
            context.sendBroadcast(Intent(action))
            shadowOf(Looper.getMainLooper()).idle()
            runCurrent()
        }
        power.setIsInteractive(false)
        broadcast(Intent.ACTION_SCREEN_OFF)
        assertTrue(states.last())
        power.setIsInteractive(true)
        keyguard.setKeyguardLocked(true)
        broadcast(Intent.ACTION_SCREEN_ON)
        assertTrue(states.last())
        // A broadcast alone must never make a still-locked device expose its controls.
        broadcast(Intent.ACTION_USER_PRESENT)
        assertTrue(states.last())
        keyguard.setKeyguardLocked(false)
        broadcast(Intent.ACTION_USER_PRESENT)
        assertFalse(states.last())
    }

    @Test fun aNewSubscriptionSamplesTheCurrentLockState() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyguard = shadowOf(context.getSystemService(KeyguardManager::class.java))
        shadowOf(context.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        keyguard.setKeyguardLocked(false)
        val source = KeyguardStateDataSource(context)
        assertFalse(source.isKeyguardVisibleFlow.first())
        keyguard.setKeyguardLocked(true)
        assertTrue(source.isKeyguardVisibleFlow.first())
    }
}
