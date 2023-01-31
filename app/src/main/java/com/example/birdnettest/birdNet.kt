package com.example.birdnettest

// Android libraries
import android.content.Context
import android.widget.TextView

// java imports
import java.nio.ByteBuffer.allocateDirect
import java.io.FileOutputStream
import java.nio.ByteOrder
import java.io.File

// tensorflow libraries
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

// External Libraries
import com.example.birdnettest.ml.BirdnetGlobal3kV22ModelFp32
import com.jlibrosa.audio.JLibrosa

class BirdNet (view: TextView,
               ctx: Context) {
    private val pathToBirdCall = "Hawk.wav" // Audio file with bird call
    private val errorMsg       = "ERROR: %s\n\n" // Error message on failed model
    private val message        = "%s: %.5f\n"    // Message with model output prediction
    private var sampleRate     = 48000           // Standard sampling rate of audio files
    private val display        = view            // Text label to output to
    private val context        = ctx             // Context/app screen

    private lateinit var model: BirdnetGlobal3kV22ModelFp32 // BirdNet interpreter
    private lateinit var species: List<String>              // All species

    /**
     * Creates temporary file of audio data to access for librosa
     */
    private fun makeFile(audioFile: String): String {
        val birdcall = context.assets.open(audioFile) // Relative path to assets folder


        // giTODO - handle non wav files
        //      - handle files in external drive/SD card


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
     * Activation function taken from BirdNet-analyzer code
     */
    private fun sigmoid(prediction: Float): Float {
        return (1.0 / (1.0 + kotlin.math.exp(-1.5 * prediction.coerceIn(-15.0f, 15.0f)))).toFloat()
    }

    /**
     * Get string with 5 highest confidence species
     */
    private fun buildString(confidences: FloatArray): String {
        var outputString = ""
        val topFive = confidences.sortedArrayDescending().copyOfRange(0, 5) // Get 5 highest confidences

        // Build string with 5 highest confidences and corresponding species
        for (confidence in topFive) {
            val index = confidences.indexOfFirst{it == confidence}
            outputString += message.format(species[index], sigmoid(confidence))
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
    private fun runInterpreter(samples: ArrayList<FloatArray>) {
        var outputString = "" // String output to screen

        for (i in samples.indices) {
            outputString += "+++++++++ (%d to %d seconds) +++++++++\n".format(3*i, 3*i + 3)
            outputString += buildString(getConfidences(samples[i])) // Get top 5 outputs
            outputString += "++++++++++++++++++++++++++++++++\n\n"
        }

        display.text = outputString
    }

    /**
     * Takes an audio file and returns a list of 3 second
     * audio byte chunks for input to BirdNet
     */
    private fun getSamples(audioFile: String): ArrayList<FloatArray> {
        // Call JLibrosa to get audio data in floats - only supports wav files
        return segmentAudio(JLibrosa().loadAndRead(makeFile(audioFile), sampleRate,-1))
    }

    /**
     * Build array list of all species supported by BirdNet
     */
    private fun getSpecies() {
        species = context.assets.open("BirdNET_GLOBAL_3K_V2.2_Labels.txt").bufferedReader().readLines()
    }

    /**
     * Run BirdNet tflite model on downloaded audio file sample
     */
    fun runTest() {
        try {
            model = BirdnetGlobal3kV22ModelFp32.newInstance(context) // build interpreter
            getSpecies()
            // Read in 144,000 floats - 3 seconds of audio sampled at 48kHz
            // Creates tensor buffer for input for inference.
            runInterpreter(getSamples(pathToBirdCall))
        }
        catch (e: Exception) {
            display.text = errorMsg.format(e.message)
        }
        finally {
            // Releases model resources if no longer used.
            model.close()
        }
    }
}
