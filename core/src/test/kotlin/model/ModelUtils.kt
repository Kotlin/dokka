package model

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.model.doc.DocumentationNode
import testApi.testRunner.AbstractCoreTest

abstract class AbstractModelTest : AbstractCoreTest() {

    fun inlineModelTest(
        query: String,
        platform: String = "jvm",
        targetList: List<String> = listOf("jvm"),
        block: Module.() -> Unit
    ) {
        val configuration = dokkaConfiguration {
            passes {
                pass {
                    sourceRoots = listOf("src/")
                    analysisPlatform = platform
                    targets = targetList
                }
            }
        }

        testInline(query.trimIndent(), configuration) {
            documentablesTransformationStage = block
        }
    }

    operator fun List<Documentable?>?.div(name: String): Documentable? = this?.let { d -> d.find { it?.name == name } }

    operator fun Documentable?.div(name: String): Documentable? = this?.children / name
    fun <T> T?.assertNotNull(name: String): T = this.also { assert(this != null) { "$name should not be null" } }
        .let { it!! }

    fun <T> Any?.assertIsInstance(name: String): T =
        this.let { it as? T }.also { assert(this != null) { "$name should not be null" } }.let { it!! }

    fun List<DocumentationNode>.commentsToString(): String =
        this.flatMap { it.children }.joinToString(separator = "\n") { it.root.docTagSummary() }

    fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    fun <T> T?.assertEqual(expected: T, prefix: String = "") = assert(this == expected) {
        "${prefix}Expected $expected, got $this"
    }

}
