/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import org.jetbrains.dokka.Platform


/**
 * The Kotlin Platform that code is compiled to.
 *
 * This is a separate implementation of [org.jetbrains.dokka.Platform] to avoid requiring Dokka
 * classes in build scripts.
 *
 * @see org.jetbrains.dokka.Platform
 * @param[displayName] The display name, eventually used in the rendered Dokka publication.
 */
enum class KotlinPlatform(
    internal val displayName: String
) {
    AndroidJVM("Android JVM"),
    Common("Common"),
    JS("JS"),
    JVM("JVM"),
    Native("Native"),
    Wasm("Wasm"),
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

            return when (key.toLowerCase()) {
                "android" -> AndroidJVM
                "metadata" -> Common
                else -> error("Unrecognized platform: $key")
            }
        }

        // Not defined as a property to try and minimize the dependency on Dokka Core types
        internal val KotlinPlatform.dokkaType: Platform
            get() =
                when (this) {
                    AndroidJVM, JVM -> Platform.jvm
                    JS -> Platform.js
                    Wasm -> Platform.wasm
                    Native -> Platform.native
                    Common -> Platform.common
                }
    }
}
