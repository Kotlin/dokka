package translators

import com.intellij.openapi.application.PathManager
import kotlinx.coroutines.Job
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.cast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExternalDocumentables : BaseAbstractTest() {
    @Test
    fun `external documentable from java stdlib`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += jvmStdlibPath!!
                }
            }
        }

        testInline(
            """
            /src/com/sample/MyList.kt
            package com.sample
            class MyList: ArrayList<Int>()
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            documentablesTransformationStage = { mod ->
                val entry = mod.packages.single().classlikes.single().cast<DClass>().supertypes.entries.single()
                val res = provider.findClasslike(
                    entry.value.single().typeConstructor.dri,
                    entry.key)
                assertEquals("ArrayList", res?.name)

                val supertypes = res?.cast<DClass>()?.supertypes?.values?.single()
                    ?.map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("AbstractList", "RandomAccess", "Cloneable", "Serializable", "MutableList"),
                    supertypes
                )
            }
        }
    }

    @Test
    fun `external documentable from dependency`() {
        val coroutinesPath =
            PathManager.getResourceRoot(Job::class.java, "/kotlinx/coroutines/Job.class")

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += listOf(jvmStdlibPath!!, coroutinesPath!!)
                }
            }
        }

        testInline(
            """
            /src/com/sample/MyJob.kt
            package com.sample
            import kotlinx.coroutines.Job
            abstract class MyJob: Job
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            documentablesTransformationStage = { mod ->
                val entry = mod.packages.single().classlikes.single().cast<DClass>().supertypes.entries.single()
                val res = provider.findClasslike(
                    entry.value.single().typeConstructor.dri,
                    entry.key)
                assertEquals("Job", res?.name)

                val supertypes = res?.cast<DInterface>()?.supertypes?.values?.single()
                    ?.map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("CoroutineContext.Element"),
                    supertypes
                )
            }
        }
    }

    @Test
    fun `external documentable for nested class`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += jvmStdlibPath!!
                }
            }
        }

        testInline(
            """
            /src/com/sample/MyList.kt
            package com.sample
            abstract class MyEntry: Map.Entry<Int, String>
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            documentablesTransformationStage = { mod ->
                val entry = mod.packages.single().classlikes.single().cast<DClass>().supertypes.entries.single()
                val res = provider.findClasslike(
                    entry.value.single().typeConstructor.dri,
                    entry.key)
                assertEquals("Entry", res?.name)
                assertEquals("Map.Entry", res?.dri?.classNames)
            }
        }
    }
}