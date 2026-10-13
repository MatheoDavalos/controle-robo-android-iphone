plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "br.edu.robotcontroller"
    compileSdk = 34
    defaultConfig {
        applicationId = "br.edu.robotcontroller"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.5"
        manifestPlaceholders["orientation"] = "unspecified"
        buildConfigField("boolean", "FINAL_APP", "false")
    }
    buildFeatures { buildConfig = true }
    flavorDimensions += "stage"
    productFlavors {
        create("controleRobo") {
            dimension = "stage"
            applicationIdSuffix = ".finalapp"
            manifestPlaceholders["launcherActivity"] = ".MainActivity"
            manifestPlaceholders["appLabel"] = "Controle_Robo"
            manifestPlaceholders["orientation"] = "sensorLandscape"
            buildConfigField("boolean", "FINAL_APP", "true")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies { testImplementation("junit:junit:4.13.2") }
