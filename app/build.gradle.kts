plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.kapt") }
android {
 namespace="com.momentum.app"; compileSdk=36
 defaultConfig {
  applicationId=if(providers.gradleProperty("momentumQa").isPresent)"com.momentum.app.qa" else "com.momentum.app"; minSdk=29; targetSdk=36; versionCode=7; versionName="4.1"
  manifestPlaceholders["appLabel"]=if(providers.gradleProperty("momentumQa").isPresent)"Momentum QA" else "Momentum"
  manifestPlaceholders["appIcon"]=if(providers.gradleProperty("momentumQa").isPresent)"@drawable/ic_momentum_qa" else "@drawable/ic_momentum"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
  javaCompileOptions { annotationProcessorOptions { arguments["room.schemaLocation"]="$projectDir/schemas" } }
 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 buildFeatures { compose=true }
 buildTypes {
  getByName("release") {
   isMinifyEnabled=true
   isShrinkResources=true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
  }
 }
 bundle { language { enableSplit=false } }
 sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
 testOptions { unitTests.isIncludeAndroidResources=true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.10.01"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.navigation:navigation-compose:2.9.5")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 implementation("androidx.room:room-runtime:2.8.2")
 implementation("androidx.room:room-ktx:2.8.2")
 kapt("androidx.room:room-compiler:2.8.2")
 implementation("androidx.datastore:datastore-preferences:1.1.7")
 implementation("androidx.work:work-runtime-ktx:2.10.5")
 implementation("androidx.core:core-ktx:1.17.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.work:work-testing:2.10.5")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.room:room-testing:2.8.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}