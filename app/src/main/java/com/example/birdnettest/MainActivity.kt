package com.example.birdnettest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.birdnettest.ui.main.MainFragment
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var util: Util

    private lateinit var statusTable: TableLayout
    // Array of all confidence bars
    private lateinit var bars: Array<ProgressBar>
    // Array of bird names
    private lateinit var textViews: Array<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        statusTable = findViewById(R.id.statusTable)
        bars = arrayOf(
            findViewById(R.id.determinateBarOne),
            findViewById(R.id.determinateBarTwo),
            findViewById(R.id.determinateBarThree),
            findViewById(R.id.determinateBarFour),
            findViewById(R.id.determinateBarFive)
        )
        textViews = arrayOf(
            findViewById(R.id.confidenceOne),
            findViewById(R.id.confidenceTwo),
            findViewById(R.id.confidenceThree),
            findViewById(R.id.confidenceFour),
            findViewById(R.id.confidenceFive)
        )

        // Array of all permissions to request
        // Handle permissions for Android 12 and greater
        val permissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
            // Handle permissions for versions under Android 12
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        // Request manage external storage permissions for Android 11 or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
        // Check whether or not the application has permission to read/write files.
        if (hasPermissions(permissions)) {
            runJobWorkers()
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                this,
                permissions,
                100
            )
        }

        util = Util(applicationContext)
    }

    /*
     * Check that all permissions have been granted
     */
    private fun hasPermissions(permissions: Array<String>): Boolean {
        var allGranted = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                Log.d("Permission not granted yet", permission)
                allGranted = false
            }
        }
        return allGranted
    }

    // https://developer.android.com/training/permissions/requesting
    /*
     * Request runtime permission from array
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()) {
                    var denied = false
                    for (i in grantResults.indices) {
                        // Permission has been denied
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            Log.d("Permission denied", permissions[i])
                            denied = true
                        }
                    }
                    // If all permissions are granted start background task
                    if (!denied) {
                        runJobWorkers()
                    }
                }
            }
        }
    }

    /*
     * After checking for permissions starts periodic tasks to run app
     */
    private fun runJobWorkers() {
        // Logger
        val loggerWorkRequest =
            PeriodicWorkRequestBuilder<LoggerWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork("LOGGER", ExistingPeriodicWorkPolicy.KEEP, loggerWorkRequest)
        // BirdNET app
        val birdNetWorkRequest =
            PeriodicWorkRequestBuilder<BirdnetWorker>(3, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "EXECUTE_BIRDNET",
            ExistingPeriodicWorkPolicy.KEEP,
            birdNetWorkRequest
        )
    }

    /*
     * Restart logger
     * Will delete existing worker and start new worker at current time
     */
    fun restartLogger(view: View) {
        WorkManager.getInstance(applicationContext).cancelUniqueWork("LOGGER")
        // Logger
        val loggerWorkRequest =
            PeriodicWorkRequestBuilder<LoggerWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork("LOGGER", ExistingPeriodicWorkPolicy.KEEP, loggerWorkRequest)
    }

    /*
     * Restart birdnet worker
     * Will delete existing worker and start new worker at current time
     */
    fun restartBird(view: View) {
        WorkManager.getInstance(applicationContext).cancelUniqueWork("EXECUTE_BIRDNET")
        // BirdNET app
        val birdNetWorkRequest =
            PeriodicWorkRequestBuilder<BirdnetWorker>(3, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "EXECUTE_BIRDNET",
            ExistingPeriodicWorkPolicy.KEEP,
            birdNetWorkRequest
        )
    }

    /*
     * Show status of app and workers
     */
    fun updateStatus(view: View) {
        for (i in bars.indices) {
            bars[i].visibility = View.INVISIBLE
            textViews[i].visibility = View.INVISIBLE
        }
        findViewById<TableLayout>(R.id.FileStatus).visibility = View.INVISIBLE
        statusTable.visibility = View.VISIBLE

        // Update status - RUNNING, ENQUEUED, STOPPED, ...
        val loggerStatus = WorkManager.getInstance(applicationContext).getWorkInfosForUniqueWork("LOGGER")
        val birdStatus = WorkManager.getInstance(applicationContext).getWorkInfosForUniqueWork("EXECUTE_BIRDNET")
        try {
            val logInfos = loggerStatus.get()
            for (info in logInfos) {
                findViewById<TextView>(R.id.logStatus).text = "${info.state}"
            }

            val birdInfos = birdStatus.get()
            for (info in birdInfos) {
                findViewById<TextView>(R.id.birdStatus).text = "${info.state}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Keep track of last execution
        var prefs = applicationContext.getSharedPreferences("BirdNET_last_execution", Context.MODE_PRIVATE)
        findViewById<TextView>(R.id.birdNETLastRun).text = prefs.getString("timestamp", "N/A")
        prefs = applicationContext.getSharedPreferences("Logger_last_execution", Context.MODE_PRIVATE)
        findViewById<TextView>(R.id.LoggerLastRun).text = prefs.getString("timestamp", "N/A")
        val totalFiles = applicationContext.filesDir.list { _, name -> name.endsWith("-result.csv") }?.size ?: 0
        findViewById<TextView>(R.id.filesProcessed).text = totalFiles.toString()
    }

    /*
     * Function associated with button click
     * runs inference, and outputs results to screen as well as saving to file
     */
    fun runBirdNet(view: View) {
        statusTable.visibility = View.INVISIBLE
        findViewById<TableLayout>(R.id.FileStatus).visibility = View.VISIBLE

        util.runBirdNet(findViewById(R.id.ProcessStatus),
            findViewById(R.id.progressBar),
            findViewById(R.id.audioName),
            bars,
            textViews,
            findViewById(R.id.spinner))
    }
}