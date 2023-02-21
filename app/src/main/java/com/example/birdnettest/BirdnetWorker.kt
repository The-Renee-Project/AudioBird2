package com.example.birdnettest

import android.content.Context
import android.os.Environment
import androidx.work.Worker
import androidx.work.WorkerParameters

class BirdnetWorker (appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    private val myBird = BirdNet(appContext)
    private val pathToBirdCall = "Hawk.wav" // Audio file with bird call

    override fun doWork(): Result {
        // run birdnet inference
        runBirdNet()

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun runBirdNet() {
        // Reads/Writes audio file from Downloads folder
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + pathToBirdCall
        val data = myBird.runTest(path)
    }
}
