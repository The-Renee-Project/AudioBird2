package com.example.birdnettest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

    // Array of all permissions to request
    private val PERMISSIONS = listOf(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        util = Util(applicationContext)

        // Check whether or not the application has permission to read/write files.
        if (hasPermissions(PERMISSIONS)) {
            runBirdNetDaily()
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS.toTypedArray(),
                100
            )
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    /*
     * Check that all permissions have been granted
     */
    private fun hasPermissions(permissions: List<String>): Boolean {
        var allGranted = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                Log.d("Permission denied", permission)
                allGranted = false
            }
        }
        return allGranted
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

        util.runBirdNet(bars,
                        textViews,
                        findViewById(R.id.audioName),
                        findViewById(R.id.spinner),
                        applicationContext)
    }

    /*
     * After checking for permissions starts periodic task to run app
     */
    private fun runBirdNetDaily() {
        val birdNetWorkRequest = PeriodicWorkRequestBuilder<BirdnetWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("EXECUTE_DAILY", ExistingPeriodicWorkPolicy.KEEP, birdNetWorkRequest)
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
                        runBirdNetDaily()
                    }
                }
            }
        }
    }
}