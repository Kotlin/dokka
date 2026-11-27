/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.AndroidXExternalLocationProvider
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

class AndroidXExternalLocationProviderTest : BaseAbstractTest() {
    private val androidx = ExternalDocumentation(
        URL("https://developer.android.com/reference/kotlin"),
        PackageList(
            RecognizedLinkFormat.DokkaHtml,
            mapOf("" to setOf("androidx.appcompat.app", "androidx.core.app", "androidx.core.util")),
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
        return AndroidXExternalLocationProvider(androidx, dokkaContext)
    }

    @Test
    fun `anchor to a method is lowercase`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "androidx.appcompat.app",
            "AppCompatActivity",
            Callable("findViewById", null, listOf(TypeConstructor("kotlin.Int", emptyList())))
        )

        assertEquals(
            "${androidx.documentationURL}/androidx/appcompat/app/AppCompatActivity.html#findviewbyid",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `anchor to an uppercase property from AndroidX`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "androidx.core.app",
            "NotificationCompat",
            Callable(
                name = "PRIORITY_DEFAULT",
                params = emptyList(),
                isProperty = true
            )
        )

        assertEquals(
            "${androidx.documentationURL}/androidx/core/app/NotificationCompat.html#PRIORITY_DEFAULT()",
            locationProvider.resolve(dri)
        )
    }

    @Test
    fun `anchor to a lowercase instance property from AndroidX`() {
        val locationProvider = getLocationProvider()
        val dri = DRI(
            "androidx.core.util",
            "Pair",
            Callable(
                name = "first",
                params = emptyList(),
                isProperty = true
            )
        )

        assertEquals(
            "${androidx.documentationURL}/androidx/core/util/Pair.html#first()",
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
