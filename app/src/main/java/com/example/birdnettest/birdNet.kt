package com.example.birdnettest

// Android libraries
import android.content.Context
import android.widget.TextView
import com.example.birdnettest.ml.BirdnetGlobal3kV22MdataModelFp16
import org.tensorflow.lite.DataType

// tensorflow libraries
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

// java imports
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocateDirect
import java.nio.ByteOrder
import java.nio.FloatBuffer

class birdNet (view: TextView,
               ctx: Context) {
    private val pathToBirdNet  = "BirdNET_GLOBAL_3K_V2.2_Model_FP32.tflite" // birdnet tflite model
    private val pathToBirdCall = "MountainChickadee.mp3" // test file
    private val errorMsg       = "ERROR: %s\n"           // error message on failed model
    private val message        = "%.2f %s\n"             // message with model output prediction
    private var sampleRate     = 48000                   // standard sampling rate of audio files
    private val display        = view                    // text label to output to
    private val context        = ctx                     // context/app screen

    private lateinit var model: Interpreter              // interpreter

    init {
        buildInterpreter()
    }

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

        // read in audio bytes in 3 second intervals until end of file, truncate any extra
        while (audioStream.read(audioBytes) != -1) {
            output.add(audioBytes.clone()) // build ArrayList using deep copy
        }

        // convert to 32 bit floating point and return
        val fpOutput: ArrayList<FloatArray> = ArrayList() // float array output
        // convert every 3 second byte buffer to float array
        for (byteBuf in output) {
            val floatChunk = FloatArray(byteBuf.size) // array of floats from bytes
            for (i in floatChunk.indices) {
                floatChunk[i] = byteBuf[i].toFloat() // convert byte to float
            }
            fpOutput.add(floatChunk)
        }

        return fpOutput
    }

    /**
     * Build interpreter
     */
    private fun buildInterpreter() {
        val inputStream = context.assets.open(pathToBirdNet) // get birdnet tflite
        val buffer      = ByteArray(3000 )              // read in 3 KB chunks, arbitrary
        var bytesRead   = inputStream.read(buffer)          // Check how many bytes read
        val output      = ByteArrayOutputStream()           // Read tflite raw bytes
        // keep reading until all bytes read
        while (bytesRead != -1) {
            output.write(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
        }
        val rawBirdNet  = output.toByteArray() // Get raw byte array
        val directBytes = ByteBuffer.allocateDirect(rawBirdNet.size) // ensure correct byte order
        directBytes.order(ByteOrder.nativeOrder()) // store bytes in native order of phone
        directBytes.put(rawBirdNet)                // write bytes
        model = Interpreter(directBytes)           // build interpreter to model
    }

    /**
     * Run BirdNet tflite model on downloaded audio file sample
     */
    fun runTest() {
        // TODO - check for uninitialized model
//        try {
            // read in 144,000 floats - 3 seconds of audio sampled at 48kHz
            val samples    = getSamples(pathToBirdCall) // get chunks of audio data input
            //val inputBuff  = samples[0]                 // test on first sample
        val inputBuff = FloatArray(144000)
        for (i in inputBuff.indices) {
            inputBuff[i] = 0.001f
        }
            var outputBuff = FloatArray(3337)
//            var outputBuff = mutableMapOf<Integer, Float>()       // output confidences for all 3337
//            var elements = FloatBuffer.allocate(3337)
            // TODO - format input/output

            model.run(inputBuff, outputBuff)
//            model.runForMultipleInputsOutputs(arrayOf(inputBuff), outputBuff) // run birdnet and get output
            var outputString   = ""          // output string of all results

//            outputBuff.sortDescending() // get highest confidence values
            // TODO - get corresponding label/bird species from assets/labels.txt
            for (i in 0..4) {
                outputString += message.format(outputBuff[i], "dummy")
            }
            println(outputString)       // print to debug console
            display.text = outputString // print result to screen TODO - make prettier

            // Releases model resources if no longer used.
            model.close()
//        }
//        catch (e: Exception) {
//            println(errorMsg.format(e.message))
//        }
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
/*
            val model = BirdnetGlobal3kV22MdataModelFp16.newInstance(context)
            // Only accepts 3 because is not actual classifier - used to filter results from
            // actual birdnet using latitude, longitude and week of year
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 3), DataType.FLOAT32)
            var buff = ByteBuffer.allocateDirect(3*4)
            buff.putFloat(1.0f)
            buff.putFloat(1.0f)
            buff.putFloat(1.0f)
            inputFeature0.loadBuffer(buff)

            // Runs model inference and gets result.
            val outputs        = model.process(inputFeature0)         // run birdnet and get output
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer // format output to buffer
            var outputString   = ""                                   // output string of all results
            val outputAsFloat  = outputFeature0.floatArray            // get all results as float
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