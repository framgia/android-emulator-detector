package com.framgia.example.emulatordetector;

import android.Manifest;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.framgia.android.emulator.BuildConfig;
import com.framgia.android.emulator.EmulatorDetector;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.text);
        textView.setText("Checking....");

        // Last check
        // BlueStacksPlayer
        // NoxPlayer
        // KoPlayer
        // MEmu

        MainActivityPermissionsDispatcher.checkEmulatorDetectorWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.READ_PHONE_STATE)
    void checkEmulatorDetector() {
        checkWith(true);
    }

    @OnShowRationale(Manifest.permission.READ_PHONE_STATE)
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
            .setMessage("Need READ_PHONE_STATE permission for check with Telephony function")
            .setPositiveButton("Allow", (dialog, button) -> request.proceed())
            .setNegativeButton("Deny", (dialog, button) -> request.cancel())
            .show();
    }

    private void checkWith(boolean telephony) {
        EmulatorDetector.with(this)
            .setCheckTelephony(telephony)
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

    @OnPermissionDenied(Manifest.permission.READ_PHONE_STATE)
    void showDeniedForCamera() {
        checkWith(false);
        Toast.makeText(this, "We check without Telephony function", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.READ_PHONE_STATE)
    void showNeverAskForCamera() {
        Toast.makeText(this, "Never check with Telephony function", Toast.LENGTH_SHORT).show();
    }

    private String getCheckInfo() {
        return "\nTelephony enable is "
            + EmulatorDetector.with(MainActivity.this).isCheckTelephony()
            + "\n\n\n" + EmulatorDetector.getDeviceInfo()
            + "\n\nEmulator Detector version " + BuildConfig.VERSION_NAME;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}
