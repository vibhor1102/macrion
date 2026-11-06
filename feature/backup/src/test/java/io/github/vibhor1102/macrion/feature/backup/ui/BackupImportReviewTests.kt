package io.github.vibhor1102.macrion.feature.backup.ui

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.feature.backup.data.*
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.domain.Backup
import io.github.vibhor1102.macrion.feature.backup.domain.BackupRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BackupImportReviewTests {
    @Test fun partialRecoveryRequiresBothExplicitReviewAndConfirmation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val plan = plan(warning = true)
        try {
            val repository = mockk<BackupRepository>()
            val display = mockk<DisplayConfigManager>()
            every { display.displayConfig.sizePx } returns Point(1080, 1920)
            coEvery { repository.prepareScenarioImport(any()) } returns plan
            every { repository.restorePreparedScenarioBackup(plan, any(), true) } returns flowOf(Backup.Completed(1, 0, false))
            val context = ApplicationProvider.getApplicationContext<Context>()
            val viewModel = BackupViewModel(repository, display)
            viewModel.initialize(context, true)
            viewModel.startBackup(context, Uri.parse("content://backup/test"), true)
            runCurrent()
            assertTrue(viewModel.backupState.value!!.canTryPartialImport)
            verify(exactly = 0) { repository.restorePreparedScenarioBackup(any(), any(), any()) }
            viewModel.reviewPartialImport(context)
            assertTrue(viewModel.backupState.value!!.importReviewReady)
            verify(exactly = 0) { repository.restorePreparedScenarioBackup(any(), any(), any()) }
            viewModel.confirmPartialImport(context)
            viewModel.confirmPartialImport(context) // A double tap must not restore twice.
            runCurrent()
            verify(exactly = 1) { repository.restorePreparedScenarioBackup(plan, any(), true) }
            assertFalse(plan.directory.exists())
        } finally { plan.close(); Dispatchers.resetMain() }
    }

    @Test fun cancellingReviewDeletesThePrivateSnapshotWithoutRestoringAnything() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val plan = plan(warning = true)
        try {
            val repository = mockk<BackupRepository>()
            coEvery { repository.prepareScenarioImport(any()) } returns plan
            val context = ApplicationProvider.getApplicationContext<Context>()
            val model = BackupViewModel(repository, mockk())
            model.initialize(context, true)
            model.startBackup(context, Uri.parse("content://backup/test"), true)
            runCurrent()
            model.reset()
            assertFalse(plan.directory.exists())
            verify(exactly = 0) { repository.restorePreparedScenarioBackup(any(), any(), any()) }
        } finally { plan.close(); Dispatchers.resetMain() }
    }

    @Test fun fullyCompatibleDataUsesStrictImportWithoutOfferingFallback() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val plan = plan(warning = false)
        try {
            val repository = mockk<BackupRepository>()
            val display = mockk<DisplayConfigManager>()
            every { display.displayConfig.sizePx } returns Point(1080, 1920)
            coEvery { repository.prepareScenarioImport(any()) } returns plan
            every { repository.restorePreparedScenarioBackup(plan, any(), false) } returns flowOf(Backup.Completed(1, 0, false))
            val context = ApplicationProvider.getApplicationContext<Context>()
            val model = BackupViewModel(repository, display)
            model.initialize(context, true)
            model.startBackup(context, Uri.parse("content://backup/test"), true)
            runCurrent()
            verify(exactly = 1) { repository.restorePreparedScenarioBackup(plan, any(), false) }
            assertFalse(model.backupState.value!!.canTryPartialImport)
            assertFalse(plan.directory.exists())
        } finally { plan.close(); Dispatchers.resetMain() }
    }

    private fun plan(warning: Boolean): BackupImportPlan {
        val directory = Files.createTempDirectory("backup-review-test").toFile()
        val source = directory.resolve("source.zip").apply { writeText("private snapshot") }
        return BackupImportPlan(directory, source, BackupArchiveFormat.KLICKR_COMPATIBLE,
            mapOf("1/1.json" to directory.resolve("scenario.json").apply { writeText("{}") }),
            if (warning) listOf(ImportIssue("Scenario", "", ImportIssueKind.VERSION, "999")) else emptyList())
    }
}
