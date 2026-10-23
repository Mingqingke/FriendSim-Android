plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// 发布签名：从根目录 keystore.properties 读取（该文件不入库）；没有则回退 debug 签名
// 简单键值解析，避免在 kts 里引入 java.util.Properties 的命名冲突
val keystoreFile = rootProject.file("keystore.properties")
val keystoreMap = mutableMapOf<String, String>()
if (keystoreFile.exists()) {
    keystoreFile.readLines().forEach { line ->
        val idx = line.indexOf('=')
        if (idx > 0) {
            keystoreMap[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
        }
    }
}
val hasReleaseKey = keystoreMap.containsKey("storeFile")

android {
    namespace = "com.friendsim.app"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.friendsim.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 26
        versionName = "2.1.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreMap.getValue("storeFile"))
                storePassword = keystoreMap.getValue("storePassword")
                keyAlias = keystoreMap.getValue("keyAlias")
                keyPassword = keystoreMap.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// 依赖版本与 KernelSU manager 最新 main 对齐（同一套 UI 栈，去掉 Root / 模块相关库）
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // MIUIx（KernelSU manager 同款组件）
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-squircle:0.9.4")

    // 网络（OpenAI 兼容 /chat/completions 流式）
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.5.0"))
    implementation("com.squareup.okhttp3:okhttp")

    // 仅 debug：Chucker（通知栏看 API 请求 URL/Body/响应）
    debugImplementation("com.github.chuckerteam.chucker:library:4.3.1")

    testImplementation("junit:junit:4.13.2")
}
