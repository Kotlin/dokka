package packageList

import org.jetbrains.dokka.base.renderers.PackageListService
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackageListTest {
    @Test
    fun `one module package list is created correctly`() {
        val nonStandardLocations = mapOf("//longArrayWithFun/#kotlin.Int#kotlin.Function1[kotlin.Int,kotlin.Long]/PointingToDeclaration/" to "[JS root]/long-array-with-fun.html")
        val modules = mapOf("" to setOf("foo", "bar", "baz"))
        val format = RecognizedLinkFormat.DokkaHtml
        val output = PackageListService.renderPackageList(nonStandardLocations, modules, format.formatName, format.linkExtension)
        val expected = """
            |${'$'}dokka.format:html-v1
            |${'$'}dokka.linkExtension:html
            |${'$'}dokka.location://longArrayWithFun/#kotlin.Int#kotlin.Function1[kotlin.Int,kotlin.Long]/PointingToDeclaration/[JS root]/long-array-with-fun.html
            |bar
            |baz
            |foo
            |""".trimMargin()
        assertEquals(expected, output)
    }

    @Test
    fun `multi-module package list is created correctly`() {
        val nonStandardLocations = mapOf("//longArrayWithFun/#kotlin.Int#kotlin.Function1[kotlin.Int,kotlin.Long]/PointingToDeclaration/" to "[JS root]/long-array-with-fun.html")
        val modules = mapOf("moduleA" to setOf("foo", "bar"), "moduleB" to setOf("baz"), "moduleC" to setOf("qux"))
        val format = RecognizedLinkFormat.DokkaHtml
        val output = PackageListService.renderPackageList(nonStandardLocations, modules, format.formatName, format.linkExtension)
        val expected = """
            |${'$'}dokka.format:html-v1
            |${'$'}dokka.linkExtension:html
            |${'$'}dokka.location://longArrayWithFun/#kotlin.Int#kotlin.Function1[kotlin.Int,kotlin.Long]/PointingToDeclaration/[JS root]/long-array-with-fun.html
            |module:moduleA
            |bar
            |foo
            |module:moduleB
            |baz
            |module:moduleC
            |qux
            |""".trimMargin()
        assertEquals(expected, output)
    }

    @Test
    fun `empty package set in module`() {
        val nonStandardLocations = emptyMap<String, String>()
        val modules = mapOf("moduleA" to setOf("foo", "bar"), "moduleB" to emptySet(), "moduleC" to setOf("qux"))
        val format = RecognizedLinkFormat.DokkaHtml
        val output = PackageListService.renderPackageList(nonStandardLocations, modules, format.formatName, format.linkExtension)
        val expected = """
            |${'$'}dokka.format:html-v1
            |${'$'}dokka.linkExtension:html
            |
            |module:moduleA
            |bar
            |foo
            |module:moduleC
            |qux
            |""".trimMargin()
        assertEquals(expected, output)
    }
}
