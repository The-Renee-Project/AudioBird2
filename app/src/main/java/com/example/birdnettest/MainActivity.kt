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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        util = Util(applicationContext)

        // Check whether or not the application has permission to read/write files.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            accessAudioFiles()
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
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
    private fun accessAudioFiles() {
        val birdNetWorkRequest = PeriodicWorkRequestBuilder<BirdnetWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("EXECUTE_DAILY", ExistingPeriodicWorkPolicy.KEEP, birdNetWorkRequest)
    }

    // https://developer.android.com/training/permissions/requesting
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                     grantResults[0] == PackageManager.PERMISSION_GRANTED && // read permission
                     grantResults[1] == PackageManager.PERMISSION_GRANTED  // write permission
                     )  // audio media permission
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    accessAudioFiles()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}