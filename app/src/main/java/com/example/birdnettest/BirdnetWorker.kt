package com.example.birdnettest

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileWriter
import java.util.Calendar

class BirdnetWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private val util = Util(appContext)

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        util.runBirdNet()
        try {
            // get all result files or 0 if none exist
            val totalFiles = applicationContext.filesDir.list { _, name -> name.endsWith("-result.csv") }?.size ?: 0
            FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                out.write("\n------------------------------------------------------------------------\n")
                out.write("BirdNET Worker Completed Successfully: ${Calendar.getInstance().time}\n")
                out.write("Count Result Files: $totalFiles\n")
                out.write("--------------------------------------------------------------------------\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
