package io.github.vibhor1102.macrion.core.display.recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito.mock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MediaProjectionRequestTests {
    @Test fun recreatedOwnerReceivesPendingResultWithoutLaunchingAgain() {
        val caller = ResultCaller()
        val request = MediaProjectionRequest()
        request.registerForActivityResult(caller)
        var granted = 0
        var denied = 0
        var error = 0
        request.showMediaProjectionWarning(mock(Context::class.java), false,
            { _, _ -> granted++ }, { denied++ }, { error++ }, launchRequest = false)
        assertEquals(0, caller.launchCount)
        caller.deliver(ActivityResult(Activity.RESULT_OK, Intent()))
        assertEquals(1, granted)
        caller.deliver(ActivityResult(Activity.RESULT_OK, Intent()))
        assertEquals("A result callback must only complete once", 1, granted)
        assertEquals(0, denied)
        assertEquals(0, error)
    }

    @Test fun cancellationAndMissingProjectionServiceHaveCompletionCallbacks() {
        val caller = ResultCaller()
        val request = MediaProjectionRequest()
        request.registerForActivityResult(caller)
        var denied = 0
        var error = 0
        request.showMediaProjectionWarning(mock(Context::class.java), false,
            { _, _ -> fail("Unexpected success") }, { denied++ }, { error++ }, launchRequest = false)
        caller.deliver(ActivityResult(Activity.RESULT_CANCELED, null))
        assertEquals(1, denied)
        request.showMediaProjectionWarning(mock(Context::class.java), false,
            { _, _ -> fail("Unexpected success") }, { denied++ }, { error++ })
        assertEquals(1, error)
        caller.deliver(ActivityResult(Activity.RESULT_CANCELED, null))
        assertEquals(1, denied)
    }

    private class ResultCaller : ActivityResultCaller {
        var launchCount = 0
        private lateinit var callback: ActivityResultCallback<ActivityResult>
        fun deliver(result: ActivityResult) = callback.onActivityResult(result)

        @Suppress("UNCHECKED_CAST")
        override fun <I, O> registerForActivityResult(contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
            this.callback = callback as ActivityResultCallback<ActivityResult>
            return object : ActivityResultLauncher<I>() {
                override fun launch(input: I, options: androidx.core.app.ActivityOptionsCompat?) { launchCount++ }
                override fun unregister() = Unit
                override fun getContract(): ActivityResultContract<I, *> = contract
            }
        }

        override fun <I, O> registerForActivityResult(contract: ActivityResultContract<I, O>, registry: ActivityResultRegistry, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> =
            registerForActivityResult(contract, callback)
    }
}
