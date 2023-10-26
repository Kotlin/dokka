package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.Platform


/**
 * The Kotlin
 *
 * @see org.jetbrains.dokka.Platform
 * @param[displayName] The display name, eventually used in the rendered Dokka publication.
 */
enum class KotlinPlatform(
  internal val displayName: String
) {
  AndroidJVM("androidJvm"),
  Common("common"),
  JS("js"),
  JVM("jvm"),
  Native("native"),
  WASM("wasm"),
  ;

  companion object {
    internal val values: Set<KotlinPlatform> = values().toSet()

    val DEFAULT: KotlinPlatform = JVM

    fun fromString(key: String): KotlinPlatform {
      val keyMatch = values.firstOrNull {
        it.name.equals(key, ignoreCase = true) || it.displayName.equals(key, ignoreCase = true)
      }
      if (keyMatch != null) {
        return keyMatch
      }

      return when (key.lowercase()) {
        "android"  -> AndroidJVM
        "metadata" -> Common
        else       -> error("Unrecognized platform: $key")
      }
    }

    // Not defined as a property to try and minimize the dependency on Dokka Core types
    internal val KotlinPlatform.dokkaType: Platform
      get() =
        when (this) {
          AndroidJVM, JVM -> Platform.jvm
          JS              -> Platform.js
          WASM            -> Platform.wasm
          Native          -> Platform.native
          Common          -> Platform.common
        }
  }
}
