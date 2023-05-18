package com.example.birdnettest

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileWriter
import kotlin.math.ceil


class Util (appContext: Context) {
    private val myBird = BirdNet(appContext)
    private val ctx = appContext

    // Run birdnet on found files without outputting to screen
    fun runBirdNet(){
        // Reads/Writes audio file from Downloads folder
        val audioFileAccessor = AudioFileAccessor()
        val audioFiles = audioFileAccessor.getAudioFiles(ctx.contentResolver)

        val prefs = ctx.getSharedPreferences("last_timestamp", MODE_PRIVATE)
        val lastTimestamp = prefs.getString("timestamp", "")

        for (file in audioFiles) {
            if(lastTimestamp == null || lastTimestamp == "" || file.dateAdded > lastTimestamp) {
                // update shared preference to last file processed
                with (prefs.edit()) {
                    putString("timestamp", file.dateAdded)
                    apply() // asynchronous write to external memory
                }
            } else {
                continue // already processed file - skip
            }

            val data = myBird.runTest(file.data)
            if (data == null || data.size == 0) {
                return
            }
            val secondsList = arrayListOf<String>()     // build list of chunks for seconds
            saveToFile(data, secondsList, ctx.filesDir.toString(), file.data)    // save results from data to file
        }
    }

    // Run birdnet on found files and output to screen
    fun runBirdNet(progressBars: Array<ProgressBar>,
                   textViews: Array<TextView>,
                   audioName: TextView,
                   spinner: Spinner,
                   ctx: Context)
    {
        // Reads/Writes audio file from Downloads folder
        val audioFileAccessor = AudioFileAccessor()
        val audioFiles = audioFileAccessor.getAudioFiles(ctx.contentResolver)
        // shared preferences to make sure we only read new files
        val prefs = ctx.getSharedPreferences("last_timestamp", MODE_PRIVATE)
        var lastTimestamp = prefs.getString("timestamp", "")

        for (file in audioFiles) {
            if(lastTimestamp == null || lastTimestamp == "" || file.dateAdded > lastTimestamp) {
                // update shared preference to last file processed
                with (prefs.edit()) {
                    putString("timestamp", file.dateAdded)
                    apply() // asynchronous write to external memory
                    lastTimestamp = file.dateAdded
                }
            } else {
                continue // already processed file - skip
            }

            Log.d("FILE NAME:", file.data)
            val data = myBird.runTest(file.data)

            if (data == null || data.size == 0) {
                return
            }
            audioName.text = "File Name: ${file.title}" // print file title being processed
            val secondsList = arrayListOf<String>()     // build list of chunks for seconds
            saveToFile(data, secondsList, ctx.filesDir.toString(), file.title)    // save results from data to file
            updateScreen(data, progressBars, secondsList, textViews, ctx, spinner) // print results to phone screen
        }
    }

    // save results of birdnet to internal file
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

    // output results of birdnet to screen
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

    // update screen
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