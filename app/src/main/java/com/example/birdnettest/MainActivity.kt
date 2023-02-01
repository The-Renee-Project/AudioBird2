package com.example.birdnettest

import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.birdnettest.ui.main.MainFragment
import kotlin.math.ceil


class MainActivity : AppCompatActivity() {
    private lateinit var myBird: BirdNet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        myBird = BirdNet(applicationContext)
    }

    fun runBirdNet(view: View){
        var data = myBird.runTest()

        if(data == null) {
            return;
        }

        // dropdown size is size of data
        var secondsList = arrayListOf<String>();

        for(i in 0..data.size step 3) {
            val start = i+1
            val end = i+3
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
//            Log.d("CONFIDENCE", element.first);
//            Log.d("BIRD", element.second.toString());
            textViews[i].text = element.first
            progressBars[i].progress = (ceil(element.second * 100)).toInt()
            textViews[i].visibility = View.VISIBLE
            progressBars[i].visibility = View.VISIBLE
        }
    }

    private fun getViews(): ArrayList<TextView> {
        var views = arrayListOf<TextView>()

        views.add(findViewById(R.id.confidenceOne));
        views.add(findViewById(R.id.confidenceTwo));
        views.add(findViewById(R.id.confidenceThree));
        views.add(findViewById(R.id.confidenceFour));
        views.add(findViewById(R.id.confidenceFive));

        return views;
    }

    private fun getBars(): ArrayList<ProgressBar> {
        var bars = arrayListOf<ProgressBar>()

        bars.add(findViewById(R.id.determinateBarOne));
        bars.add(findViewById(R.id.determinateBarTwo));
        bars.add(findViewById(R.id.determinateBarThree));
        bars.add(findViewById(R.id.determinateBarFour));
        bars.add(findViewById(R.id.determinateBarFive));

        return bars;
    }
}
