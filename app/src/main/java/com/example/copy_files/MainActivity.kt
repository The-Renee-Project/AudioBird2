package com.example.copy_files

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun copyFile(view: View) {
        Log.d("Working", "WORKINGWAHUIEWHFU;H;KER")
        // Copies files from AudioMoth's storage to external storage in the Download folder
        try {
            val proc = Runtime.getRuntime().exec(
                arrayOf(
                    "su",
                    "-c",
                    "cp -r /data/user/0/org.nativescript.AudioMoth9/files /sdcard/Download/"
                )
            )
            proc.waitFor()
        } catch (e: Exception) {
            Log.d("Exceptions", "Exception: $e")
        }
    }
}