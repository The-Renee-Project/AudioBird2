package com.example.birdnettest

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.birdnettest.ui.main.MainFragment
import kotlin.math.ceil


class MainActivity : AppCompatActivity() {
    private lateinit var myBird: BirdNet
    private val pathToBirdCall = "birds-chirping.mp3" // Audio file with bird call

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        myBird = BirdNet(applicationContext)
        //runBirdNet(view)

        // Check whether or not the application has permission to access files.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            accessAudioFiles()
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
        }
    }

    fun runBirdNet(view: View){
        // Reads/Writes audio file from Downloads folder
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + pathToBirdCall
        val data = myBird.runTest(path)

        if(data == null || data.size == 0) {
            return
        }

        // dropdown size is size of data
        var secondsList = arrayListOf<String>();

        for(i in data.indices) {
            val start = 3*i
            val end = start+3
            secondsList.add("$start-$end s")
        }

        var spinner : Spinner = findViewById(R.id.spinner);

        var arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, secondsList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.adapter = arrayAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateViewsAndBars(data[position])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }
        }

        spinner.visibility = View.VISIBLE

        // default to first 3 seconds of data shown
        updateViewsAndBars(data[0])
    }

    private fun updateViewsAndBars(confidences: ArrayList<Pair<String,Float>>) {
        val textViews = getViews()
        val progressBars = getBars()

        confidences.forEachIndexed { i, element ->
            textViews[i].text = element.first
            progressBars[i].progress = (ceil(element.second * 100)).toInt()
            textViews[i].visibility = View.VISIBLE
            progressBars[i].visibility = View.VISIBLE
        }
    }

    private fun getViews(): ArrayList<TextView> {
        var views = arrayListOf<TextView>()

        views.add(findViewById(R.id.confidenceOne))
        views.add(findViewById(R.id.confidenceTwo))
        views.add(findViewById(R.id.confidenceThree))
        views.add(findViewById(R.id.confidenceFour))
        views.add(findViewById(R.id.confidenceFive))

        return views
    }

    private fun getBars(): ArrayList<ProgressBar> {
        var bars = arrayListOf<ProgressBar>()

        bars.add(findViewById(R.id.determinateBarOne))
        bars.add(findViewById(R.id.determinateBarTwo))
        bars.add(findViewById(R.id.determinateBarThree))
        bars.add(findViewById(R.id.determinateBarFour))
        bars.add(findViewById(R.id.determinateBarFive))

        return bars
    }

    private fun accessAudioFiles() {
        val audioFileAccessor = AudioFileAccessor()
        val audioFiles = audioFileAccessor.getAudioFiles(contentResolver)
        var audioFileText = ""
        if (audioFiles.isEmpty()) {
            audioFileText = "No audio files found in the media store."
        } else {
            // Loop over each of the audio files retrieved and display the file path on the screen.
            for (audioFile in audioFiles) {
                audioFileText += audioFile.data + "\n "
            }
        }
        Log.d("AUDIO FILE TEXT", audioFileText);
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
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
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
