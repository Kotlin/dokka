package javadoc

import javadoc.pages.JavadocModulePageNode
import org.junit.jupiter.api.Test

internal class JavadocModuleTemplateMapTest : AbstractJavadocTemplateMapTest() {

    @Test
    fun simpleKotlinExample() {
        testTemplateMapInline(
            """
            /src/source.kt
            package test
            class Test
            """.trimIndent(),
            config
        ) {
            val moduleContentMap = singlePageOfType<JavadocModulePageNode>().templateMap
            println("module")
        }
    }
}
