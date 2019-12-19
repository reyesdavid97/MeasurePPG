Installation

Version 1.0 of the app is installed onto Android smartwatches using Android Studio debugging. In order for the app to work correctly, the OAuth client must be configured for your project. This can be done in the following way:

- Open the app’s project in Android Studio and open the Gradle pane on the right side of the screen. Navigate through the drop down menu as follows MeasurePPG>app>tasks>android>signingReport. Double click on signingReport and record the SHA1 signing certificate it provides. 
- Go to the following website: https://developers.google.com/identity/sign-in/android/start-integrating
- Scroll down and click the “Configure a project” button.
- Sign in with a Google account.
- Select or create a project for Google Sign-in.
- Select Android from the drop-down menu.
- Enter “com.example.measureppg” for the package name.
- Enter SHA1 signing certificate that you obtained from the signingReport earlier.
- Press create.

Video: https://www.youtube.com/watch?v=u_ameCYORHY
