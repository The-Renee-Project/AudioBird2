package com.example.birdnettest

// Android libraries
import android.content.Context
import android.util.Log

// java imports
import java.nio.ByteBuffer.allocateDirect
import java.io.FileOutputStream
import java.nio.ByteOrder
import java.io.File

// tensorflow libraries
import com.example.birdnettest.ml.BirdnetGlobal3kV22ModelFp32
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

// External Libraries
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.FFmpegKit
import com.jlibrosa.audio.JLibrosa

class BirdNet (ctx: Context) {
    private var sampleRate     = 48000           // Standard sampling rate of audio files
    private val context        = ctx             // Context/app screen

    private lateinit var model: BirdnetGlobal3kV22ModelFp32 // BirdNet interpreter
    lateinit var species: List<String>                      // All species

    /**
     * Creates temporary file of audio data to access file in assets folder
     */
    private fun makeFile(audioFile: String): String {
        val birdcall = context.assets.open(audioFile) // Relative path to assets folder

        // Read data into temp file to pass to librosa
        val tempFile = File.createTempFile("tmp", ".wav") // Create temp wav from bytes
        tempFile.deleteOnExit()                                      // Free up file
        val fileOS = FileOutputStream(tempFile)                      // Write audio byte chunks to file
        var byte: Int // Read in single bytes to make sure we get whole file
        while (true) {
            byte = birdcall.read() // Read in a single byte
            // Stop when end of file
            if (byte == -1) {break}
            fileOS.write(byte)
        }
        birdcall.close() // Close reference to file
        fileOS.close()   // Close output stream

        return tempFile.absolutePath
    }

    /**
     * Activation function taken from BirdNet-analyzer code
     */
    private fun sigmoid(prediction: Float): Float {
        return (1.0 / (1.0 + kotlin.math.exp(-1.5 * prediction.coerceIn(-15.0f, 15.0f)))).toFloat()
    }

    /**
     * Get string with 5 highest confidence species
     */
    private fun generateDataPair(confidences: FloatArray): ArrayList<Pair<String,Float>> {
        val outputString = arrayListOf<Pair<String,Float>>()
        val topFive = confidences.sortedArrayDescending().copyOfRange(0, 5) // Get 5 highest confidences

        // Build string with 5 highest confidences and corresponding species
        for (confidence in topFive) {
            val index = confidences.indexOfFirst{it == confidence}
            Log.d("BIRD", species[index])
            Log.d("CONFIDENCE", sigmoid(confidence).toString())
            outputString.add(Pair(species[index], sigmoid(confidence)))
        }

        return outputString
    }

    /**
     * Run BirdNet on audio samples and return array of confidences
     */
    private fun getConfidences(samples: FloatArray): FloatArray {
        // Fill with values from audio file
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 144000), DataType.FLOAT32)
        val byteBuffer = allocateDirect(144000*4) // 144000 4 byte floats
        byteBuffer.order(ByteOrder.nativeOrder())        // Format byte order

        for (float in samples) {
            byteBuffer.putFloat(float)
        }

        inputFeature0.loadBuffer(byteBuffer) // initialize buffer

        // Runs inference using model and gets output
        return model.process(inputFeature0).outputFeature0AsTensorBuffer.floatArray
    }

    /**
     * Calls BirdNet on given samples of audio and prints top confidences to screen
     */
    private fun runInterpreter(samples: ArrayList<FloatArray>) : ArrayList<ArrayList<Pair<String,Float>>> {
        val result = arrayListOf<ArrayList<Pair<String,Float>>>()

        for (i in samples.indices) {
            result.add(generateDataPair(getConfidences(samples[i]))) // Get top 5 outputs
        }

        return result
    }

    /**
     * Segment audio into 3 second chunks, at sample rate 48kHz
     */
    private fun segmentAudio(audioData: FloatArray): ArrayList<FloatArray> {
        val chunks = FloatArray(144000)  // 3 second chunks
        val output = ArrayList<FloatArray>() // Arraylist of all chunks
        // Read in chunks from audio file
        for (i in audioData.indices step 144000) {
            // Truncate any data that is not 3 seconds worth
            if ((audioData.size - i) < 144000) {break}
            audioData.copyInto(chunks, 0, i, i + 144000)
            output.add(chunks.clone())
        }
        return output
    }

    /**
     * Converts passed file to wav and stores it in the same directory
     */
    private fun toWav(audioFile: String): String {
        if(audioFile.substring(audioFile.lastIndexOf(".")) == ".wav") {
            return audioFile
        }
        val outFile = audioFile.substring(0, audioFile.lastIndexOf(".")) + ".wav"
        val session = FFmpegKit.execute("-y -i %s -ac %d -f wav %s".format(audioFile, 2, outFile))

        if (ReturnCode.isSuccess(session.returnCode)) {
            return outFile
        }
        else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            Log.d("CANCELED", "Command failed to finish executing because it was canceled")
        }
        else {
            // FAILURE
            Log.d("FAILURE", "Command failed with state ${session.state} and rc ${session.returnCode}.${session.failStackTrace}");
        }
        return "KABLOOM!"
    }

    /**
     * Takes an audio file and returns a list of 3 second
     * audio byte chunks for input to BirdNet
     */
    private fun getSamples(audioFile: String): ArrayList<FloatArray> {
        // Call JLibrosa to get audio data in floats
        return segmentAudio(JLibrosa().loadAndRead(toWav(audioFile), sampleRate,-1))
    }

    /**
     * Build array list of all species supported by BirdNet
     */
    fun getSpecies() {
        species = context.assets.open("BirdNET_GLOBAL_3K_V2.2_Labels.txt").bufferedReader().readLines()
    }

    /**
     * Run BirdNet tflite model on downloaded audio file sample
     */
    fun runTest(pathToBirdCall: String) : ArrayList<ArrayList<Pair<String,Float>>>? {
        var data: ArrayList<ArrayList<Pair<String,Float>>>? = null

        try {
            model = BirdnetGlobal3kV22ModelFp32.newInstance(context) // build interpreter
            getSpecies() // read list of 3337 species

            // Read in 144,000 floats - 3 seconds of audio sampled at 48kHz
            // Creates tensor buffer for input for inference.
            data = runInterpreter(getSamples(pathToBirdCall))
        }
        catch (e: Exception) {
            Log.d("ERROR:", e.message.toString())
        }
        finally {
            // Releases model resources if no longer used.
            model.close()

            return data
        }
    }
}
