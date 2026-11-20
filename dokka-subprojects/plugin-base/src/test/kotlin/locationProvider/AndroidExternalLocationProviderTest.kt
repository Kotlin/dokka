/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.AndroidExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.plugability.DokkaContext
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidExternalLocationProviderTest : BaseAbstractTest() {
    private val android = ExternalDocumentation(
        URL("https://developer.android.com/reference/kotlin"),
        PackageList(
            RecognizedLinkFormat.DokkaHtml,
            mapOf("" to setOf("android.content", "android.net", "android.os", "java.lang")),
            emptyMap(),
            URL("file://not-used")
        )
    )
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun getLocationProvider(
        context: DokkaContext? = null
    ): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        return AndroidExternalLocationProvider(android, dokkaContext)
    }

    @Test
    fun `anchor to a method is lowercase`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "android.content",
            "ContextWrapper",
            Callable(
                "checkCallingUriPermission",
                null,
                listOf(
                    TypeConstructor("android.net.Uri", emptyList()),
                    TypeConstructor("kotlin.Int", emptyList())
                )
            )
        )

        assertEquals(
            "${android.documentationURL}/android/content/ContextWrapper.html#checkcallinguripermission",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `anchor to a property from Android`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "android.os",
            "Build",
            Callable(
                name = "DEVICE",
                params = emptyList(),
                isProperty = true
            )
        )

        assertEquals(
            "${android.documentationURL}/android/os/Build.html#device",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `anchor to a Java property from Android documentation`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "java.lang",
            "Integer",
            Callable(
                name = "MAX_VALUE",
                params = emptyList(),
                isProperty = true
            )
        )

        assertEquals(
            "${android.documentationURL}/java/lang/Integer.html#max_value",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `should return null for method not in list`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "foo",
            "Bar",
            Callable(
                "baz",
                null,
                emptyList()
            )
        )

        assertEquals(null, locationProvider.resolve(dri))
    }
}
