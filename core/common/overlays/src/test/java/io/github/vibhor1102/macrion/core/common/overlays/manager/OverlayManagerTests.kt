/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.common.overlays.manager

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.lifecycle.Lifecycle
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.core.common.overlays.other.FullscreenOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.common.overlays.base.Overlay
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import io.github.vibhor1102.macrion.core.common.overlays.testutils.anyNotNull

import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [OverlayManager] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayManagerTests {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockSharedPreferences: SharedPreferences
    @Mock private lateinit var mockSharedPrefsEditor: SharedPreferences.Editor
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockOverlay1: Overlay
    @Mock private lateinit var mockOverlay2: Overlay
    @Mock private lateinit var mockOverlay3: Overlay
    @Mock private lateinit var keyguardSource: KeyguardStateDataSource
    @Mock private lateinit var settings: SettingsRepository
    private val keyguardVisible = MutableStateFlow(false)
    private val showOnLockScreen = MutableStateFlow(false)

    private lateinit var overlayManager: OverlayManager

    private inline fun <reified T : Any?> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)

    private fun Overlay.setupDismissMock() {
        val cbCaptor = argumentCaptor<((Context, Overlay) -> Unit)?>()
        Mockito.verify(this).create(anyNotNull(), cbCaptor.capture())

        Mockito.doAnswer { cbCaptor.value?.invoke(mockContext, this) }
            .`when`(this).destroy()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        Mockito.`when`(mockContext.getSharedPreferences(anyNotNull(), Mockito.anyInt())).thenReturn(mockSharedPreferences)
        Mockito.`when`(mockSharedPreferences.edit()).thenReturn(mockSharedPrefsEditor)
        Mockito.`when`(mockSharedPrefsEditor.putInt(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockSharedPrefsEditor)
        Mockito.`when`(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
        Mockito.`when`(keyguardSource.isKeyguardVisibleFlow).thenReturn(keyguardVisible)
        Mockito.`when`(keyguardSource.isKeyguardVisible()).thenAnswer { keyguardVisible.value }
        Mockito.`when`(settings.showOverlaysOnLockScreenFlow).thenReturn(showOnLockScreen)
        Mockito.`when`(settings.showOverlaysOnLockScreen()).thenAnswer { showOnLockScreen.value }

        overlayManager = OverlayManager(
            displayConfigManager = DisplayConfigManager(mockContext),
            menuPositionDataSource = OverlayMenuPositionDataSource(
                mockContext
            ),
            keyguardStateDataSource = keyguardSource,
            settingsRepository = settings,
            mainDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun Overlay.withLifecycle(state: Lifecycle.State = Lifecycle.State.RESUMED) {
        val lifecycle = Mockito.mock(Lifecycle::class.java)
        Mockito.`when`(lifecycle.currentState).thenReturn(state)
        Mockito.`when`(this.lifecycle).thenReturn(lifecycle)
    }

    @Test fun lockingHidesAndUnlockingRestoresStackAndFullscreenOverlay() {
        mockOverlay1.withLifecycle()
        overlayManager.navigateTo(mockContext, mockOverlay1)
        val top = Mockito.mock(FullscreenOverlay::class.java).apply { withLifecycle() }
        Mockito.`when`(mockOverlay1.context).thenReturn(mockContext)
        overlayManager.setTopOverlay(top)
        Mockito.clearInvocations(mockOverlay1, top)

        keyguardVisible.value = true
        Mockito.verify(mockOverlay1).hide()
        Mockito.verify(top).hide()
        Assert.assertFalse(overlayManager.hasVisibleOverlayAboveRoot())
        Mockito.clearInvocations(mockOverlay1, top)
        keyguardVisible.value = false
        Mockito.verify(mockOverlay1).resume()
        Mockito.verify(top).resume()
    }

    @Test fun permissionHidingAndLockHidingNeverOverwriteEachOthersSavedState() {
        for (permissionFirst in listOf(true, false)) {
            mockOverlay1.withLifecycle()
            if (overlayManager.isEmpty()) overlayManager.navigateTo(mockContext, mockOverlay1)
            Mockito.clearInvocations(mockOverlay1)
            if (permissionFirst) { overlayManager.hideAll(); keyguardVisible.value = true }
            else { keyguardVisible.value = true; overlayManager.hideAll() }
            Mockito.verify(mockOverlay1).hide()

            keyguardVisible.value = false
            Mockito.verify(mockOverlay1, Mockito.never()).resume()
            overlayManager.restoreVisibility()
            Mockito.verify(mockOverlay1).resume()
        }
    }

    @Test fun permissionCompletionWhileLockedWaitsForUnlock() {
        mockOverlay1.withLifecycle()
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.hideAll()
        keyguardVisible.value = true
        Mockito.clearInvocations(mockOverlay1)
        overlayManager.restoreVisibility()
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
        keyguardVisible.value = false
        Mockito.verify(mockOverlay1).resume()
    }

    @Test fun overlaysCreatedWhileLockedWaitAndLivePreferenceChangesApplyImmediately() {
        keyguardVisible.value = true
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Mockito.verify(mockOverlay1, Mockito.never()).start()
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
        showOnLockScreen.value = true
        Mockito.verify(mockOverlay1).resume()
        mockOverlay1.withLifecycle()
        showOnLockScreen.value = false
        Mockito.verify(mockOverlay1).hide()
        keyguardVisible.value = false
        Mockito.verify(mockOverlay1, Mockito.times(2)).resume()
    }

    @Test fun navigationWhileLockedRestoresOnlyTheCurrentForegroundOverlay() {
        mockOverlay1.withLifecycle()
        overlayManager.navigateTo(mockContext, mockOverlay1)
        keyguardVisible.value = true
        Mockito.clearInvocations(mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2)
        Mockito.verify(mockOverlay2, Mockito.never()).start()
        Mockito.verify(mockOverlay2, Mockito.never()).resume()
        keyguardVisible.value = false
        Mockito.verify(mockOverlay1).start()
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
        Mockito.verify(mockOverlay2).resume()
    }

    @Test
    fun stackTop_emptyStack() {
        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateTo_initial() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        Assert.assertEquals(mockOverlay1, overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateTo_second() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2)

        Assert.assertEquals(mockOverlay2, overlayManager.getBackStackTop())
    }

    @Test
    fun hasVisibleOverlayAboveRoot_returnsTrueOnlyForChildOverlays() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Assert.assertFalse(overlayManager.hasVisibleOverlayAboveRoot())

        overlayManager.navigateTo(mockContext, mockOverlay2)
        Assert.assertTrue(overlayManager.hasVisibleOverlayAboveRoot())
    }

    @Test
    fun stackTop_navigateUp_initial() {
        overlayManager.navigateUp(mockContext)

        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateUp_second() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.setupDismissMock()

        overlayManager.navigateUp(mockContext)

        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun lifecycles_navigateTo_initialOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        Mockito.inOrder(mockOverlay1).apply {
            verify(mockOverlay1).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).start()
            verify(mockOverlay1).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1)
    }

    @Test
    fun lifecycles_navigateTo_noHide_secondOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).pause()
            verify(mockOverlay2).start()
            verify(mockOverlay2).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }

    @Test
    fun lifecycles_navigateTo_hide_secondOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = true)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).pause()
            verify(mockOverlay1).stop()
            verify(mockOverlay2).start()
            verify(mockOverlay2).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }

    @Test
    fun lifecycles_navigateTo_noHide_thirdOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateTo(mockContext, mockOverlay3, hideCurrent = false)

        Mockito.inOrder(mockOverlay2, mockOverlay3).apply {
            verify(mockOverlay3).create(anyNotNull(), anyNotNull())
            verify(mockOverlay2).pause()
            verify(mockOverlay3).start()
            verify(mockOverlay3).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2, mockOverlay3)
    }

    @Test
    fun lifecycles_navigateTo_hide_thirdOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateTo(mockContext, mockOverlay3, hideCurrent = true)

        Mockito.inOrder(mockOverlay2, mockOverlay3).apply {
            verify(mockOverlay3).create(anyNotNull(), anyNotNull())
            verify(mockOverlay2).pause()
            verify(mockOverlay2).stop()
            verify(mockOverlay3).start()
            verify(mockOverlay3).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2, mockOverlay3)
    }

    @Test
    fun lifecycles_navigateUp_oneOverlayInStack() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.setupDismissMock()
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateUp(mockContext)

        Mockito.inOrder(mockOverlay1).apply {
            verify(mockOverlay1).pause()
            verify(mockOverlay1).stop()
            verify(mockOverlay1).destroy()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1)
    }

    @Test
    fun lifecycles_navigateUp_twoOverlaysInStack() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2)
        mockOverlay2.setupDismissMock()
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateUp(mockContext)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).pause()
            verify(mockOverlay2).stop()
            verify(mockOverlay2).destroy()
            verify(mockOverlay1).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }
}
