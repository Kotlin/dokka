package org.jetbrains.dokka.analysis.kotlin.descriptors.ide

import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerExtensionPointProvider
import org.jetbrains.kotlin.caches.resolve.CommonPlatformKindResolution
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.caches.resolve.JsPlatformKindResolution
import org.jetbrains.kotlin.caches.resolve.JvmPlatformKindResolution
import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.ide.konan.NativePlatformKindResolution
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind

internal class IdeCompilerExtensionPointProvider : CompilerExtensionPointProvider {
    override fun get(): List<CompilerExtensionPointProvider.CompilerExtensionPoint> {

        @Suppress("UNCHECKED_CAST")
        val idePlatformKind = CompilerExtensionPointProvider.CompilerExtensionPoint(
            ApplicationExtensionDescriptor(
                "org.jetbrains.kotlin.idePlatformKind",
                IdePlatformKind::class.java
            ) as ApplicationExtensionDescriptor<Any>,
            listOf(
                CommonIdePlatformKind,
                JvmIdePlatformKind,
                JsIdePlatformKind,
                NativeIdePlatformKind
            )
        )

        @Suppress("UNCHECKED_CAST")
        val resolution = CompilerExtensionPointProvider.CompilerExtensionPoint(
            IdePlatformKindResolution as ApplicationExtensionDescriptor<Any>,
            listOf(
                CommonPlatformKindResolution(),
                JvmPlatformKindResolution(),
                JsPlatformKindResolution(),
                NativePlatformKindResolution()
            )
        )

        return listOf(idePlatformKind, resolution)
    }
}
