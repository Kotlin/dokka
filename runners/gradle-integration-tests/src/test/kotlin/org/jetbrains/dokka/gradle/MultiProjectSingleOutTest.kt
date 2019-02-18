package org.jetbrains.dokka.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MultiProjectSingleOutTest : AbstractDokkaGradleTest() {

    fun prepareTestData(testDataRootPath: String) {
        val testDataRoot = testDataFolder.resolve(testDataRootPath)
        val tmpRoot = testProjectDir.root.toPath()

        testDataRoot.apply {
            resolve("build.gradle").copy(tmpRoot.resolve("build.gradle"))
            resolve("settings.gradle").copy(tmpRoot.resolve("settings.gradle"))
            resolve("subA").copy(tmpRoot.resolve("subA"))
            resolve("subB").copy(tmpRoot.resolve("subB"))
        }
    }

    @Before
    fun prepareData() = prepareTestData("multiProjectSingleOut")

    private fun buildDocs(gradleVersion: String, kotlinVersion: String, taskName: String = "dokka"): String {
        val result = configure(gradleVersion, kotlinVersion, arguments = arrayOf(taskName, "--stacktrace")).build()

        println(result.output)

        assertEquals(TaskOutcome.SUCCESS, result.task(":$taskName")?.outcome)

        return "build/$taskName"
    }

    private fun testHtml(gradleVersion: String, kotlinVersion: String) {
        val docsOutput = buildDocs(gradleVersion, kotlinVersion)

        checkOutputStructure("multiProjectSingleOut/fileTree.txt", docsOutput)

        checkNoErrorClasses(docsOutput)
        checkNoUnresolvedLinks(docsOutput)

        checkExternalLink(docsOutput, "<span class=\"identifier\">String</span>",
            """<a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html"><span class="identifier">String</span></a>""")

        checkExternalLink(docsOutput, ">s1.MyClass<",
            """<a href="../s1/-my-class/index.html">s1.MyClass</a>""")

        checkExternalLink(docsOutput, "s1.Super.foo",
            """<p>Overriden magic. Also reference to <a href="../../s1/-super/foo.html">s1.Super.foo</a></p>""")

    }

    private fun testMarkdown(gradleVersion: String, kotlinVersion: String) {
        val docsOutput = buildDocs(gradleVersion, kotlinVersion, "dokkaMd")

        checkOutputStructure("multiProjectSingleOut/fileTreeMarkdown.txt", docsOutput)

        checkNoErrorClasses(docsOutput, extension = "md")
        checkNoUnresolvedLinks(docsOutput, extension = "md")

        checkExternalLink(docsOutput, "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html",
            """[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)"""
            , extension = "md")

        checkExternalLink(docsOutput, "[s1.MyClass]",
            """[s1.MyClass](../s1/-my-class/index.md)""", extension = "md")

        checkExternalLink(docsOutput, "[s1.Super.foo]",
            """Overriden magic. Also reference to [s1.Super.foo](../../s1/-super/foo.md)""", extension = "md")
    }

    @Test fun `test kotlin 1_1_2 and gradle 3_5 html`() {
        testHtml("3.5", "1.1.2")
    }

    @Test fun `test kotlin 1_0_7 and gradle 2_14_1 html`() {
        testHtml("2.14.1", "1.0.7")
    }

    @Test fun `test kotlin 1_1_2 and gradle 4_0 html`() {
        testHtml("4.0", "1.1.2")
    }

    @Test fun `test kotlin 1_2_20 and gradle 4_5 html`() {
        testHtml("4.5", "1.2.20")
    }

    @Test fun `test kotlin 1_1_2 and gradle 3_5 markdown`() {
        testMarkdown("3.5", "1.1.2")
    }

    @Test fun `test kotlin 1_0_7 and gradle 2_14_1 markdown`() {
        testMarkdown("2.14.1", "1.0.7")
    }

    @Test fun `test kotlin 1_1_2 and gradle 4_0 markdown`() {
        testMarkdown("4.0", "1.1.2")
    }

    @Test fun `test kotlin 1_2_20 and gradle 4_5 markdown`() {
        testMarkdown("4.5", "1.2.20")
    }
}