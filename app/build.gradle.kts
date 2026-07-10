plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jasc.jascbattlechess"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jasc.jascbattlechess"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        // 🛠️ CORRECCIÓN: Cambiado de VERSION_11 a VERSION_17 para compatibilidad total
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // 🛠️ CORRECCIÓN: Cambiado de "11" a "17" para que coincida con las opciones de compilación
        jvmTarget = "17"
    }
}

dependencies {
    // ✅ Compose BOM (Maneja versiones de Compose automáticamente)
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // ✅ Core y Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // ✅ Media3 (ExoPlayer para tus videos de recompensa de 1000 pts)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // ✅ Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ✅ Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ✅ Tests locales (JUnit)
    testImplementation("junit:junit:4.13.2")

    // ✅ Tests instrumentados (AndroidX Test + Espresso)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ✅ Debug para tests
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}