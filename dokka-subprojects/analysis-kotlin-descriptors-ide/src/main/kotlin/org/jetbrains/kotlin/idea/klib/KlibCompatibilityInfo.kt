/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:JvmName("KlibCompatibilityInfoUtils")

package org.jetbrains.kotlin.idea.klib


import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.metadataVersion
import java.io.IOException

// File content is based on https://github.com/JetBrains/intellij-community/blob/76b51b233ad2e831c3e1fe8f7dcb0f7c77ad918a/plugins/kotlin/base/project-structure/src/org/jetbrains/kotlin/idea/base/projectStructure/moduleInfo/KlibCompatibilityInfo.kt

/**
 * Whether a certain KLIB is compatible for the purposes of IDE: indexation, resolve, etc.
 */
internal sealed class KlibCompatibilityInfo(val isCompatible: Boolean) {
    object Compatible : KlibCompatibilityInfo(true)
    class IncompatibleMetadata(val isOlder: Boolean) : KlibCompatibilityInfo(false)
}

internal fun <T> KotlinLibrary.safeRead(defaultValue: T, action: KotlinLibrary.() -> T) = try {
    action()
} catch (_: IOException) {
    defaultValue
}

internal val KotlinLibrary.compatibilityInfo: KlibCompatibilityInfo
    get() {
        val metadataVersion = safeRead(null) { this.metadataVersion }
        return when {
            metadataVersion == null -> {
                // Too old KLIB format, even doesn't have metadata version
                KlibCompatibilityInfo.IncompatibleMetadata(true)
            }

            !metadataVersion.isCompatibleWithCurrentCompilerVersion() -> {
                val isOlder = metadataVersion.isAtLeast(KlibMetadataVersion.INSTANCE)
                KlibCompatibilityInfo.IncompatibleMetadata(!isOlder)
            }

            else -> KlibCompatibilityInfo.Compatible
        }
    }
