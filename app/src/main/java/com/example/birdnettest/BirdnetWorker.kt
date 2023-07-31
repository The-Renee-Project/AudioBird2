package com.example.birdnettest

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileWriter
import java.util.Calendar

class BirdnetWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private val util = Util(appContext)
    private val ctx = appContext

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        util.runBirdNet()
        try {
            // get all result files or 0 if none exist
            val totalFiles = applicationContext.filesDir.list { _, name -> name.endsWith("-result.csv") }?.size ?: 0
            // Calculate how many new files were created
            val prefs = ctx.getSharedPreferences("files_processed", Context.MODE_PRIVATE)
            var newFiles = prefs.getInt("processed", 0)
            newFiles = totalFiles - newFiles
            with (prefs.edit()) {
                putInt("processed", totalFiles)
                apply() // asynchronous write to external memory
            }
            // Write to log
            FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                out.write("\n------------------------------------------------------------------------\n")
                out.write("BirdNET Worker Completed Successfully: ${Calendar.getInstance().time}\n")
                out.write("Total Files Found: $totalFiles\n")
                out.write("New Files Processed: $newFiles\n")
                out.write("--------------------------------------------------------------------------\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
