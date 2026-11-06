package io.github.vibhor1102.macrion.core.smart.debugging.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.smart.debugging.domain.model.live.LiveDebugOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class LiveDebugOptionsTests {
    @Test fun displayOptionsPersistIndependentlyFromReportSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("DebugConfigPreferences", Context.MODE_PRIVATE).edit().clear().commit()
        val source = DebugConfigurationLocalDataSource(context)
        assertEquals(LiveDebugOptions(), source.liveOptions.value)
        source.setDebuggingConfig(debugView = true, debugReport = true)
        source.setLiveOptions(LiveDebugOptions(showEventStatistics = false, showActionIcons = true))
        val reloaded = DebugConfigurationLocalDataSource(context)
        assertEquals(LiveDebugOptions(false, true), reloaded.liveOptions.value)
        assertTrue(reloaded.isDebugReportEnabled())
        assertTrue(reloaded.isDebugViewEnabled())
    }
}
