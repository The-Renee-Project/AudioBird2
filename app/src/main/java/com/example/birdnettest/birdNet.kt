package com.example.birdnettest

// Android libraries
import android.content.Context // context to access screen elements
import android.widget.TextView // text view to update text

// Birdnet tflite model
import com.example.birdnettest.ml.BirdnetGlobal3kV22MdataModelFp16

// tensorflow libraries
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer // buffer to format data
import org.tensorflow.lite.DataType                          // tensorflow data types

// java imports
import java.nio.BufferUnderflowException // check for exceptions
import java.nio.ByteBuffer               // byte buffer to format input/output

class birdNet (view: TextView,
               ctx: Context) {
    private val pathToBirdCall = "MountainChickadee.mp3" // test file
    private val errorMsg       = "ERROR: %s\n"           // error message on failed model
    private val message        = "%.2f %s\n"             // message with model output prediction
    private var sampleRate     = 44100                   // standard sampling rate of audio files
    private val display        = view                    // text label to output to
    private val context        = ctx                     // context/app screen

    /**
     * Takes an audio file and returns a list of 3 second
     * audio byte chunks for input to birdnet
     */
    private fun getSamples(audioFile: String): ArrayList<FloatArray> {
        val birdcall = context.assets.openFd(audioFile) // relative path to assets folder
        // split into 3 second chunks - 3 * sample rate
        val audioStream = birdcall.createInputStream()   // Create File input stream from path
        val audioBytes  = ByteArray(sampleRate * 3) // 3 seconds worth of audio bytes
        val output: ArrayList<ByteArray> = ArrayList()  // all 3 second audio chunks

        // read in audio bytes in 3 second intervals until end of file
        while (audioStream.read(audioBytes) != -1) {
            output.add(audioBytes.clone()) // build ArrayList using deep copy
        }

        // convert to 32 bit floating point and return
        val fpOutput: ArrayList<FloatArray> = ArrayList() // float array output
        // convert every 3 second byte buffer to float array
        for (byteBuf in output) {
            /*
            // TODO -  buffer does not have exactly 4 bytes needed to make float
            while ((byteBuf.size % 4) != 0) {
                // add random data to force fit into float
            }
            */
            val floatChunk = FloatArray(byteBuf.size/4) // array of floats from bytes
            val buffer     = ByteBuffer.wrap(byteBuf)       // use ByteBuffer for conversion
            try {
                for (i in floatChunk.indices) {
                    floatChunk[i] = buffer.float // automatically convert to floats from buffer
                }
            } catch (e: BufferUnderflowException) {
                println(errorMsg.format(e.message))
            }
            fpOutput.add(floatChunk)
        }

        return fpOutput
    }

    /**
     * Run BirdNet tflite model on downloaded audio file sample
     */
    fun runTest() {
        try {
            val model = BirdnetGlobal3kV22MdataModelFp16.newInstance(context) //

            // Creates inputs for reference.
            // birdnet.tflite expects a 1D array of 3 32 bit floats as input
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 3), DataType.FLOAT32)
            val samples       = getSamples(pathToBirdCall) // get chunks of audio data input

            // dummy data to test model
            // TODO - figure out why birdnet tflite only accepts 3 floats as input
            var buf = ByteBuffer.allocateDirect(3*4)
            buf.putFloat(1.0f)
            buf.putFloat(1.0f)
            buf.putFloat(1.0f)
            inputFeature0.loadBuffer(buf)

            // Runs model inference and gets result.
            val outputs        = model.process(inputFeature0)         // run birdnet and get output
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer // format output to buffer
            var outputString   = ""                                   // output string of all results
            val outputAsFloat  = outputFeature0.floatArray            // get all results as float
            outputAsFloat.sortDescending()
            // build string m with 5 highest confidence matches
            // TODO - get corresponding label/bird species from assets/labels.txt
            for (i in 0..4) {
                outputString += message.format(outputAsFloat[i], "dummy")
            }
            println(outputString)       // print to debug console
            display.text = outputString // print result to screen TODO - make prettier

            // Releases model resources if no longer used.
            model.close()
        }
        catch (e: IllegalStateException) {
            display.text = errorMsg.format(e.message)
        }
    }

    /**
     * Activation function taken from birdnet-analyzer code
     */
    private fun sigmoid(prediction: Float): Float {
        var x = prediction
        // clip so data is between [-15, 15]
        if (x < -15.0f) {
            x = -15.0f
        }
        if (x > 15.0f) {
            x = 15.0f
        }

        return (1 / (1.0 + kotlin.math.exp(-1 * x))).toFloat()// taken from birdnet code
    }
}


/* Probably useless code
    /**
     * Start inference using interpreter with birdnet tflite mode
     */
    private fun startAudioInterpretation(samples: ArrayList<ByteArray>) {
        // input = array of 3 second audio chunks 32 bit fp
        var output = -1.23f    // output = prediction/classification confidence, should also be bird species
        val input = samples[0] // input audio buffer with floating point audio bytes
        println("Inference started")
        // run inference on input
        model.run(input, output)              // run birdnet with on input
        println("Inference completed")
        val result = sigmoid(output)          // run output through activation function
        display.text = message.format(result) // print result to screen
    }
            /*
            val inputStream: FileInputStream = FileInputStream(birdNet.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset: Long = birdNet.startOffset
            val declaredLength: Long = birdNet.length
            val direct = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)


            val inputStream = FileInputStream(birdNet.fileDescriptor)
            var buf = ByteArray(birdNet.length.toInt())
            println(buf.size)
            inputStream.read(buf)
            inputStream.close()

            var tempFile = File.createTempFile("tmp", "tflite") // create temp mp3 from bytes
            tempFile.deleteOnExit()                                     // free up file
            var fileOS = FileOutputStream(tempFile)                     // write audio byte chunks to file
            fileOS.write(buf)                                    // write first 3 sec
            fileOS.close()                                              // close output stream

            var bf = ByteBuffer.wrap(buf)
            var direct = ByteBuffer.allocateDirect(bf.remaining())
            if (bf.hasArray()) {
                direct.put(bf.array(), bf.arrayOffset(), bf.remaining())
            }
            else{
                println("No Array")
            }
            */
            // TEMP - check that samples are read properly in 3 sec bytes
            var tempFile = File.createTempFile("tmp", "mp3") // create temp mp3 from bytes
            tempFile.deleteOnExit()                                     // free up file
            var fileOS = FileOutputStream(tempFile)                     // write audio byte chunks to file
            fileOS.write(samples[0])                                    // write first 3 sec
            fileOS.close()                                              // close output stream
            // play file
            var mediaPlayer = MediaPlayer()        // media player to play sound
            //var fileIS = FileInputStream(pathToBirdCall)
            var fileIS = FileInputStream(tempFile) // read from temp file
            mediaPlayer.setDataSource(fileIS.fd)   // set temp file to play from
            //mediaPlayer.setDataSource(birdcall.fileDescriptor, birdcall.startOffset, birdcall.length)
            mediaPlayer.prepare()                  // start up audio/speaker
            mediaPlayer.start()                    // start playing sound
             */