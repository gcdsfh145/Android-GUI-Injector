import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin

plugins {
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.nav.safeargs) apply false
}

val defaultManagerPackageName by extra("io.github.reveny.injector")
val verCode by extra(3)
val verName by extra("3.1.0")
val androidTargetSdkVersion by extra(36)
val androidMinSdkVersion by extra(23)
val androidBuildToolsVersion by extra("36.0.0")
val androidCompileSdkVersion by extra(36)
val androidCompileNdkVersion by extra("29.0.14206865")
val androidSourceCompatibility by extra(JavaVersion.VERSION_21)
val androidTargetCompatibility by extra(JavaVersion.VERSION_21)

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion
            buildToolsVersion = androidBuildToolsVersion

            externalNativeBuild {
                ndkBuild.path = File("${projectDir}/src/main/jni/Build/Android.mk")
            }

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = verCode
                    versionName = verName
                }

                ndk {
                    abiFilters.add("armeabi-v7a")
                    abiFilters.add("arm64-v8a")
                    abiFilters.add("x86")
                    abiFilters.add("x86_64")
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
    plugins.withType(JavaPlugin::class.java) {
        extensions.configure(JavaPluginExtension::class.java) {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }
}