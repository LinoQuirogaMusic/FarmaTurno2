plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    namespace = "com.linoquirogamusic.farmaciasdeturnochile"
    compileSdk = 35

    packaging {
        resources {
            // Excluir archivos duplicados de META-INF
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*,**/DEPENDENCIES,INDEX.LIST,*.properties,*.txt}"
            pickFirsts += "META-INF/*"
        }
    }

    defaultConfig {
        applicationId = "com.linoquirogamusic.farmaciasdeturnochile"
        minSdk = 27
        targetSdk = 35
        versionCode = 7
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

configurations.all {
    // Resolver conflictos de Netty
    exclude(group = "io.netty", module = "netty-common")
    exclude(group = "io.netty", module = "netty-codec-http2")
    exclude(group = "io.netty", module = "netty-handler-proxy")
    exclude(group = "io.netty", module = "netty-codec-http")
    exclude(group = "io.netty", module = "netty-handler")
    exclude(group = "io.netty", module = "netty-transport-native-unix-common")
    exclude(group = "io.netty", module = "netty-codec-socks")
    exclude(group = "io.netty", module = "netty-codec")
    exclude(group = "io.netty", module = "netty-transport")
    exclude(group = "io.netty", module = "netty-buffer")
    exclude(group = "io.netty", module = "netty-resolver")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    
    // Firebase
    implementation(platform(libs.firebase.bom)) {
        exclude(group = "io.netty")
    }
    implementation(libs.firebase.messaging.ktx)
    
    // Retrofit & Networking
    implementation(libs.retrofit) {
        exclude(group = "io.netty")
    }
    implementation(libs.converter.gson) {
        exclude(group = "io.netty")
    }
    implementation(libs.converter.scalars) {
        exclude(group = "io.netty")
    }
    implementation(libs.okhttp) {
        exclude(group = "io.netty")
    }
    implementation(libs.okhttp.logging.interceptor) {
        exclude(group = "io.netty")
    }
    
    implementation(libs.google.accompanist.pager)
    implementation(libs.google.accompanist.pager.indicators)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.ads) {
        exclude(group = "io.netty")
    }
    implementation(libs.firebase.appdistribution.gradle) {
        exclude(group = "io.netty")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}