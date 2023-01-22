# AudioBird2
This app is a project developed for the UCSD ERSP program. It uses the birdNET tflite model to classify bird calls on an Android smartphone. 
Eventually this app is planned to be used alongside the AudioMoth app developed to reliably run periodic audio collection tasks.

## Code Description
This app was built in Android Studio, and uses the custom template. Most of the code can be found in the 
`app/src/main/java/com/example/birdnettest/` folder. The `app/src/main/assets` folder has all the resource files used by the app, and
the `app/src/main/ml` folder has all the tflite models used by the app. 
The main UI elements used on the screen can be found in `app/src/main/res/layout/activity_main.xml`
