package com.satwik.oodapplication.worker

import android.content.Context
import androidx.work.*
import com.satwik.oodapplication.data.model.LockStatus
import com.satwik.oodapplication.domain.repository.FoodCountRepository
import com.satwik.oodapplication.utils.Constants
import com.satwik.oodapplication.utils.Resource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

class LockAutomationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LockAutomationEntryPoint {
        fun foodCountRepository(): FoodCountRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            LockAutomationEntryPoint::class.java
        )
        val repository = entryPoint.foodCountRepository()

        // Wait for actual data, skip Loading state
        val lockResource = repository.getLockStatus(Constants.ACTIVE_LOCK_ID)
            .filterIsInstance<Resource.Success<LockStatus>>()
            .first()
            
        val currentLock = lockResource.data ?: return Result.success()

        if (!currentLock.automationEnabled) {
            return Result.success()
        }

        val now = Calendar.getInstance()
        val hour = now[Calendar.HOUR_OF_DAY]
        val minute = now[Calendar.MINUTE]
        val m = hour * 60 + minute

        // 100% RELIABLE STATE-BASED LOGIC:
        // Instead of narrow windows, we define the "Correct State" for any given minute of the day.
        
        // Is it the Reset/Unlock period? (8:00 PM to 5:00 AM)
        val isResetPeriod = m >= 1200 || m < 300
        
        // Define Target States based on the current minute
        val targetB = !isResetPeriod && m >= 300 // Lock B from 5 AM to 8 PM
        val targetL = !isResetPeriod && m >= 540 // Lock L from 9 AM to 8 PM
        val targetD = !isResetPeriod && m >= 990 // Lock D from 4:30 PM to 8 PM
        
        // Universal Unlock Logic (Covers the entire 8 PM to 5 AM period)
        if (isResetPeriod) {
            if (currentLock.locked) repository.updateSingleLock("locked", false)
            if (currentLock.breakfastLocked) repository.updateSingleLock("breakfastLocked", false)
            if (currentLock.lunchLocked) repository.updateSingleLock("lunchLocked", false)
            if (currentLock.dinnerLocked) repository.updateSingleLock("dinnerLocked", false)
        } else {
            // Daytime Locking Logic: Apply targets only if they differ from current state
            if (currentLock.breakfastLocked != targetB) {
                repository.updateSingleLock("breakfastLocked", targetB)
            }
            if (currentLock.lunchLocked != targetL) {
                repository.updateSingleLock("lunchLocked", targetL)
            }
            if (currentLock.dinnerLocked != targetD) {
                repository.updateSingleLock("dinnerLocked", targetD)
            }
        }

        scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        fun scheduleNext(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<LockAutomationWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .addTag("lock_automation")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "lock_automation_task_v2",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun startImmediately(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<LockAutomationWorker>()
                .addTag("lock_automation")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "lock_automation_task_v2",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
