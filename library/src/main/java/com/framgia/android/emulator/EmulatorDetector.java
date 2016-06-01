package com.framgia.android.emulator;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
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

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public final class EmulatorDetector {

    public interface OnEmulatorDetectorListener {
        void onResult(boolean isEmulator);
    }

    private static final String[] PHONE_NUMBERS = {
        "15555215554", "15555215556", "15555215558", "15555215560", "15555215562", "15555215564",
        "15555215566", "15555215568", "15555215570", "15555215572", "15555215574", "15555215576",
        "15555215578", "15555215580", "15555215582", "15555215584"
    };

    private static final String[] DEVICE_IDS = {
        "000000000000000",
        "e21833235b6eef10",
        "012345678912345"
    };

    private static final String[] IMSI_IDS = {
        "310260000000000"
    };

    private static final String[] GENY_FILES = {
        "/dev/socket/genyd",
        "/dev/socket/baseband_genyd"
    };

    private static final String[] QEMU_DRIVERS = {"goldfish"};

    private static final String[] PIPES = {
        "/dev/socket/qemud",
        "/dev/qemu_pipe"
    };

    private static final String[] X86_FILES = {
            "ueventd.android_x86.rc",
            "x86.prop"
    };

    private static final String[] ANDY_FILES = {
        "fstab.andy",
        "ueventd.andy.rc",
        "ueventd.goldfish.rc",
    };

    private static final Property[] PROPERTIES = {new Property("init.svc.qemud", null),
            new Property("init.svc.qemu-props", null), new Property("qemu.hw.mainkeys", null),
            new Property("qemu.sf.fake_camera", null), new Property("qemu.sf.lcd_density", null),
            new Property("ro.bootloader", "unknown"), new Property("ro.bootmode", "unknown"),
            new Property("ro.hardware", "goldfish"), new Property("ro.kernel.android.qemud", null),
            new Property("ro.kernel.qemu.gles", null), new Property("ro.kernel.qemu", "1"),
            new Property("ro.product.device", "generic"), new Property("ro.product.model", "sdk"),
            new Property("ro.product.name", "sdk"),
            new Property("ro.serialno", null)};

    private static final String IP = "10.0.2.15";

    private static final int MIN_PROPERTIES_THRESHOLD = 0x5;

    private static Context mContext;
    private boolean isDebug = false;
    private boolean isTelephony = false;
    private List<String> mListPackageName = new ArrayList<>();

    private static EmulatorDetector mEmulatorDetector;

    public static EmulatorDetector with(Context pContext) {
        if (mEmulatorDetector == null)
            mEmulatorDetector = new EmulatorDetector(pContext);
        return mEmulatorDetector;
    }

    public EmulatorDetector(Context pContext) {
        this.mContext = pContext;
        mListPackageName.add("com.google.android.launcher.layouts.genymotion");
        mListPackageName.add("com.bluestacks");
        mListPackageName.add("com.bignox.app");
    }

    public EmulatorDetector setDebug(boolean isDebug) {
        this.isDebug = isDebug;
        return this;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean isCheckTelephony() {
        return isTelephony;
    }

    public EmulatorDetector setCheckTelephony(boolean telephony) {
        this.isTelephony = telephony;
        return this;
    }

    public EmulatorDetector addPackageName(String pPackageName) {
        this.mListPackageName.add(pPackageName);
        return this;
    }

    public EmulatorDetector addPackageName(List<String> pListPackageName) {
        this.mListPackageName.addAll(pListPackageName);
        return this;
    }

    public List<String> getPackageNameList() {
        return this.mListPackageName;
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
            result = checkBasic();
            log("Check basic " + result);
        }

        // Check Advanced
        if (!result) {
            result = checkAdvanced();
            log("Check Advanced " + result);
        }

        // Check Package Name
        if (!result) {
            result = checkPackageName();
            log("Check Package Name " + result);
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
        boolean result = checkTelephony()
                || checkGenyFiles()
                || checkAndyFiles()
                || checkQEmuDrivers()
                || checkPipes()
                || checkIp()
                || (checkQEmuProps() && checkX86Files());
        return result;
    }

    private boolean checkPackageName() {
        final PackageManager packageManager = mContext.getPackageManager();
        List<ApplicationInfo> packages = packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            String packageName = packageInfo.packageName;
            boolean isEmulator = mListPackageName.contains(packageName);
            if (isEmulator) {
                log("Detected " + packageName);
                return true;
            }
        }
        return false;
    }

    private boolean checkTelephony() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED && this.isTelephony) {
            return checkPhoneNumber()
                    || checkDeviceId()
                    || checkImsi()
                    || checkOperatorNameAndroid();
        }

        return false;
    }

    private boolean checkPhoneNumber() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        String phoneNumber = telephonyManager.getLine1Number();

        for (String number : PHONE_NUMBERS) {
            if (number.equalsIgnoreCase(phoneNumber)) {
                log(" check phone number is detected");
                return true;
            }

        }
        return false;
    }

    private boolean checkDeviceId() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        String deviceId = telephonyManager.getDeviceId();

        for (String known_deviceId : DEVICE_IDS) {
            if (known_deviceId.equalsIgnoreCase(deviceId)) {
                log("Check device id is detected");
                return true;
            }

        }
        return false;
    }

    private boolean checkImsi() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = telephonyManager.getSubscriberId();

        for (String known_imsi : IMSI_IDS) {
            if (known_imsi.equalsIgnoreCase(imsi)) {
                log("Check imsi is detected");
                return true;
            }
        }
        return false;
    }

    private boolean checkOperatorNameAndroid() {
        String operatorName = ((TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
        if (operatorName.equalsIgnoreCase("android")) {
            log("Check operator name android is detected");
            return true;
        }
        return false;
    }

    private boolean checkGenyFiles() {
        for (String file : GENY_FILES) {
            File geny_file = new File(file);
            if (geny_file.exists()) {
                log("Check genymotion is detected");
                return true;
            }
        }

        return false;
    }

    private boolean checkQEmuDrivers() {
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
                for (String known_qemu_driver : QEMU_DRIVERS) {
                    if (driver_data.indexOf(known_qemu_driver) != -1) {
                        log("Check QEmuDrivers is detected");
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean checkPipes() {
        for (String pipe : PIPES) {
            File qemu_socket = new File(pipe);
            if (qemu_socket.exists()) {
                log("Check pipes is detected");
                return true;
            }
        }

        return false;
    }

    private boolean checkX86Files() {
        for (String pipe : X86_FILES) {
            File qemu_file = new File(pipe);
            if (qemu_file.exists()) {
                log("Check X86 system is detected");
                return true;
            }
        }

        return false;
    }

    private boolean checkAndyFiles() {
        for (String pipe : ANDY_FILES) {
            File qemu_file = new File(pipe);
            if (qemu_file.exists()) {
                log("Check Andy is detected");
                return true;
            }
        }

        return false;
    }

    private boolean checkQEmuProps() {
        int found_props = 0;

        for (Property property : PROPERTIES) {
            String property_value = getProp(mContext, property.name);
            if ((property.seek_value == null) && (property_value != null)) {
                found_props++;
            }
            if ((property.seek_value != null)
                    && (property_value.indexOf(property.seek_value) != -1)) {
                found_props++;
            }

        }

        if (found_props >= MIN_PROPERTIES_THRESHOLD) {
            log("Check QEmuProps is detected");
            return true;
        }

        return false;
    }

    private boolean checkIp() {
        boolean ipDetected = false;
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.INTERNET)
            == PackageManager.PERMISSION_GRANTED) {
            String[] args = { "/system/bin/netcfg" };
            try {
                ProcessBuilder builder = new ProcessBuilder(args);
                builder.directory(new File("/system/bin/"));
                builder.redirectErrorStream(true);
                Process process = builder.start();
                InputStream in = process.getInputStream();
                byte[] re = new byte[1024];
                while (in.read(re) != -1) {
                    String lineData = new String(re);
                    log("Check ip -> " + lineData);
                    if (!TextUtils.isEmpty(lineData) && lineData.contains(IP)) {
                        ipDetected = true;
                        log("Check ip is detected");
                        break;
                    }
                }
                in.close();

            } catch (Exception ex) {
                // empty catch
            }
        }
        return ipDetected;
    }

    private String getProp(Context context, String property) {
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class<?> systemProperties = classLoader.loadClass("android.os.SystemProperties");

            Method get = systemProperties.getMethod("get", String.class);

            Object[] params = new Object[1];
            params[0] = new String(property);

            return (String) get.invoke(systemProperties, params);
        } catch (IllegalArgumentException iAE) {
            // empty catch
        } catch (Exception exception) {
            // empty catch
        }
        return null;
    }

    private void log(String str) {
        if (this.isDebug) {
            Log.d(getClass().getName(), str);
        }
    }

    public static String getDeviceInfo() {
        return "Build.PRODUCT: " + Build.PRODUCT + "\n" +
            "Build.MANUFACTURER: " + Build.MANUFACTURER + "\n" +
            "Build.BRAND: " + Build.BRAND + "\n" +
            "Build.DEVICE: " + Build.DEVICE + "\n" +
            "Build.MODEL: " + Build.MODEL + "\n" +
            "Build.HARDWARE: " + Build.HARDWARE + "\n" +
            "Build.FINGERPRINT: " + Build.FINGERPRINT;
    }
}
