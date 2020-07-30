package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.javadoc.pages.JavadocPageNode
import org.jetbrains.dokka.javadoc.renderer.JavadocContentToTemplateMapTranslator
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.javadoc.location.JavadocLocationProvider
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest

internal abstract class AbstractJavadocTemplateMapTest : AbstractCoreTest() {
    protected var config: DokkaConfigurationImpl = dokkaConfiguration {
        format = "javadoc"
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "jvm"
                externalDocumentationLinks = listOf(
                    ExternalDocumentationLink("https://docs.oracle.com/javase/8/docs/api/"),
                    ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
                )
            }
        }
    }

    data class Result(
        val rootPageNode: RootPageNode,
        val context: DokkaContext
    ) {

        val translator: JavadocContentToTemplateMapTranslator by lazy {
            val locationProvider = context.plugin<JavadocPlugin>()
                .querySingle { locationProviderFactory }
                .getLocationProvider(rootPageNode) as JavadocLocationProvider

            JavadocContentToTemplateMapTranslator(locationProvider, context)
        }

        val JavadocPageNode.templateMap: Map<String, Any?> get() = translator.templateMapForPageNode(this)

        inline fun <reified T : JavadocPageNode> allPagesOfType(): List<T> {
            return rootPageNode.withDescendants().filterIsInstance<T>().toList()
        }

        inline fun <reified T : JavadocPageNode> firstPageOfType(): T {
            return rootPageNode.withDescendants().filterIsInstance<T>().first()
        }

        inline fun <reified T : JavadocPageNode> firstPageOfTypeOrNull(): T? {
            return rootPageNode.withDescendants().filterIsInstance<T>().firstOrNull()
        }

        inline fun <reified T : JavadocPageNode> singlePageOfType(): T {
            return rootPageNode.withDescendants().filterIsInstance<T>().single()
        }
    }

    fun testTemplateMapInline(
        query: String,
        configuration: DokkaConfigurationImpl = config,
        pluginsOverride: List<DokkaPlugin> = emptyList(),
        assertions: Result.() -> Unit
    ) {
        testInline(query, configuration, pluginOverrides = pluginsOverride) {
            renderingStage = { rootPageNode, dokkaContext ->
                val preprocessors = dokkaContext.plugin<JavadocPlugin>().query { javadocPreprocessors }
                val transformedRootPageNode = preprocessors.fold(rootPageNode) { acc, pageTransformer ->
                    pageTransformer(acc)
                }

                Result(transformedRootPageNode, dokkaContext).assertions()
            }
        }
    }

    fun dualTestTemplateMapInline(
        kotlin: String? = null,
        java: String? = null,
        configuration: DokkaConfigurationImpl = config,
        pluginsOverride: List<DokkaPlugin> = emptyList(),
        assertions: Result.() -> Unit
    ) {
        val kotlinException = kotlin?.let {
            runCatching {
                testTemplateMapInline(
                    query = kotlin,
                    configuration = configuration,
                    pluginsOverride = pluginsOverride,
                    assertions = assertions
                )
            }.exceptionOrNull()
        }

        val javaException = java?.let {
            runCatching {
                testTemplateMapInline(
                    query = java,
                    configuration = configuration,
                    pluginsOverride = pluginsOverride,
                    assertions = assertions
                )
            }.exceptionOrNull()
        }

        if (kotlinException != null && javaException != null) {
            throw AssertionError(
                "Kotlin and Java Code failed assertions\n" +
                        "Kotlin: ${kotlinException.message}\n" +
                        "Java  : ${javaException.message}",
                kotlinException
            )
        }

        if (kotlinException != null) {
            throw AssertionError("Kotlin Code failed assertions", kotlinException)
        }

        if (javaException != null) {
            throw AssertionError("Java Code failed assertions", javaException)
        }
    }
}
