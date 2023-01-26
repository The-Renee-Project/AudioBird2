package com.example.birdnettest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import com.example.birdnettest.ui.main.MainFragment
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {
    private lateinit var myBird: birdNet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
        findViewById<TextView>(R.id.changeME).isElegantTextHeight = true
        findViewById<TextView>(R.id.changeME).inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        findViewById<TextView>(R.id.changeME).isSingleLine = false
        myBird = birdNet(findViewById(R.id.changeME), applicationContext)
    }

    fun runBirdNet(view: View){
        myBird.runTest()
    }
}
