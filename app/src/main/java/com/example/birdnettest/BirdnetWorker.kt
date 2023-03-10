package com.example.birdnettest

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class BirdnetWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private val util = Util(appContext)

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        Log.d("TASK:", "Task ran!")
        util.runBirdNet()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
