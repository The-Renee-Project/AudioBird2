package com.example.birdnettest

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileWriter
import java.util.Calendar

class LoggerWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    /*
     * Add to log file
     */
    private fun logger() {
        try {
            FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                out.write("Logger Successfully Ran: ${Calendar.getInstance().time}\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        logger()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}