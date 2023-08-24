# AudioBird2
This app is a project developed for the UCSD ERSP program. It uses the BirdNET tflite model to classify bird calls on an Android smartphone.
Eventually this app is planned to be used alongside the AudioMoth app developed to reliably run periodic audio collection tasks.

## Code Description
This app was built in Android Studio, and uses the custom template. Most of the code can be found in the
`app/src/main/java/com/example/birdnettest/` folder. The `app/src/main/assets` folder has all the resource files used by the app, and
the `app/src/main/ml` folder has all the tflite models used by the app. The main UI components used on the screen can be found in `app/src/main/res/layout/activity_main.xml`

## How to run the application
1. Download and install [Android Studio](https://developer.android.com/studio)
2. Set up the [FFMPEG](https://github.com/arthenica/ffmpeg-kit/tree/main/android) library in the project's root directory
3. Use Android Studio to build and install the application on the device
    1. After installing the application:
        1. Accept requested permissions to ensure that the application runs as expected

## Navigating the application 
### Classify Audio 
Displays the name of the file currently being processed by BirdNET, as well as a progress bar to keep track of the number of audio files processed. 

### Status
Contains the status of both the logger worker and BirdNET worker (RUNNING, ENQUEUED, STOPPED, etc). The status also allows 
users to view when each of the workers were last run, as well as the number of audio files processed thus far. Users 
can also choose to restart the logger or BirdNET worker. 

