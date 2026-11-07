package io.github.vibhor1102.macrion.core.common.permissions

import android.content.Context
import android.app.NotificationManager
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionsControllerLifecycleTests {
    @Test fun recreationRebindsCallbacksAndKeepsThePendingPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("permissions", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        val controller = PermissionsController()
        var oldCompletions = 0
        var newCompletions = 0
        controller.startPermissionsUiFlow(context, listOf(PermissionPostNotification()), { oldCompletions++ }, { oldCompletions++ })
        val pending = controller.uiState.value
        assertTrue(pending is PermissionUiState.Request)
        controller.startPermissionsUiFlow(context, listOf(PermissionPostNotification()), { newCompletions++ }, { newCompletions++ }, resumeExisting = true)
        assertSame(pending, controller.uiState.value)
        controller.onPermissionResult(false)
        assertEquals(PermissionUiState.MandatoryDenied, controller.uiState.value)
        controller.onMandatoryDeniedDismissed()
        assertEquals(0, oldCompletions)
        assertEquals(1, newCompletions)
        controller.onPermissionResult(true)
        assertEquals(1, newCompletions)
        assertEquals(PermissionUiState.Idle, controller.uiState.value)
    }
}
