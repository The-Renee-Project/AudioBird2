package com.example.birdnettest

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileWriter
import java.util.Calendar

class LoggerWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // Log to file
        try {
            FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                out.write("Logger Successfully Ran: ${Calendar.getInstance().time}\n")
            }
            // Keep track of last execution
            val prefs = applicationContext.getSharedPreferences("Logger_last_execution", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putString("timestamp", Calendar.getInstance().time.toString())
                apply() // asynchronous write to external memory
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}