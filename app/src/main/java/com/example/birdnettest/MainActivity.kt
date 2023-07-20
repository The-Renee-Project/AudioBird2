package com.example.birdnettest

import android.Manifest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

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
     * Function associated with button click
     * runs inference, and outputs results to screen as well as saving to file
     */
    fun runBirdNet(view: View) {
        val bars: Array<ProgressBar> = arrayOf(
            findViewById(R.id.determinateBarOne),
            findViewById(R.id.determinateBarTwo),
            findViewById(R.id.determinateBarThree),
            findViewById(R.id.determinateBarFour),
            findViewById(R.id.determinateBarFive)
        )

        val textViews: Array<TextView> = arrayOf(
            findViewById(R.id.confidenceOne),
            findViewById(R.id.confidenceTwo),
            findViewById(R.id.confidenceThree),
            findViewById(R.id.confidenceFour),
            findViewById(R.id.confidenceFive)
        )

        util.runBirdNet(
            bars,
            textViews,
            findViewById(R.id.audioName),
            findViewById(R.id.spinner),
            applicationContext
        )
    }
}