package com.framgia.example.emulatordetector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.framgia.android.emulator.EmulatorDetector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textView = (TextView) findViewById(R.id.text);
        textView.setText("Checking....");

        EmulatorDetector.with(this)
                .setCheckTelephony(true)
                .setDebug(true)
                .detect(new EmulatorDetector.OnEmulatorDetectorListener() {
                    @Override
                    public void onResult(boolean isEmulator) {
                        if (isEmulator) {
                            textView.setText("This device is emulator"
                                    + "\nTelephony enable is "
                                    + EmulatorDetector.with(MainActivity.this).isCheckTelephony()
                                    + "\n\n\n" + EmulatorDetector.getDeviceInfo());
                        } else {
                            textView.setText("This device is not emulator"
                                    + "\nTelephony enable is "
                                    + EmulatorDetector.with(MainActivity.this).isCheckTelephony()
                                    + "\n\n\n" + EmulatorDetector.getDeviceInfo());
                        }
                        Log.d(getClass().getName(), "Running on emulator --> " + isEmulator);
                    }
                });
    }
}
