//
// Created by reveny on 5/30/24.
//
#include <Proxy/Headers/JNIProxy.hpp>

#include <Include/Logger.hpp>
#include <Include/SoList.hpp>
#include <Include/RemapTools.hpp>

#include <xdl.h>

// https://github.com/Dr-TSNG/ZygiskNext/blob/338d3-165-11ce64378-17f66d9e4f179dc5b-1d1a8f/loader/src/injector/hook.cpp#L261
auto JNIProxy::GetCreatedJavaVMS() {
    auto getCreatedJavaVMS = reinterpret_cast<jint (*)(JavaVM **, jsize, jsize *)>(dlsym(RTLD_DEFAULT, "JNI_GetCreatedJavaVMs"));
    if (getCreatedJavaVMS != nullptr) {
        return getCreatedJavaVMS;
    }

    // Aquire the path from maps
    std::string nativeHelper = {};
    std::vector<RemapTools::MapInfo> maps = RemapTools::ListModulesWithName(-1, "libnativehelper.so");
    for (RemapTools::MapInfo info : maps) {
        nativeHelper = info.path;
        break;
    }
    LOGI("[+] Found libnativehelper.so at: %s", nativeHelper.c_str());
    void *handle = xdl_open(nativeHelper.c_str(), XDL_TRY_FORCE_LOAD);

    if (handle != nullptr) {
        getCreatedJavaVMS = reinterpret_cast<jint (*)(JavaVM **, jsize, jsize *)>(xdl_sym(handle, "JNI_GetCreatedJavaVMs",nullptr));
        if (getCreatedJavaVMS == nullptr) {
            LOGW("[-] Failed to find JNI_GetCreatedJavaVMs in libnativehelper.so: %s", dlerror());
        } else {
            LOGI("[+] Found JNI_GetCreatedJavaVMs in libnativehelper.so");
            return getCreatedJavaVMS;
        }
    }

    // Could also be in libart.so
    std::string libArt = {};
    std::vector<RemapTools::MapInfo> artMaps = RemapTools::ListModulesWithName(-1, "libart.so");
    for (RemapTools::MapInfo info : artMaps) {
        libArt = info.path;
        break;
    }
    LOGI("[+] Found libart.so at: %s", libArt.c_str());
    handle = xdl_open(libArt.c_str(), XDL_TRY_FORCE_LOAD);

    if (handle == nullptr) {
        LOGW("[-] Failed to load libart.so: %s", dlerror());
    }

    getCreatedJavaVMS = reinterpret_cast<jint (*)(JavaVM **, jsize, jsize *)>(xdl_sym(handle, "JNI_GetCreatedJavaVMs", nullptr));
    if (getCreatedJavaVMS == nullptr) {
        LOGW("[-] Failed to find JNI_GetCreatedJavaVMs in libart.so: %s", dlerror());
    } else {
        LOGI("[+] Found JNI_GetCreatedJavaVMs in libart.so");
        return getCreatedJavaVMS;
    }
}

int JNIProxy::JNILoad(JavaVM *vm, std::string libraryPath) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("[-] Failed to get JNIEnv");
        return -1;
    }

    if (vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
        LOGE("[-] Failed to attach current thread");
        return -1;
    }

    LOGI("[+] JNILoad called");

    // Load library
    jclass systemClass = env->FindClass("java/lang/System");
    jmethodID loadLibraryMethod = env->GetStaticMethodID(systemClass, "load", "(Ljava/lang/String;)V");

    // When calling System.load, it will automatically do the arm translation for us
    // This likely works on 9-1% of emulators
    LOGI("[+] Loading library: %s", libraryPath.c_str());
    jstring jLibraryPath = env->NewStringUTF(libraryPath.c_str());
    env->CallStaticVoidMethod(systemClass, loadLibraryMethod, jLibraryPath);

    return 1;
}

int JNIProxy::DexLoad(JavaVM *vm, RemoteInjectorData *data) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("[-] Failed to get JNIEnv");
        return -1;
    }

    if (vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
        LOGE("[-] Failed to attach current thread");
        return -1;
    }

    LOGI("[+] DexLoad called for: %s", (char*)data->libraryPath);

    // Get a ClassLoader. Using the system class loader might not find application classes,
    // but for loading a new DEX it should be fine as a parent.
    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID getSystemClassLoaderMethod = env->GetStaticMethodID(classLoaderClass, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    jobject systemClassLoader = env->CallStaticObjectMethod(classLoaderClass, getSystemClassLoaderMethod);

    // Path to DEX and optimized directory (cache)
    jstring dexPath = env->NewStringUTF((char*)data->libraryPath);
    
    // Create DexClassLoader
    jclass dexClassLoaderClass = env->FindClass("dalvik/system/DexClassLoader");
    jmethodID dexClassLoaderContructor = env->GetMethodID(dexClassLoaderClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");

    // In modern Android (API 26+), optimizedDirectory should be null.
    // The system will manage optimized dex files in the app's private data directory.
    jobject dexClassLoader = env->NewObject(dexClassLoaderClass, dexClassLoaderContructor, dexPath, NULL, NULL, systemClassLoader);

    if (dexClassLoader == NULL) {
        LOGE("[-] Failed to create DexClassLoader");
        return -1;
    }

    // Load the class
    jmethodID loadClassMethod = env->GetMethodID(dexClassLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring className = env->NewStringUTF((char*)data->dexClassName);
    jclass loadedClass = (jclass)env->CallObjectMethod(dexClassLoader, loadClassMethod, className);

    if (loadedClass == NULL) {
        LOGE("[-] Failed to load class: %s", (char*)data->dexClassName);
        return -1;
    }

    // Get the current Application (Context)
    jclass activityThreadClass = env->FindClass("android/app/ActivityThread");
    jmethodID currentActivityThreadMethod = env->GetStaticMethodID(activityThreadClass, "currentActivityThread", "()Landroid/app/ActivityThread;");
    jobject activityThread = env->CallStaticObjectMethod(activityThreadClass, currentActivityThreadMethod);
    jmethodID getApplicationMethod = env->GetMethodID(activityThreadClass, "getApplication", "()Landroid/app/Application;");
    jobject currentContext = env->CallObjectMethod(activityThread, getApplicationMethod);

    if (currentContext == NULL) {
        LOGW("[!] Could not get Application Context, falling back to argumentless call");
    }

    // Try calling with (Context) parameter
    jmethodID methodId = env->GetStaticMethodID(loadedClass, (char*)data->dexMethodName, "(Landroid/content/Context;)V");
    if (methodId != NULL && currentContext != NULL) {
        env->CallStaticVoidMethod(loadedClass, methodId, currentContext);
        LOGI("[+] Successfully called static %s(Context)", (char*)data->dexMethodName);
        return 1;
    }

    // Fallback: Call the method (assuming it's a static method with no arguments)
    methodId = env->GetStaticMethodID(loadedClass, (char*)data->dexMethodName, "()V");
    if (methodId == NULL) {
        // Try with ([Ljava/lang/String;)V (standard main method)
        methodId = env->GetStaticMethodID(loadedClass, (char*)data->dexMethodName, "([Ljava/lang/String;)V");
        if (methodId != NULL) {
            jclass stringClass = env->FindClass("java/lang/String");
            jobjectArray args = env->NewObjectArray(0, stringClass, NULL);
            env->CallStaticVoidMethod(loadedClass, methodId, args);
            LOGI("[+] Successfully called static %s with String[] args", (char*)data->dexMethodName);
            return 1;
        }
        LOGE("[-] Failed to find static method: %s", (char*)data->dexMethodName);
        return -1;
    }

    env->CallStaticVoidMethod(loadedClass, methodId);
    LOGI("[+] Successfully called static %s", (char*)data->dexMethodName);

    return 1;
}

int JNIProxy::Inject(RemoteInjectorData *data) {
    auto get_created_java_vms = GetCreatedJavaVMS();
    if (get_created_java_vms == nullptr) {
        LOGE("[-] Failed to find get_created_java_vms");
        return -1;
    }
    LOGI("[+] get_created_java_vms: %p", get_created_java_vms);

    JavaVM *vm = nullptr;
    jsize num = -1;
    jint res = get_created_java_vms(&vm, 1, &num);

    if (res != JNI_OK || vm == nullptr) {
        LOGE("[-] Failed to get JavaVM: %d", res);
        return -1;
    }

    if (data->injectType == 1) {
        return DexLoad(vm, data);
    }

    int loadResult = JNILoad(vm, (char *)data->libraryPath);
    if (loadResult == -1) {
        LOGE("[-] Failed to load library with JNI: %d", loadResult);
        return -1;
    }

    // Do hiding, this involves hiding from the linker as well as remapping if enabled.
    // Note that this can both be detected and not be fixed in usermode even with root.
    // Hiding Injection is practically impossible to fully hide but some protectors are not that advanced.
    if (data->hideLibrary) {
        if (SoList::Initialize()) {
            SoList::NullifySoName((char *)data->libraryPath);
        }
    }

    if (data->remapLibrary) {
        RemapTools::RemapLibrary((char *)data->libraryPath);
    }

    return 1;
}