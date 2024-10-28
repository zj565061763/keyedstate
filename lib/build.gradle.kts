plugins {
   alias(libs.plugins.android.library)
   alias(libs.plugins.kotlin.android)
   `maven-publish`
}

val libGroupId = "com.sd.lib.android"
val libArtifactId = "keyedstate"
val libVersion = "1.0.0"

android {
   namespace = "com.sd.lib.keyedstate"
   compileSdk = libs.versions.androidCompileSdk.get().toInt()
   defaultConfig {
      minSdk = 21
   }

   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
   }

   kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs += "-module-name=$libGroupId.$libArtifactId"
   }

   publishing {
      singleVariant("release") {
         withSourcesJar()
      }
   }
}

dependencies {
   implementation(libs.kotlin.coroutines)
}

publishing {
   publications {
      create<MavenPublication>("release") {
         groupId = libGroupId
         artifactId = libArtifactId
         version = libVersion
         afterEvaluate {
            from(components["release"])
         }
      }
   }
}