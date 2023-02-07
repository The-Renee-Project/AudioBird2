package com.example.birdnettest

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

class AudioFileAccessor {
    // Container for information about each audio file
    data class AudioFile(
        val uri: Uri, // check if this is needed
        //val id: Long,
        val title: String,
        val data: String,
        val mimeType: String
    )
    /*
        make an example code that creates + saves file -> read it
     */

    fun getAudioFiles(contentResolver: ContentResolver): List<AudioFile> {

        val audioFiles = mutableListOf<AudioFile>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Select audio files with the mp4 extension (recorded files on AudioMoth end with .mp4)
        val selection = "${MediaStore.Audio.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("audio/mp4")

        val query = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )

        query?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (it.moveToNext()) {
                // Get values of column for a given audio file
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val data = it.getString(dataColumn)
                val mimeType = it.getString(mimeTypeColumn)

                val audioUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                audioFiles.add(AudioFile(audioUri, title, data, mimeType))
            }

            it.close()
        }

        return audioFiles
    }
}
