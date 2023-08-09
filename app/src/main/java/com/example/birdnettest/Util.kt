package com.example.birdnettest

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil


class Util (appContext: Context) {
    private val myBird = BirdNet(appContext)
    private val ctx = appContext

    /*
     * Run birdnet on found files without outputting to screen
     * Used by the birdnet worker to run periodically
     */
    fun runBirdNet()
    {
        // Get all audio files from Downloads folder
        val audioFileAccessor = AudioFileAccessor()
        val audioFiles = audioFileAccessor.getAudioFiles(ctx.contentResolver)

        for (file in audioFiles) {
            // Only process files if they haven't been processed before, or have been updated
            if (!File(ctx.filesDir.toString(), "${file.title}-result.csv").exists()) {
                // Classify birds from audio recording
                val data = myBird.runTest(file.data)
                // Only process data if it exists
                if (data != null && data.size != 0) {
                    val secondsList = arrayListOf<String>()     // build list of chunks for seconds
                    saveToFile(data, secondsList, ctx.filesDir.toString(), file.title)    // save results from data to file
                }
            }
        }
    }

    /*
     * Run birdnet on found files and output to screen
     * Used for running on click
     */
    fun runBirdNet(filesProcessed: TextView,
                   filesProgress: ProgressBar,
                   audioName: TextView,
                   progressBars: Array<ProgressBar>,
                   textViews: Array<TextView>,
                   spinner: Spinner
    )
    {
        filesProcessed.visibility = View.VISIBLE
        filesProgress.visibility  = View.VISIBLE
        audioName.visibility      = View.VISIBLE

        MediaScannerConnection.scanFile(
            ctx, arrayOf(Environment.getExternalStorageDirectory().path), null
        ) { path, uri_ ->
            // Get all audio files from Downloads folder
            val audioFileAccessor = AudioFileAccessor()
            val audioFiles = audioFileAccessor.getAudioFiles(ctx.contentResolver)
            filesProcessed.text = "0/${audioFiles.size}"
            filesProgress.progress = 0
            filesProgress.max = 1000
            var total = 1

            Thread {
                for (file in audioFiles) {
                    try {
                        audioName.text = file.title
                        // Only process files if they haven't been processed before, or have been updated
                        if (!File(ctx.filesDir.toString(), "${file.title}-result.csv").exists()) {
                            // Classify birds from audio recording
                            val data = myBird.runTest(file.data)
                            // Only process data if it exists
                            if (data != null && data.size != 0) {
                                val secondsList =
                                    arrayListOf<String>()     // build list of chunks for seconds
                                saveToFile(
                                    data,
                                    secondsList,
                                    ctx.filesDir.toString(),
                                    file.title
                                )    // save results from data to file
                            }
                        }
                        filesProcessed.text = "$total/${audioFiles.size}"
                        filesProgress.progress = ((total.toDouble() / audioFiles.size) * 1000).toInt()
                        total++
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
                writeToLog(false)
            }.start()
        }
//        updateScreen(
//            data,
//            progressBars,
//            secondsList,
//            textViews,
//            ctx,
//            spinner
//        ) // print results to phone screen
    }

    /*
     * Write stats to log
     */
    fun writeToLog(isWorker: Boolean) {
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
            if (isWorker) {
                out.write("BirdNET worker Completed Successfully: ${Calendar.getInstance().time}\n")
            } else {
                out.write("Button Pressed, BirdNET Completed Successfully: ${Calendar.getInstance().time}\n")
            }
            out.write("Total Files Found: $totalFiles\n")
            out.write("New Files Processed: $newFiles\n")
            out.write("--------------------------------------------------------------------------\n")
        }
    }

    /*
     * Save results of birdnet to internal csv file
     */
    private fun saveToFile(data: ArrayList<ArrayList<Pair<String, Float>>>,
                           secondsList: ArrayList<String>,
                           filesDir: String,
                           path: String)
    {
        try {
            File("$filesDir/$path-result.csv").printWriter().use { out ->
                out.println("start_of_interval,end_of_interval,species,confidence")
                for (index in data.indices) {
                    secondsList.add("${3*index}-${3*index + 3} s")
                    data[index].forEachIndexed { _, element ->
                        out.println("${3*index}, ${3*index + 3}, ${element.first}, ${element.second}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
     * Create spinner with results of birdnet classifications
     */
    private fun updateScreen(data: ArrayList<ArrayList<Pair<String, Float>>>,
                             progressBars: Array<ProgressBar>,
                             secondsList: ArrayList<String>,
                             textViews: Array<TextView>,
                             appContext: Context,
                             spinner: Spinner)
    {
        val arrayAdapter =
            ArrayAdapter(appContext, android.R.layout.simple_spinner_item, secondsList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateViewsAndBars(data[position], progressBars, textViews)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spinner.visibility = View.VISIBLE

        // default to first 3 seconds of data shown
        updateViewsAndBars(data[0], progressBars, textViews)
    }

    /*
     * Output classifications results to screen
     */
    private fun updateViewsAndBars(confidences: ArrayList<Pair<String, Float>>,
                                   progressBars: Array<ProgressBar>,
                                   textViews: Array<TextView>)
    {
        confidences.forEachIndexed { i, element ->
            textViews[i].text = element.first
            progressBars[i].progress = (ceil(element.second * 100)).toInt()
            textViews[i].visibility = View.VISIBLE
            progressBars[i].visibility = View.VISIBLE
        }
    }
}