package com.example.birdnettest

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File


// Resource used: https://developer.android.com/training/data-storage/shared/media.
class AudioFileAccessor {

    // Container for information about each audio file.
    data class AudioFile(
        val uri: Uri,
        val title: String,
        val data: String,
        val mimeType: String,
        val dateAdded: String
    )

    fun getAudioFiles(contentResolver: ContentResolver): List<AudioFile> {
        // Copies files from AudioMoth's storage to external storage in the Download folder
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

        val audioFiles = mutableListOf<AudioFile>()

        var collection =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else{
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        var projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // Select audio files with the mp4 extension
//        val selection = "${MediaStore.Audio.MediaStore.Audio.Media.MIME_TYPE} = ?"
//        val selectionArgs =
//            arrayOf("audio/mp4") // MIME type for mp4 files (https://www.mpi.nl/corpus/html/lamus2/apa.html)

        var query = contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        )

        // Add error handling in case the query is null.
        if (query == null) {
            Log.e("AudioFileAccessor", "Query returned null.")
            return audioFiles
        }

        query.use {
            // Cache column indices
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (it.moveToNext()) {
                // Get values of column for a given audio file
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val data = it.getString(dataColumn)
                val mimeType = it.getString(mimeTypeColumn)
                val dateAdded = it.getString(dateAddedColumn)

                Log.d("FILE NAME", title)

                val audioUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                // Store column values and contentUri in a local object representing the audio file.
                audioFiles.add(AudioFile(audioUri, title, data, mimeType, dateAdded))
            }

            it.close()
        }

        // For android 10 and under also query videos for mp4 files
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            collection =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else{
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Video.Media.MIME_TYPE} = ?"
            val selectionArgs =
                arrayOf("video/mp4") // MIME type for mp4 files (https://www.mpi.nl/corpus/html/lamus2/apa.html)

            query = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )

            // Add error handling in case the query is null.
            if (query == null) {
                Log.e("AudioFileAccessor", "Query returned null.")
                return audioFiles
            }

            query.use {
                // Cache column indices
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (it.moveToNext()) {
                    // Get values of column for a given audio file
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val data = it.getString(dataColumn)
                    val mimeType = it.getString(mimeTypeColumn)
                    val dateAdded = it.getString(dateAddedColumn)

                    Log.d("FILE NAME", title)

                    val audioUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )

                    // Store column values and contentUri in a local object representing the audio file.
                    audioFiles.add(AudioFile(audioUri, title, data, mimeType, dateAdded))
                }

                it.close()
            }
        }

        Log.d("AudioFileAccessor", "Number of audio files after loop: ${audioFiles.size}")
        return audioFiles
    }
}