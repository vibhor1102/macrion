package io.github.vibhor1102.macrion.core.detection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NativeDetectorLifecycleTests {

    @Test
    fun closeBeforeInitializationDoesNotCrash() {
        val detector = NativeDetector.newInstance()
        assertNotNull(detector)

        detector!!.close()
        detector.close()
    }
}
