Step 1: Create a Firebase Project
Go to Firebase Console.
Click on "Add project".
Enter your project name and click "Continue".
Click "Create project" and wait for it to initialize.

Step 2: Add an Android App
Inside Firebase Console, click on "Add App" and choose Android.
Enter the package name (must match your app’s package name in AndroidManifest.xml).
Click "Register app".
Step 3: Download google-services.json
After registering the app, Firebase provides a google-services.json file.
Click "Download google-services.json".
Place this file inside your Android project at:
just copy the json file and paste on /App which is on Andriod studio

Step 4: Add Firebase SDK to Your App
Open Android Studio.
Click on Tools → Firebase.
In the Assistant window (on the right side), expand Realtime Database (or the service you want to use).
Click "Connect to Firebase" and sign in with your Google account.
Click "Add the necessary dependencies" to automatically add Firebase SDK dependencies.
Sync the project.

Step 5: Enable Firebase Realtime Database
In the Firebase Console, go to Build > Realtime Database.
Click Create Real Time Database and choose a region.
Set rules to allow read/write:
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
Click Publish.

Step 6:
Go to Firebase Console.
Select your project.
Navigate to Build → Authentication.
Click Sign-in method.
Enable Email/Password authentication.
Click Save.

Step 7: Add Firebase authentication SDK on your andriod studio
Open Android Studio and load your project.
Go to Tools → Firebase.
In the Firebase Assistant (right-side panel), expand Authentication.
Click Email and password authentication.
Click "Connect to Firebase" → Sign in with Google → Select your project.
Click "Add Firebase Authentication to your app", then Accept changes.

Then you can the app..!
