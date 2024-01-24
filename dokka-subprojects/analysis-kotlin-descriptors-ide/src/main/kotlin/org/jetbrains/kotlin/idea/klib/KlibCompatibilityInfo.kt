/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:JvmName("KlibCompatibilityInfoUtils")

package org.jetbrains.kotlin.idea.klib


import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.metadataVersion
import java.io.IOException

/**
 * Whether a certain KLIB is compatible for the purposes of IDE: indexation, resolve, etc.
 */
internal sealed class KlibCompatibilityInfo(val isCompatible: Boolean) {
    object Compatible : KlibCompatibilityInfo(true)
    object Pre14Layout : KlibCompatibilityInfo(false)
    class IncompatibleMetadata(val isOlder: Boolean) : KlibCompatibilityInfo(false)
}


internal fun <T> KotlinLibrary.safeRead(defaultValue: T, action: KotlinLibrary.() -> T) = try {
    action()
} catch (_: IOException) {
    defaultValue
}
internal val KotlinLibrary.compatibilityInfo: KlibCompatibilityInfo
    get() {
        val hasPre14Manifest = safeRead(false) { has_pre_1_4_manifest }
        if (hasPre14Manifest)
            return KlibCompatibilityInfo.Pre14Layout

        val metadataVersion = safeRead(null) { this.metadataVersion }
        @Suppress("DEPRECATION")
        return when {
            metadataVersion == null -> {
                // Too old KLIB format, even doesn't have metadata version
                KlibCompatibilityInfo.IncompatibleMetadata(true)
            }

            !metadataVersion.isCompatible() -> {
                val isOlder = metadataVersion.isAtLeast(KlibMetadataVersion.INSTANCE)
                KlibCompatibilityInfo.IncompatibleMetadata(!isOlder)
            }

            else -> KlibCompatibilityInfo.Compatible
        }
    }
