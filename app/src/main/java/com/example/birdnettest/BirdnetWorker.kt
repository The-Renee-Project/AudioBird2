package com.example.birdnettest

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.FileWriter
import java.util.Calendar

class BirdnetWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private val util = Util(appContext)
    private val ctx = appContext

    /*
     * Write stats to log
     */
    fun writeToLog(start: java.util.Date) {
        // Write to log
        val totalFiles =
            ctx.filesDir.list { _, name -> name.endsWith("-result.csv") }?.size
                ?: 0
        // Calculate how many new files were created
        val prefs = ctx.getSharedPreferences("files_processed", Context.MODE_PRIVATE)
        var newFiles = prefs.getInt("processed", 0)
        newFiles = totalFiles - newFiles
        with(prefs.edit()) {
            putInt("processed", totalFiles)
            apply() // asynchronous write to external memory
        }
        // Write to log
        FileWriter("${ctx.filesDir}/AudioBird-Log.txt", true).use { out ->
            out.write("\n------------------------------------------------------------------------\n")
            out.write("BirdNET worker Started Successfully: $start\n")
            out.write("BirdNET worker Completed Successfully: ${Calendar.getInstance().time}\n")
            out.write("Total Files Found: $totalFiles\n")
            out.write("New Files Processed: $newFiles\n")
            out.write("--------------------------------------------------------------------------\n")
        }
    }

    /*
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        MediaScannerConnection.scanFile(
            ctx, arrayOf(Environment.getExternalStorageDirectory().path), null
        ) { path, uri_ ->
            val start = Calendar.getInstance().time
            try {
                util.runBirdNet()
                // Write result to log
                writeToLog(start)
            } catch (e: Exception) {
                e.printStackTrace()
                FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                    out.write("\n------------------------------------------------------------------------\n")
                    out.write("BirdNET worker Started Successfully: $start\n")
                    out.write("BirdNET Worker Stopped: ${Calendar.getInstance().time}\n")
                    out.write("Failed with error: ${e.message}")
                    out.write("--------------------------------------------------------------------------\n")
                }
            }
        }
        // Keep track of last execution
        val prefs = ctx.getSharedPreferences("BirdNET_last_execution", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("timestamp", Calendar.getInstance().time.toString())
            apply() // asynchronous write to external memory
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
