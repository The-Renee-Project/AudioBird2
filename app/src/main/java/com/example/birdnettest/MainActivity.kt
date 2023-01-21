package com.example.birdnettest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
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
        myBird = birdNet(findViewById(R.id.changeME), applicationContext)

    }

    fun runBirdNet(view: View){
        myBird.runTest()
    }
}