package com.example.birdnettest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.birdnettest.ui.main.MainFragment
import java.io.*
import kotlin.math.ceil


class MainActivity : AppCompatActivity() {
    private lateinit var myBird: BirdNet
    private val pathToBirdCall = "MountainChickadee.wav" // Audio file with bird call

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myBird = BirdNet(applicationContext) // initialize birdnet

        // Check whether or not the application has permission to read/write files.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            accessAudioFiles()
        } else {
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    fun runBirdNet(view: View){
        // Reads/Writes audio file from Downloads folder
        //val audioFiles = accessAudioFiles()
        //Log.d("audioFilesCount", audioFiles.size.toString())
        //val audioFile = audioFiles[0]
        //val path = audioFile.data
        //Log.d("AUDIO FILE PATH", path.toString())
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + pathToBirdCall
        val data = myBird.runTest(path)

        if(data == null || data.size == 0) {
            return
        }

        val dir = File(this.filesDir.toString())

        // dropdown size is size of data
        var secondsList = arrayListOf<String>();

        try {
            val resultsFile = File(dir, pathToBirdCall.substring(0, pathToBirdCall.indexOf('.'))+"-result.txt")
            val writer = FileWriter(resultsFile)

            for(i in data.indices) {
                val start = 3*i+1
                val end = start+2

                secondsList.add("$start-$end s")
                writer.append("$start-$end\n")

                data[i].forEachIndexed { i, element ->
                    val name: String = element.first
                    val probability: Float = element.second

                    writer.append("$name: $probability\n");
                }
            }

            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
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

    private fun accessAudioFiles(): List<AudioFileAccessor.AudioFile> {
        val audioFileAccessor = AudioFileAccessor()
        val audioFiles = audioFileAccessor.getAudioFiles(contentResolver)
        for (path in audioFiles) {
            val data = myBird.runTest(path.data)
            if (data != null && data.size != 0) {
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
        }
        return audioFiles
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
                            grantResults[1] == PackageManager.PERMISSION_GRANTED)   // write permission
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
