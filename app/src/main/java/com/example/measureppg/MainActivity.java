package com.example.measureppg;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.sql.Timestamp;


import androidx.annotation.NonNull;

public class MainActivity extends WearableActivity implements SensorEventListener
{
    private TextView mTextView;
    private static final String TAG = "MeasurePPG";
    private String mOpenFileID;
    private String fileName;
    private StringBuilder sensorData;

    public static final int REQUEST_CODE_SIGN_IN = 1;
    public static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private SensorManager mSensorManager;
    private DriveServiceHelper mDriveServiceHelper;

    private Sensor mHeartrateSensor;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mStepDetector;
    private Sensor mHeartBeatSensor;

    private Button startButton;
    private Button stopButton;
    private Button signInButton;
    private Button configButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        signInButton = findViewById(R.id.signInButton);
        configButton = findViewById(R.id.configButton);

        sensorData = new StringBuilder();
        //handles sign in (prompt)
        checkAlreadySignedIn();
        signIn();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        logAvailableSensors();

        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startMeasurement();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                stopMeasurement();
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                signIn();
            }
        });

        configButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // query();
                Intent intent = new Intent(MainActivity.this,  ConfigActivity.class);
                startActivity(intent);
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    protected void onResume()
    {
        super.onResume();
    }

    // Prints list of device's available sensors to the log.
    private void logAvailableSensors()
    {
        final List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
        Log.d(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", "SensorName", "StringType", "Type"));
        for (Sensor sensor : sensors) {
            Log.v(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", sensor.getName(), sensor.getStringType(), sensor.getType()));
        }

        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    // Registers listeners for device's sensors.
    protected void startMeasurement()
    {
        int numSensors = 0; // Number of sensors turned on
        if (ConfigActivity.ACC)
        {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            numSensors++;
        }
        if (ConfigActivity.HR)
        {
            mHeartrateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            numSensors++;
        }

        // mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        // mHeartBeatSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);

        if (numSensors <= 0)
        {
            Toast.makeText(this, "Error: No sensors selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        createFile();
        if (mSensorManager != null)
        {
            Toast.makeText(this, "Starting measurement.", Toast.LENGTH_SHORT).show();

            if (mHeartrateSensor != null)
                mSensorManager.registerListener(this, mHeartrateSensor, 1000000); // Sampling period is in microseconds
            else
                Log.d(TAG, "No heart rate sensor.");

            if (mAccelerometer != null)
                mSensorManager.registerListener(this, mAccelerometer, 1000000);
            else
                Log.d(TAG, "No accelerometer.");

            if (mGyroscope != null)
                mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            else
                Log.d(TAG, "No gyroscope.");

            if (mStepDetector != null)
                mSensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_NORMAL);
            else
                Log.d(TAG, "No step detector.");

            if (mHeartBeatSensor != null)
                mSensorManager.registerListener(this, mHeartBeatSensor, SensorManager.SENSOR_DELAY_NORMAL);
            else
                Log.d(TAG, "No heart beat sensor.");
        }
    }

    // Unregisters sensor listener.
    private void stopMeasurement()
    {
        saveFile();
        if (mSensorManager != null)
        {
            mSensorManager.unregisterListener(this);
            Toast.makeText(this, "Stopping measurement.", Toast.LENGTH_SHORT).show();
        }
    }

    // Appends sensor data on sensor change event to sensorData string builder.
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        Log.d(TAG, "" + event.sensor.getType() + " / " + event.accuracy + " / " + event.timestamp + " / " + event.values[0]);
        mTextView.setText("" + event.values[0]);

        String timeStamp = getTimeStamp();
        if (event.sensor.getType() == 1) // Accelerometer is type 1 and has 3 values
        {
            sensorData.append(timeStamp + "\t");
            sensorData.append(event.values[0] + "\t");
            sensorData.append(event.values[1] + "\t");
            sensorData.append(event.values[2] + "\n");
        }
        else // Other sensor type only have 1 value
        {
            sensorData.append(timeStamp + "\t");
            sensorData.append(event.values[0] + "\n");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // TO-DO
    }

    // Checks for a previously signed in Google account on the device.
    private void checkAlreadySignedIn()
    {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    // Passes intents to relevant methods, such as handleSignIn().
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "Activity request code: " + requestCode);
        if (requestCode == REQUEST_CODE_SIGN_IN)
        {
            if (resultCode == Activity.RESULT_OK && data != null)
                handleSignIn(data);
        }
        else if (requestCode == REQUEST_CODE_OPEN_DOCUMENT)
        {
            if (resultCode == Activity.RESULT_OK && data != null)
            {
                Uri uri = data.getData();
                if (uri != null)
                {
                    openFileFromFilePicker(uri);
                }
            }
        }
    }

    // Updates UI after sign-in and/or sign-out.
    private void updateUI(GoogleSignInAccount account)
    {
        if (account != null)
        {
            Toast.makeText(this, "Sign-in successful.", Toast.LENGTH_SHORT).show();
            signInButton.setVisibility(View.GONE);
        }
        else
        {
            signInButton.setVisibility(View.VISIBLE);
        }
    }

    // Begins sign-in process and gets sign-in intent.
    private void signIn()
    {
        Log.d(TAG, "Requesting sign-in.");

        GoogleSignInOptions signInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE))
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    // Handles sign-in intent.
    private void handleSignIn(Intent result)
    {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>()
                {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleAccount)
                    {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                        GoogleAccountCredential credential = GoogleAccountCredential
                                .usingOAuth2(MainActivity.this, Collections.singleton(DriveScopes.DRIVE));
                        credential.setSelectedAccount(googleAccount.getAccount());
                        Drive googleDriveService = new Drive
                                .Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                                .setApplicationName(TAG)
                                .build();
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                        mDriveServiceHelper.checkFolder(); // Creates folder for app in user's Google Drive
                        updateUI(googleAccount);
                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception exception)
                    {
                        Log.e(TAG, "Unable to sign in.", exception);
                    }
                });
    }

    // Lists files in Google Drive.
    private void query()
    {
        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Querying for files.");

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>()
                    {
                        @Override
                        public void onSuccess(FileList fileList)
                        {
                            StringBuilder builder = new StringBuilder();
                            List<com.google.api.services.drive.model.File> files = fileList.getFiles();
                            Log.d(TAG, "Files: ");
                            String fN; // File name
                            String fID; // File ID
                            for (com.google.api.services.drive.model.File file : files)
                            {
                                builder.append(file.getName()).append("\n");
                                fN = file.getName();
                                fID = file.getId();
                                Log.d(TAG, fN + " | " + fID);
                            }
                            String fileNames = builder.toString();

                            //mFileTitleEditText.setText("File List");
                            //mDocContentEditText.setText(fileNames);

                            //setReadOnlyMode();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            Log.e(TAG, "Unable to query files.", exception);
                        }
                    });
        }
    }

    // Opens Storage Access Framework file picker.
    private void openFilePicker()
    {
        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    // Opens a file from its {@code uri} returned from the Storage Access Framework file picker.
    private void openFileFromFilePicker(Uri uri)
    {
        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>()
                    {
                        @Override
                        public void onSuccess(Pair<String, String> nameAndContent)
                        {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            // mFileTitleEditText.setText(name);
                            // mDocContentEditText.setText(content);

                            // Files opened through SAF cannot be modified.
                            // setReadOnlyMode();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            Log.e(TAG, "Unable to open file from picker.", exception);
                        }
                    });
        }
    }

    // Creates a new file via the Drive REST API.
    private void createFile()
    {
        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Creating a file.");

            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(new OnSuccessListener<String>()
                    {
                        @Override
                        public void onSuccess(String fileId)
                        {
                            readFile(fileId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            Log.e(TAG, "Couldn't create file.", exception);
                        }
                    });
        }
    }

    // Retrieves the title and content of a file identified by {@code fileID} and populates the UI.
    private void readFile(final String fileID)
    {
        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Reading file " + fileID);

            mDriveServiceHelper.readFile(fileID)
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>()
                    {
                        @Override
                        public void onSuccess(Pair<String, String> nameAndContent)
                        {
                            // String name = nameAndContent.first;
                            // String content = nameAndContent.second;

                            // mFileTitleEditText.setText(name);
                            // mDocContentEditText.setText(content);

                            mOpenFileID = fileID;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            Log.e(TAG, "Couldn't read file.", exception);
                        }
                    });
        }
    }

    // Saves currently opened file created by {@link #createFile()} if one exists.
    private void saveFile()
    {
        if (mDriveServiceHelper != null && mOpenFileID != null)
        {
            Log.d(TAG, "Saving " + mOpenFileID);

            if(ConfigActivity.ACC)
                fileName = getTimeStamp() + "Accelerometer.txt" ;
            else if(ConfigActivity.HR)
                fileName = getTimeStamp() + "Heart Rate.txt" ;
            else
                fileName = getTimeStamp() + "Other.txt";

            String fileContent = sensorData.toString();

            mDriveServiceHelper.saveFile(mOpenFileID, fileName, fileContent)
                    .addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception exception)
                        {
                            Log.e(TAG, "Unable to save file via REST.", exception);
                        }
                    });
        }
    }

    // Returns current timestamp as a string.
    public static String getTimeStamp()
    {
        Date date = new Date();

        long time = date.getTime();

        Timestamp ts = new Timestamp(time);
        return ts.toString();
    }



}
