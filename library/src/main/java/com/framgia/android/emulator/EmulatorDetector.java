package com.framgia.android.emulator;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2016 Framgia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by pham.quy.hai on 5/16/16.
 */

public final class EmulatorDetector {

    public interface OnEmulatorDetectorListener {
        void onResult(boolean isEmulator);
    }

    private static final String[] KNOWN_NUMBERS = { // Default emulator phone numbers + VirusTotal
            "15555215554", "15555215556", "15555215558", "15555215560", "15555215562",
            "15555215564", "15555215566","15555215568", "15555215570", "15555215572",
            "15555215574", "15555215576", "15555215578", "15555215580", "15555215582",
            "15555215584"};
    private static final String[] KNOWN_DEVICE_IDS = {
            "000000000000000", // Default emulator id
            "e21833235b6eef10", // VirusTotal id
            "012345678912345"};

    private static final String[] KNOWN_IMSI_IDS = {"310260000000000"}; // Default imsi id

    private static String[] KNOWN_GENY_FILES = {"/dev/socket/genyd", "/dev/socket/baseband_genyd"};
    private static String[] KNOWN_QEMU_DRIVERS = {"goldfish"};

    private static Context mContext;
    private boolean isDebug = false;
    private boolean isTelephony = false;

    private List<String> mListPackageName = new ArrayList<>();

    public static EmulatorDetector with(Context pContext) {
        return new EmulatorDetector(pContext);
    }

    public EmulatorDetector(Context pContext) {
        this.mContext = pContext;
        mListPackageName.add("com.google.android.launcher.layouts.genymotion");
        mListPackageName.add("com.bluestacks");
        mListPackageName.add("com.bignox.app");
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean isCheckTelephony() {
        return isTelephony;
    }

    public void setCheckTelephony(boolean telephony) {
        isTelephony = telephony;
    }

    public void addPackageName(String pPackageName) {
        mListPackageName.add(pPackageName);
    }

    public void addPackageName(List<String> pListPackageName) {
        mListPackageName.addAll(pListPackageName);
    }

    public List<String> getPackageNameList() {
        return mListPackageName;
    }

    public void detect(final OnEmulatorDetectorListener pOnEmulatorDetectorListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isEmulator = detect();
                log("This System is Emulator: " + isEmulator);
                if (pOnEmulatorDetectorListener != null) {
                    pOnEmulatorDetectorListener.onResult(isEmulator);
                }
            }
        }).start();
    }

    public boolean detect() {
        boolean result = false;

        log(getDeviceInfo());

        // Check Basic
        if (!result) {
            //result = checkBasic();
            log(" Check basic " + result);
        }

        // Check Advanced
        if (!result) {
            result = checkAdvanced();
            log(" Check Advanced " + result);
        }

        // Check Package Name Trick
        if (!result) {
            result = checkPackageName();
            log(" Check Package Name Trick " + result);
        }

        return result;
    }

    private boolean checkBasic() {
        boolean result = Build.FINGERPRINT.startsWith("generic")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.toLowerCase().contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.equals("goldfish")
                || Build.HARDWARE.equals("vbox86")
                || Build.PRODUCT.equals("sdk")
                || Build.PRODUCT.equals("google_sdk")
                || Build.PRODUCT.equals("sdk_x86")
                || Build.PRODUCT.equals("vbox86p")
                || Build.BOARD.toLowerCase().contains("nox")
                || Build.BOOTLOADER.toLowerCase().contains("nox")
                || Build.HARDWARE.toLowerCase().contains("nox")
                || Build.PRODUCT.toLowerCase().contains("nox")
                || Build.SERIAL.toLowerCase().contains("nox");

        if (result) return true;
        result |= Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic");
        if (result) return true;
        result |= "google_sdk".equals(Build.PRODUCT);
        return result;
    }

    private boolean checkAdvanced() {
        boolean result = checkTelephony() || hasGenyFiles() || hasQEmuDrivers();
        return result;
    }

    private boolean checkPackageName() {
        final PackageManager packageManager = mContext.getPackageManager();
        List<ApplicationInfo> packages = packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);
        log("--- Package Installed ---");
        for (ApplicationInfo packageInfo : packages) {
            String packageName = packageInfo.packageName;
            boolean isEmulator = mListPackageName.contains(packageName);
            log("--> " + packageName + " --> " + isEmulator);
            if (isEmulator) {
                return true;
            }
        }
        return false;
    }

    private boolean checkTelephony() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED && this.isTelephony) {
            return hasKnownPhoneNumber()
                    || hasKnownDeviceId()
                    || hasKnownImsi()
                    || hasOperatorNameAndroid();
        }

        return false;
    }

    private boolean hasKnownPhoneNumber() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        String phoneNumber = telephonyManager.getLine1Number();

        for (String number : KNOWN_NUMBERS) {
            if (number.equalsIgnoreCase(phoneNumber)) {
                log(" hasKnownPhoneNumber is true");
                return true;
            }

        }
        return false;
    }

    private boolean hasKnownDeviceId() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        String deviceId = telephonyManager.getDeviceId();

        for (String known_deviceId : KNOWN_DEVICE_IDS) {
            if (known_deviceId.equalsIgnoreCase(deviceId)) {
                log(" hasKnownDeviceId is true");
                return true;
            }

        }
        return false;
    }

    private boolean hasKnownImsi() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = telephonyManager.getSubscriberId();

        for (String known_imsi : KNOWN_IMSI_IDS) {
            if (known_imsi.equalsIgnoreCase(imsi)) {
                log(" hasKnownImsi is true");
                return true;
            }
        }
        return false;
    }

    private boolean hasOperatorNameAndroid() {
        String operatorName = ((TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
        if (operatorName.equalsIgnoreCase("android")) {
            log(" hasKnownImsi is true");
            return true;
        }
        return false;
    }

    private boolean hasGenyFiles() {
        for (String file : KNOWN_GENY_FILES) {
            File geny_file = new File(file);
            if (geny_file.exists()) {
                log(" hasGenyFiles is true");
                return true;
            }
        }

        return false;
    }

    private boolean hasQEmuDrivers() {
        for (File drivers_file : new File[]{new File("/proc/tty/drivers"), new File("/proc/cpuinfo")}) {
            if (drivers_file.exists() && drivers_file.canRead()) {
                byte[] data = new byte[1024];
                try {
                    InputStream is = new FileInputStream(drivers_file);
                    is.read(data);
                    is.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                String driver_data = new String(data);
                for (String known_qemu_driver : KNOWN_QEMU_DRIVERS) {
                    if (driver_data.indexOf(known_qemu_driver) != -1) {
                        log(" hasQEmuDrivers is true");
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getDeviceInfo() {
        return "Build.PRODUCT: " + Build.PRODUCT + "\n" +
                "Build.MANUFACTURER: " + Build.MANUFACTURER + "\n" +
                "Build.BRAND: " + Build.BRAND + "\n" +
                "Build.DEVICE: " + Build.DEVICE + "\n" +
                "Build.MODEL: " + Build.MODEL + "\n" +
                "Build.HARDWARE: " + Build.HARDWARE + "\n" +
                "Build.FINGERPRINT: " + Build.FINGERPRINT;
    }


    private void log(String str) {
        if (this.isDebug) {
            Log.d(getClass().getName(), str);
        }
    }
}
