//
// Created by reveny on 5/17/24.
//
#pragma once

#include <jni.h>
#include <string>

#include <Include/InjectorData.hpp>
#include <memory>

namespace Injector {
    extern "C" jint JNICALL Inject(JNIEnv* env, jclass clazz, jobject data);
    extern "C" jobjectArray JNICALL GetNativeLogs(JNIEnv *env, jclass clazz);

    std::string GenerateRandomString();
    std::string CreateRandomTempDirectory(std::shared_ptr<InjectorData> injectorData);
    void CopyFile(const std::filesystem::path& source, const std::filesystem::path& destination);
}
