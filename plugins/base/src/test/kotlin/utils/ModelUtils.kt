package utils

import org.jetbrains.dokka.DokkaModuleConfigurationImpl
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaPlugin

abstract class AbstractModelTest(val path: String? = null, val pkg: String) : ModelDSL(), AssertDSL {

    fun inlineModelTest(
        query: String,
        platform: String = "jvm",
        prependPackage: Boolean = true,
        cleanupOutput: Boolean = true,
        pluginsOverrides: List<DokkaPlugin> = emptyList(),
        configuration: DokkaModuleConfigurationImpl? = null,
        block: DModule.() -> Unit
    ) {
        val testConfiguration = configuration ?: DokkaModuleConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = platform
                }
            }
        }
        val prepend = path.let { p -> p?.let { "|$it\n" } ?: "" } + if (prependPackage) "|package $pkg" else ""

        testInline(
            query = ("$prepend\n$query").trim().trimIndent(),
            configuration = testConfiguration,
            cleanupOutput = cleanupOutput,
            pluginOverrides = pluginsOverrides
        ) {
            documentablesTransformationStage = block
        }
    }
}
