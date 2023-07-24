package com.example.birdnettest

import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import java.io.File
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
        // Copy files from internal storage of audiomoth app - requires root
        try {
            val proc = Runtime.getRuntime().exec(
                arrayOf(
                    "su",
                    "-c",
                    "cp /data/user/0/org.nativescript.AudioMoth9/files/* /sdcard/Download"
                )
            )
            proc.waitFor()
        } catch (e: Exception) {
            Log.d("Exceptions", "Exception: $e")
        }
        // Get all audio files from Downloads folder
        val audioFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles {
                dir, filename ->
            File(dir, filename).isFile && (filename.endsWith(".aac") ||
                    filename.endsWith(".wav") ||
                    filename.endsWith(".mp4"))
        }
            ?: return // stop if null or no files found
        Log.d("Found files", "${audioFiles.size}")

        for (file in audioFiles) {
            // Only process files if they haven't been processed before, or have been updated
            if (!File(ctx.filesDir.toString(), "${file.nameWithoutExtension}-result.csv").exists()) {
                // Classify birds from audio recording
                val data = myBird.runTest(file.absolutePath)
                // Only process data if it exists
                if (data != null && data.size != 0) {
                    val secondsList = arrayListOf<String>()     // build list of chunks for seconds
                    saveToFile(data, secondsList, ctx.filesDir.toString(), file.nameWithoutExtension)    // save results from data to file
                }
            }
        }
    }

    /*
     * Run birdnet on found files and output to screen
     * Used for running on click
     */
    fun runBirdNet(progressBars: Array<ProgressBar>,
                   textViews: Array<TextView>,
                   audioName: TextView,
                   spinner: Spinner,
                   ctx: Context)
    {
        // Copy files from internal storage of audiomoth app - requires root
        try {
            val proc = Runtime.getRuntime().exec(
                arrayOf(
                    "su",
                    "-c",
                    "cp /data/user/0/org.nativescript.AudioMoth9/files/* /sdcard/Download"
                )
            )
            proc.waitFor()
        } catch (e: Exception) {
            Log.d("Exceptions", "Exception: $e")
        }
        // Get all audio files from Downloads folder
        val audioFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles {
                dir, filename ->
            File(dir, filename).isFile && (filename.endsWith(".aac") ||
                                           filename.endsWith(".wav") ||
                                           filename.endsWith(".mp4"))
        }
            ?: return // stop if no files found
        Log.d("Found files", "${audioFiles.size}")

        for (file in audioFiles) {
            // Only process files if they haven't been processed before, or have been updated
            if (!File(ctx.filesDir.toString(), "${file.nameWithoutExtension}-result.csv").exists()) {
                audioName.text = file.name
                // Classify birds from audio recording
                val data = myBird.runTest(file.absolutePath)
                // Only process data if it exists
                if (data != null && data.size != 0) {
                    val secondsList = arrayListOf<String>()     // build list of chunks for seconds
                    saveToFile(data, secondsList, ctx.filesDir.toString(), file.nameWithoutExtension)    // save results from data to file
                    updateScreen(data, progressBars, secondsList, textViews, ctx, spinner) // print results to phone screen
                }
            }
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