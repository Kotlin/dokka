package utils

import org.jetbrains.dokka.model.DModule

abstract class AbstractModelTest(val path: String? = null, val pkg: String) : ModelDSL(), AssertDSL {

    fun inlineModelTest(
        query: String,
        platform: String = "jvm",
        targetList: List<String> = listOf("jvm"),
        prependPackage: Boolean = true,
        block: DModule.() -> Unit
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
        val prepend = path.let { p -> p?.let { "|$it\n" } ?: "" } + if(prependPackage) "|package $pkg" else ""

        testInline(("$prepend\n$query").trim().trimIndent(), configuration) {
            documentablesTransformationStage = block
        }
    }


}
