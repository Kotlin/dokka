import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
//  id("com.android.library")
//  kotlin("multiplatform")
//  id("org.jetbrains.compose")
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  id("org.jetbrains.dokka")
}
group = "org.dokka.it.android.kmp"
version = "1.0"

android {
  namespace = "org.dokka.it.android.kmp"

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
}

kotlin {
  jvmToolchain(17)

  androidTarget {
    publishLibraryVariants("release")
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.animation)
      }
    }
  }
}
