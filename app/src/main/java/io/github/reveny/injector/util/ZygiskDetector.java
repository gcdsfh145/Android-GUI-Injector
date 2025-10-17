package io.github.reveny.injector.core.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

public class ZygiskDetector {
    private static final String TAG = "ZygiskDetector";
    
    // Zygisk相关的包名和特征
    private static final String[] ZYGISK_PACKAGE_NAMES = {
        "com.topjohnwu.magisk",           // 官方Magisk
        "io.github.vvb2060.magisk",       // Alpha Magisk
        "io.github.huskydg.magisk"        // Delta Magisk
    };
    
    // Zygisk相关的库文件路径
    private static final String[] ZYGISK_LIB_PATHS = {
        "/system/lib64/libzygisk.so",
        "/system/lib/libzygisk.so",
        "/apex/com.android.art/lib64/libzygisk.so",
        "/apex/com.android.art/lib/libzygisk.so"
    };
    
    // Zygisk相关的二进制文件
    private static final String[] ZYGISK_BINARIES = {
        "/system/bin/zygisk",
        "/system/xbin/zygisk",
        "/data/adb/zygisk"
    };
    
    /**
     * 检测Zygisk是否安装
     */
    public static ZygiskDetectionResult detectZygisk(Context context) {
        ZygiskDetectionResult result = new ZygiskDetectionResult();
        
        // 方法1: 检查Magisk应用包
        result.setMagiskAppDetected(checkMagiskApp(context));
        
        // 方法2: 检查Zygisk库文件
        result.setZygiskLibDetected(checkZygiskLibrary());
        
        // 方法3: 检查Zygisk二进制文件
        result.setZygiskBinaryDetected(checkZygiskBinary());
        
        // 方法4: 检查Zygisk守护进程
        result.setZygiskDaemonDetected(checkZygiskDaemon());
        
        // 方法5: 检查Magisk模块中的Zygisk模块
        result.setZygiskModulesDetected(checkZygiskModules());
        
        // 方法6: 检查系统属性
        result.setSystemPropertiesDetected(checkSystemProperties());
        
        // 综合判断
        result.setZygiskInstalled(determineIfZygiskInstalled(result));
        
        return result;
    }
    
    /**
     * 检查Magisk应用
     */
    private static boolean checkMagiskApp(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String packageName : ZYGISK_PACKAGE_NAMES) {
            try {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                if (packageInfo != null) {
                    Log.d(TAG, "Found Magisk app: " + packageName);
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // 继续检查下一个包名
            }
        }
        return false;
    }
    
    /**
     * 检查Zygisk库文件
     */
    private static boolean checkZygiskLibrary() {
        for (String libPath : ZYGISK_LIB_PATHS) {
            File libFile = new File(libPath);
            if (libFile.exists()) {
                Log.d(TAG, "Found Zygisk library: " + libPath);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查Zygisk二进制文件
     */
    private static boolean checkZygiskBinary() {
        for (String binaryPath : ZYGISK_BINARIES) {
            File binaryFile = new File(binaryPath);
            if (binaryFile.exists() && binaryFile.canExecute()) {
                Log.d(TAG, "Found Zygisk binary: " + binaryPath);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查Zygisk守护进程
     */
    private static boolean checkZygiskDaemon() {
        try {
            // 检查zygiskd进程
            Process process = Runtime.getRuntime().exec("ps -A | grep zygisk");
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking zygisk daemon: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 检查Zygisk模块
     */
    private static boolean checkZygiskModules() {
        File modulesDir = new File("/data/adb/modules");
        if (!modulesDir.exists() || !modulesDir.isDirectory()) {
            return false;
        }
        
        File[] moduleDirs = modulesDir.listFiles();
        if (moduleDirs == null) {
            return false;
        }
        
        for (File moduleDir : moduleDirs) {
            // 检查模块是否包含Zygisk相关文件
            File zygiskDir = new File(moduleDir, "zygisk");
            if (zygiskDir.exists() && zygiskDir.isDirectory()) {
                Log.d(TAG, "Found Zygisk module: " + moduleDir.getName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查系统属性
     */
    private static boolean checkSystemProperties() {
        try {
            // 检查Magisk相关的系统属性
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class, String.class);
            
            String magiskVersion = (String) getMethod.invoke(null, "ro.magisk.version", "");
            String zygiskEnabled = (String) getMethod.invoke(null, "ro.dalvik.vm.native.bridge", "");
            
            if (!magiskVersion.isEmpty()) {
                Log.d(TAG, "Found Magisk system property: " + magiskVersion);
                return true;
            }
            
            if (zygiskEnabled != null && zygiskEnabled.contains("zygisk")) {
                Log.d(TAG, "Found Zygisk in native bridge: " + zygiskEnabled);
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking system properties: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 综合判断Zygisk是否安装
     */
    private static boolean determineIfZygiskInstalled(ZygiskDetectionResult result) {
        // 如果有Zygisk库文件或者二进制文件，基本可以确定安装了Zygisk
        if (result.isZygiskLibDetected() || result.isZygiskBinaryDetected()) {
            return true;
        }
        
        // 如果有Magisk应用并且检测到Zygisk模块，也认为安装了Zygisk
        if (result.isMagiskAppDetected() && result.isZygiskModulesDetected()) {
            return true;
        }
        
        // 如果检测到系统属性并且有守护进程
        if (result.isSystemPropertiesDetected() && result.isZygiskDaemonDetected()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取Zygisk状态文本
     */
    public static String getZygiskStatusText(Context context) {
        ZygiskDetectionResult result = detectZygisk(context);
        
        if (result.isZygiskInstalled()) {
            if (result.isMagiskAppDetected()) {
                return "Zygisk (Magisk)";
            } else if (result.isZygiskLibDetected()) {
                return "Zygisk (Library)";
            } else if (result.isZygiskBinaryDetected()) {
                return "Zygisk (Binary)";
            } else {
                return "Zygisk Detected";
            }
        } else {
            return "Not detected";
        }
    }
    
    /**
     * Zygisk检测结果类
     */
    public static class ZygiskDetectionResult {
        private boolean zygiskInstalled;
        private boolean magiskAppDetected;
        private boolean zygiskLibDetected;
        private boolean zygiskBinaryDetected;
        private boolean zygiskDaemonDetected;
        private boolean zygiskModulesDetected;
        private boolean systemPropertiesDetected;
        
        // Getter and Setter methods
        public boolean isZygiskInstalled() { return zygiskInstalled; }
        public void setZygiskInstalled(boolean zygiskInstalled) { this.zygiskInstalled = zygiskInstalled; }
        
        public boolean isMagiskAppDetected() { return magiskAppDetected; }
        public void setMagiskAppDetected(boolean magiskAppDetected) { this.magiskAppDetected = magiskAppDetected; }
        
        public boolean isZygiskLibDetected() { return zygiskLibDetected; }
        public void setZygiskLibDetected(boolean zygiskLibDetected) { this.zygiskLibDetected = zygiskLibDetected; }
        
        public boolean isZygiskBinaryDetected() { return zygiskBinaryDetected; }
        public void setZygiskBinaryDetected(boolean zygiskBinaryDetected) { this.zygiskBinaryDetected = zygiskBinaryDetected; }
        
        public boolean isZygiskDaemonDetected() { return zygiskDaemonDetected; }
        public void setZygiskDaemonDetected(boolean zygiskDaemonDetected) { this.zygiskDaemonDetected = zygiskDaemonDetected; }
        
        public boolean isZygiskModulesDetected() { return zygiskModulesDetected; }
        public void setZygiskModulesDetected(boolean zygiskModulesDetected) { this.zygiskModulesDetected = zygiskModulesDetected; }
        
        public boolean isSystemPropertiesDetected() { return systemPropertiesDetected; }
        public void setSystemPropertiesDetected(boolean systemPropertiesDetected) { this.systemPropertiesDetected = systemPropertiesDetected; }
        
        @Override
        public String toString() {
            return "ZygiskDetectionResult{" +
                    "zygiskInstalled=" + zygiskInstalled +
                    ", magiskAppDetected=" + magiskAppDetected +
                    ", zygiskLibDetected=" + zygiskLibDetected +
                    ", zygiskBinaryDetected=" + zygiskBinaryDetected +
                    ", zygiskDaemonDetected=" + zygiskDaemonDetected +
                    ", zygiskModulesDetected=" + zygiskModulesDetected +
                    ", systemPropertiesDetected=" + systemPropertiesDetected +
                    '}';
        }
    }
}