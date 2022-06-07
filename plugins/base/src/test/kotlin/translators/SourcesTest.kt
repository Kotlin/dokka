package translators

import com.intellij.openapi.application.PathManager
import kotlinx.coroutines.Job
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.Language
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.cast
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SourcesTest : BaseAbstractTest() {
    @Test
    fun `external documentable from java stdlib`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += listOf(jvmStdlibPath!!,
                        PathManager.getResourceRoot(Element::class.java, "/org/jsoup/nodes/Element.class")!!
                            .replaceAfter(".jar", ""),
                        PathManager.getResourceRoot(Language::class.java, "/com/fasterxml/jackson/module/kotlin/KotlinModule.class")!!
                            .replaceAfter(".jar", "")
                    )

                }
            }
        }

        testInline(
            """
            /src/com/sample/MyList.kt
            import org.jsoup.nodes.Element
            import com.fasterxml.jackson.module.kotlin.KotlinModule
            package com.sample
            class MyList: ArrayList<Int>()
            
            class Jextern: org.jsoup.nodes.Element("Jextern")
            
            class Kstd: IntArray()
            
            class Kextern: com.fasterxml.jackson.module.kotlin.KotlinModule
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }

            documentablesTransformationStage = { mod ->
                val listEntry = mod.packages.single().classlikes.single { it.name == "MyList" }
                    .cast<DClass>().supertypes.entries.single()
                val listRes = provider.findClasslike(
                    listEntry.value.single().typeConstructor.dri,
                    listEntry.key)!!
                assertEquals("ArrayList", listRes.name)
                assertEquals("java.util/ArrayList///PointingToDeclaration/", listRes.dri.toString())

                val listSupertypes = listRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("AbstractList", "RandomAccess", "Cloneable", "Serializable", "MutableList"),
                    listSupertypes
                )
                assertEquals(Language.JAVA, listRes.sources.values.single().language)
                assertEquals("java.util", listRes.sources.values.single().path)

                val jexternEntry = mod.packages.single().classlikes.single { it.name == "Jextern" }
                    .cast<DClass>().supertypes.entries.single()
                val jexternRes = provider.findClasslike(
                    jexternEntry.value.single().typeConstructor.dri,
                    jexternEntry.key)!!
                assertEquals("Element", jexternRes.name)
                assertEquals("org.jsoup.nodes/Element///PointingToDeclaration/", jexternRes.dri.toString())

                val jexternSupertypes = jexternRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("Node"),
                    jexternSupertypes
                )
                assertEquals(Language.JAVA, jexternRes.sources.values.single().language)
                assertEquals("org.jsoup.nodes", jexternRes.sources.values.single().path)

                val kstdEntry = mod.packages.single().classlikes.single { it.name == "Kstd" }
                    .cast<DClass>().supertypes.entries.single()
                val kstdRes = provider.findClasslike(
                    kstdEntry.value.single().typeConstructor.dri,
                    kstdEntry.key)!!
                assertEquals("IntArray", kstdRes.name)
                assertEquals("kotlin/IntArray///PointingToDeclaration/", kstdRes.dri.toString())

                val kstdSupertypes = kstdRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("Cloneable", "Serializable"),
                    kstdSupertypes
                )
                assertEquals(Language.KOTLIN, kstdRes.sources.values.single().language)
                assertEquals("KotlinBuiltins.kt", kstdRes.sources.values.single().path)

                val kexternEntry = mod.packages.single().classlikes.single { it.name == "Kextern" }
                    .cast<DClass>().supertypes.entries.single()
                val kexternRes = provider.findClasslike(
                    kexternEntry.value.single().typeConstructor.dri,
                    kexternEntry.key)!!
                assertEquals("KotlinModule", kexternRes.name)
                assertEquals("com.fasterxml.jackson.module.kotlin/KotlinModule///PointingToDeclaration/", kexternRes.dri.toString())

                val kexternSupertypes = kexternRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("SimpleModule"),
                    kexternSupertypes
                )
                assertEquals(Language.KOTLIN, kexternRes.sources.values.single().language)
                assertEquals("com.fasterxml.jackson.module.kotlin", kexternRes.sources.values.single().path)
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
                assertEquals("kotlinx.coroutines/Job///PointingToDeclaration/", res?.dri?.toString())

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
                assertEquals("kotlin.collections/Map.Entry///PointingToDeclaration/", res?.dri?.toString())
            }
        }
    }
}