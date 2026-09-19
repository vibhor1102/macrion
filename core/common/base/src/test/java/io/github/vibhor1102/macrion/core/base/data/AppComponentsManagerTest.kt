package io.github.vibhor1102.macrion.core.base.data

import android.content.ContextWrapper
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AppComponentsManagerTest {
    @Test
    fun tutorialTargetsTheInstalledPackageForEveryBuildIdentity() {
        for (installedPackage in listOf(
            "io.github.vibhor1102.macrion",
            "io.github.vibhor1102.macrion.debug",
            "abc.randomized.identity",
        )) {
            val context = object : ContextWrapper(null) {
                override fun getPackageName() = installedPackage
            }
            val component = AppComponentsManager(context).tutorialActivityComponentName
            assertEquals(installedPackage, component.packageName)
            assertEquals(
                "io.github.vibhor1102.macrion.feature.tutorial.ui.TutorialActivity",
                component.className,
            )
        }
    }
}
