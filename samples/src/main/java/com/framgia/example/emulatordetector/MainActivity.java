package com.framgia.example.emulatordetector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.framgia.android.emulator.EmulatorDetector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EmulatorDetector emulatorDetector = EmulatorDetector.with(this);
        emulatorDetector.setCheckTelephony(true);
        emulatorDetector.setDebug(true);
        emulatorDetector.detect(new EmulatorDetector.OnEmulatorDetectorListener() {
            @Override
            public void onResult(boolean isEmulator) {
                Log.d(getClass().getName(), "Running on emulator --> " + isEmulator);
            }
        });
    }
}
