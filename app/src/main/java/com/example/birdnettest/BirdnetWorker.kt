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
     * Function that is run by task scheduler
     */
    override fun doWork(): Result {
        // run birdnet inference
        MediaScannerConnection.scanFile(
            ctx, arrayOf(Environment.getExternalStorageDirectory().path), null
        ) { path, uri_ ->
            try {
                util.runBirdNet()
                // Write result to log
                util.writeToLog(true)
            } catch (e: Exception) {
                e.printStackTrace()
                FileWriter("${applicationContext.filesDir}/AudioBird-Log.txt", true).use { out ->
                    out.write("\n------------------------------------------------------------------------\n")
                    out.write("BirdNET Worker Stopped: ${Calendar.getInstance().time}\n")
                    out.write("Failed with error: ${e.message}")
                    out.write("--------------------------------------------------------------------------\n")
                }
            }
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
