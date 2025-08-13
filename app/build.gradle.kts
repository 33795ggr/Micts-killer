import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Читаем свойства из файла
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Безопасная функция для получения свойств, которая выдаст понятную ошибку
fun getKeystoreProperty(key: String): String {
    val value = keystoreProperties.getProperty(key)
    require(value != null) {
        "Свойство '$key' отсутствует в файле keystore.properties. Убедитесь, что файл находится в корне проекта и содержит все необходимые строки."
    }
    return value
}

android {
    namespace = "com.lensshortcut.vivo"
    compileSdk = 35

    signingConfigs {
        create("release") {
            // Используем безопасную функцию
            // Если свойство не будет найдено, сборка упадет с понятным сообщением
            storeFile = file(getKeystoreProperty("storeFile"))
            storePassword = getKeystoreProperty("storePassword")
            keyAlias = getKeystoreProperty("keyAlias")
            keyPassword = getKeystoreProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.lensshortcut.vivo"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}