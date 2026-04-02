//
// Created by reveny on 5/30/24.
//
#include <Proxy/Headers/JNIProxy.hpp>

#include <Include/Logger.hpp>
#include <Include/SoList.hpp>
#include <Include/RemapTools.hpp>

#include <xdl.h>

namespace {
    bool ClearJniException(JNIEnv *env, const char *stage, bool logAsWarning = false) {
        if (env == nullptr || !env->ExceptionCheck()) {
            return false;
        }

        env->ExceptionDescribe();
        env->ExceptionClear();
        if (logAsWarning) {
            LOGW("[!] JNI exception cleared at %s", stage);
        } else {
            LOGE("[-] JNI exception cleared at %s", stage);
        }
        return true;
    }

    jobject GetApplicationContext(JNIEnv *env) {
        jclass activityThreadClass = env->FindClass("android/app/ActivityThread");
        if (activityThreadClass == nullptr) {
            ClearJniException(env, "FindClass(ActivityThread)");
            return nullptr;
        }

        jmethodID currentActivityThreadMethod = env->GetStaticMethodID(
            activityThreadClass,
            "currentActivityThread",
            "()Landroid/app/ActivityThread;"
        );
        if (currentActivityThreadMethod == nullptr) {
            ClearJniException(env, "GetStaticMethodID(currentActivityThread)");
            env->DeleteLocalRef(activityThreadClass);
            return nullptr;
        }

        jobject activityThread = env->CallStaticObjectMethod(activityThreadClass, currentActivityThreadMethod);
        if (ClearJniException(env, "CallStaticObjectMethod(currentActivityThread)") || activityThread == nullptr) {
            env->DeleteLocalRef(activityThreadClass);
            return nullptr;
        }

        jmethodID getApplicationMethod = env->GetMethodID(activityThreadClass, "getApplication", "()Landroid/app/Application;");
        if (getApplicationMethod == nullptr) {
            ClearJniException(env, "GetMethodID(getApplication)");
            env->DeleteLocalRef(activityThread);
            env->DeleteLocalRef(activityThreadClass);
            return nullptr;
        }

        jobject application = env->CallObjectMethod(activityThread, getApplicationMethod);
        if (ClearJniException(env, "CallObjectMethod(getApplication)", true)) {
            application = nullptr;
        }

        env->DeleteLocalRef(activityThread);
        env->DeleteLocalRef(activityThreadClass);
        return application;
    }

    jobject GetPreferredParentClassLoader(JNIEnv *env, jobject context) {
        if (context != nullptr) {
            jclass contextClass = env->GetObjectClass(context);
            if (contextClass != nullptr) {
                jmethodID getClassLoaderMethod = env->GetMethodID(contextClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
                if (getClassLoaderMethod != nullptr) {
                    jobject loader = env->CallObjectMethod(context, getClassLoaderMethod);
                    if (!ClearJniException(env, "Context.getClassLoader", true) && loader != nullptr) {
                        env->DeleteLocalRef(contextClass);
                        return loader;
                    }
                } else {
                    ClearJniException(env, "GetMethodID(getClassLoader)", true);
                }
                env->DeleteLocalRef(contextClass);
            }
        }

        jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
        if (classLoaderClass == nullptr) {
            ClearJniException(env, "FindClass(ClassLoader)");
            return nullptr;
        }

        jmethodID getSystemClassLoaderMethod = env->GetStaticMethodID(
            classLoaderClass,
            "getSystemClassLoader",
            "()Ljava/lang/ClassLoader;"
        );
        if (getSystemClassLoaderMethod == nullptr) {
            ClearJniException(env, "GetStaticMethodID(getSystemClassLoader)");
            env->DeleteLocalRef(classLoaderClass);
            return nullptr;
        }

        jobject systemClassLoader = env->CallStaticObjectMethod(classLoaderClass, getSystemClassLoaderMethod);
        if (ClearJniException(env, "CallStaticObjectMethod(getSystemClassLoader)")) {
            systemClassLoader = nullptr;
        }

        env->DeleteLocalRef(classLoaderClass);
        return systemClassLoader;
    }

    jstring GetOptimizedDirectory(JNIEnv *env, jobject context) {
        if (context == nullptr) {
            return nullptr;
        }

        jclass contextClass = env->GetObjectClass(context);
        if (contextClass == nullptr) {
            ClearJniException(env, "GetObjectClass(context)");
            return nullptr;
        }

        jmethodID getCacheDirMethod = env->GetMethodID(contextClass, "getCacheDir", "()Ljava/io/File;");
        if (getCacheDirMethod == nullptr) {
            ClearJniException(env, "GetMethodID(getCacheDir)", true);
            env->DeleteLocalRef(contextClass);
            return nullptr;
        }

        jobject cacheDir = env->CallObjectMethod(context, getCacheDirMethod);
        if (ClearJniException(env, "CallObjectMethod(getCacheDir)", true) || cacheDir == nullptr) {
            env->DeleteLocalRef(contextClass);
            return nullptr;
        }

        jclass fileClass = env->FindClass("java/io/File");
        if (fileClass == nullptr) {
            ClearJniException(env, "FindClass(File)", true);
            env->DeleteLocalRef(cacheDir);
            env->DeleteLocalRef(contextClass);
            return nullptr;
        }

        jmethodID getAbsolutePathMethod = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
        if (getAbsolutePathMethod == nullptr) {
            ClearJniException(env, "GetMethodID(getAbsolutePath)", true);
            env->DeleteLocalRef(fileClass);
            env->DeleteLocalRef(cacheDir);
            env->DeleteLocalRef(contextClass);
            return nullptr;
        }

        jstring optimizedDir = (jstring)env->CallObjectMethod(cacheDir, getAbsolutePathMethod);
        if (ClearJniException(env, "CallObjectMethod(getAbsolutePath)", true)) {
            optimizedDir = nullptr;
        }

        env->DeleteLocalRef(fileClass);
        env->DeleteLocalRef(cacheDir);
        env->DeleteLocalRef(contextClass);
        return optimizedDir;
    }

    int InvokeDexEntry(JNIEnv *env, jclass loadedClass, const char *methodName, jobject context) {
        struct Candidate {
            const char *signature;
            int mode;
        };

        const Candidate candidates[] = {
            {"(Landroid/content/Context;)V", 0},
            {"(Landroid/app/Application;)V", 1},
            {"()V", 2},
            {"([Ljava/lang/String;)V", 3},
            {"(Ljava/lang/String;)V", 4}
        };

        for (const Candidate &candidate : candidates) {
            jmethodID methodId = env->GetStaticMethodID(loadedClass, methodName, candidate.signature);
            if (methodId == nullptr) {
                ClearJniException(env, candidate.signature, true);
                continue;
            }

            switch (candidate.mode) {
                case 0:
                case 1:
                    if (context == nullptr) {
                        continue;
                    }
                    env->CallStaticVoidMethod(loadedClass, methodId, context);
                    break;
                case 2:
                    env->CallStaticVoidMethod(loadedClass, methodId);
                    break;
                case 3: {
                    jclass stringClass = env->FindClass("java/lang/String");
                    jobjectArray args = env->NewObjectArray(0, stringClass, nullptr);
                    env->CallStaticVoidMethod(loadedClass, methodId, args);
                    env->DeleteLocalRef(args);
                    env->DeleteLocalRef(stringClass);
                    break;
                }
                case 4: {
                    jstring empty = env->NewStringUTF("");
                    env->CallStaticVoidMethod(loadedClass, methodId, empty);
                    env->DeleteLocalRef(empty);
                    break;
                }
            }

            if (ClearJniException(env, candidate.signature)) {
                continue;
            }

            LOGI("[+] Successfully called static %s with signature %s", methodName, candidate.signature);
            return 1;
        }

        return -1;
    }
}

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

    jobject currentContext = GetApplicationContext(env);
    if (currentContext == nullptr) {
        LOGW("[!] Could not get Application Context, continuing with fallback class loader.");
    } else {
        LOGI("[+] Acquired Application Context for DEX entry invocation.");
    }

    jobject parentClassLoader = GetPreferredParentClassLoader(env, currentContext);
    if (parentClassLoader == nullptr) {
        LOGE("[-] Failed to acquire a parent ClassLoader");
        if (currentContext != nullptr) {
            env->DeleteLocalRef(currentContext);
        }
        return -1;
    }

    jstring dexPath = env->NewStringUTF((char*)data->libraryPath);
    jstring optimizedDir = GetOptimizedDirectory(env, currentContext);

    jclass dexClassLoaderClass = env->FindClass("dalvik/system/DexClassLoader");
    if (dexClassLoaderClass == nullptr) {
        ClearJniException(env, "FindClass(DexClassLoader)");
        env->DeleteLocalRef(parentClassLoader);
        if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
        env->DeleteLocalRef(dexPath);
        if (currentContext != nullptr) env->DeleteLocalRef(currentContext);
        return -1;
    }

    jmethodID dexClassLoaderContructor = env->GetMethodID(dexClassLoaderClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    if (dexClassLoaderContructor == nullptr) {
        ClearJniException(env, "GetMethodID(DexClassLoader.<init>)");
        env->DeleteLocalRef(dexClassLoaderClass);
        env->DeleteLocalRef(parentClassLoader);
        if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
        env->DeleteLocalRef(dexPath);
        if (currentContext != nullptr) env->DeleteLocalRef(currentContext);
        return -1;
    }

    jobject dexClassLoader = env->NewObject(
        dexClassLoaderClass,
        dexClassLoaderContructor,
        dexPath,
        optimizedDir,
        nullptr,
        parentClassLoader
    );
    if (ClearJniException(env, "NewObject(DexClassLoader)")) {
        dexClassLoader = nullptr;
    }

    if (dexClassLoader == NULL) {
        LOGE("[-] Failed to create DexClassLoader");
        env->DeleteLocalRef(dexClassLoaderClass);
        env->DeleteLocalRef(parentClassLoader);
        if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
        env->DeleteLocalRef(dexPath);
        if (currentContext != nullptr) env->DeleteLocalRef(currentContext);
        return -1;
    }

    jmethodID loadClassMethod = env->GetMethodID(dexClassLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (loadClassMethod == nullptr) {
        ClearJniException(env, "GetMethodID(loadClass)");
        env->DeleteLocalRef(dexClassLoader);
        env->DeleteLocalRef(dexClassLoaderClass);
        env->DeleteLocalRef(parentClassLoader);
        if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
        env->DeleteLocalRef(dexPath);
        if (currentContext != nullptr) env->DeleteLocalRef(currentContext);
        return -1;
    }

    jstring className = env->NewStringUTF((char*)data->dexClassName);
    jclass loadedClass = (jclass)env->CallObjectMethod(dexClassLoader, loadClassMethod, className);
    if (ClearJniException(env, "CallObjectMethod(loadClass)")) {
        loadedClass = nullptr;
    }

    if (loadedClass == NULL) {
        LOGE("[-] Failed to load class: %s", (char*)data->dexClassName);
        env->DeleteLocalRef(className);
        env->DeleteLocalRef(dexClassLoader);
        env->DeleteLocalRef(dexClassLoaderClass);
        env->DeleteLocalRef(parentClassLoader);
        if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
        env->DeleteLocalRef(dexPath);
        if (currentContext != nullptr) env->DeleteLocalRef(currentContext);
        return -1;
    }

    int result = InvokeDexEntry(env, loadedClass, (char*)data->dexMethodName, currentContext);
    if (result == -1) {
        LOGE("[-] Failed to find or invoke static method: %s", (char*)data->dexMethodName);
    }

    env->DeleteLocalRef(loadedClass);
    env->DeleteLocalRef(className);
    env->DeleteLocalRef(dexClassLoader);
    env->DeleteLocalRef(dexClassLoaderClass);
    env->DeleteLocalRef(parentClassLoader);
    if (optimizedDir != nullptr) env->DeleteLocalRef(optimizedDir);
    env->DeleteLocalRef(dexPath);
    if (currentContext != nullptr) env->DeleteLocalRef(currentContext);

    return result;
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
