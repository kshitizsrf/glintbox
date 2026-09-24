package com.glintbox.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.glintbox.app.appContainer
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.MediaType
import com.glintbox.app.data.model.SaveResult
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically copies new statuses to the save folder. Runs only while the user has
 * enabled Auto-save, entirely on-device, and only inside folders the user granted.
 */
class AutoSaveWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val settings = container.settings.settings.first()
        if (!settings.autoSave) return Result.success()

        val target = container.saves.resolveTarget(settings)
        val existing = container.saves.listSaved(target).mapTo(HashSet()) { it.name }
        var saved = 0

        for (source in settings.enabledSources) {
            if (container.statuses.accessState(source, settings) != AccessState.GRANTED) continue
            val items = container.statuses.load(source, settings).getOrNull() ?: continue
            for (item in items) {
                if (isStopped) break
                if (!settings.autoSaveVideos && item.type == MediaType.VIDEO) continue
                if (item.name in existing) continue
                if (container.saves.save(item, target, existing) is SaveResult.Saved) {
                    existing += item.name
                    saved++
                }
            }
        }
        if (saved > 0) container.settings.addSaved(saved)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "glintbox_auto_save"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoSaveWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
