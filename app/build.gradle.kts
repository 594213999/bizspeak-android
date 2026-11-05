plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.bizspeak.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.bizspeak.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Vosk 需要真实文件路径，模型文件不要压缩，否则解压到 filesDir 时耗时很久
    androidResources {
        noCompress += listOf("mdl", "fst", "int", "dubm", "conf")
    }

    // 真机安装用：release 包自签名（首次生成 keystore，之后复用）
    signingConfigs {
        create("release") {
            val ks = rootProject.file("bizspeak-release.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("BIZSPEAK_KS_PASS") ?: "bizspeak123"
                keyAlias = "bizspeak"
                keyPassword = System.getenv("BIZSPEAK_KEY_PASS") ?: "bizspeak123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (rootProject.file("bizspeak-release.jks").exists()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            // 真机主要是 arm64，只保留 arm64 + armeabi-v7a，减小体积
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        debug {
            ndk {
                abiFilters += listOf("x86_64", "arm64-v8a", "armeabi-v7a")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.vosk.android)

    "ksp"(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
