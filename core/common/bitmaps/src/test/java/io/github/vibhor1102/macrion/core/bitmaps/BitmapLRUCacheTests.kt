package io.github.vibhor1102.macrion.core.bitmaps

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.graphics.Bitmap
import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalCoroutinesApi::class)
class BitmapLRUCacheTests {
    @Test fun cacheMissSuspendsWithoutBlockingOtherUiWorkAndCachesResult() = kotlinx.coroutines.test.runTest {
        val cache = BitmapLRUCache()
        val image = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val loaded = kotlinx.coroutines.CompletableDeferred<Bitmap?>()
        var uiRan = false
        val pending = async {
            cache.getImageConditionBitmapOrDefault("image", 32, 32) { loaded.await() }
        }
        launch { uiRan = true }
        runCurrent()
        assertTrue(uiRan)
        assertFalse(pending.isCompleted)
        loaded.complete(image)
        assertSame(image, pending.await())
        assertSame(image, cache.getImageConditionBitmapOrDefault("image", 32, 32) { error("cache miss") })
    }

    @Test fun workingSetBudgetEvictsStaleImagesAndResetsAfterDetection() {
        val cache = BitmapLRUCache()
        val defaultSize = cache.maxSize()
        cache.resizeForWorkingSet(16 * 1024)
        cache.putImageConditionBitmap("first", 64, 64, Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888))
        cache.putImageConditionBitmap("second", 64, 64, Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888))
        assertEquals(1, cache.snapshot().size)
        assertTrue(cache.snapshot().keys.single().contains("second"))
        cache.resetSize()
        assertEquals(defaultSize, cache.maxSize())
    }

    @Test fun budgetRemainsValidForEmptyAndOversizedWorkingSets() {
        val cache = BitmapLRUCache()
        cache.resizeForWorkingSet(0)
        assertEquals(1, cache.maxSize())
        cache.resizeForWorkingSet(Long.MAX_VALUE)
        assertTrue(cache.maxSize().toLong() <= Runtime.getRuntime().maxMemory() / 1024 / 4)
        assertTrue(cache.maxSize() > 1)
    }
}
