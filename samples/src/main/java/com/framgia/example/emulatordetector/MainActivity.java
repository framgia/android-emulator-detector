package com.framgia.example.emulatordetector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.framgia.android.emulator.*;
import com.framgia.android.emulator.BuildConfig;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textView = (TextView) findViewById(R.id.text);
        textView.setText("Checking....");

        EmulatorDetector.with(this)
                .setCheckTelephony(true)
                .addPackageName("com.bluestacks")
                .setDebug(true)
                .detect(new EmulatorDetector.OnEmulatorDetectorListener() {
                    @Override
                    public void onResult(final boolean isEmulator) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isEmulator) {
                                    textView.setText("This device is emulator"
                                        + getCheckInfo());
                                } else {
                                    textView.setText("This device is not emulator"
                                            + getCheckInfo());
                                }
                            }
                        });
                        Log.d(getClass().getName(), "Running on emulator --> " + isEmulator);
                    }
                });
    }

    private String getCheckInfo() {
        return "\nTelephony enable is "
            + EmulatorDetector.with(MainActivity.this).isCheckTelephony()
            + "\n\n\n" + EmulatorDetector.getDeviceInfo()
            + "\n\nEmulator Detector version " + BuildConfig.VERSION_NAME;
    }
}
