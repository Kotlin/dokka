package org.jetbrains.dokka.it

import org.jsoup.Jsoup
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
abstract class AbstractIntegrationTest {

    @get:Rule
    val temporaryTestFolder = TemporaryFolder()

    val projectDir get() = File(temporaryTestFolder.root, "project")

    fun File.allDescendentsWithExtension(extension: String): Sequence<File> {
        return this.walkTopDown().filter { it.isFile && it.extension == extension }
    }

    fun File.allHtmlFiles(): Sequence<File> {
        return allDescendentsWithExtension("html")
    }

    protected fun assertContainsNoErrorClass(file: File) {
        val fileText = file.readText()
        assertFalse(
            fileText.contains("ERROR CLASS", ignoreCase = true),
            "Unexpected `ERROR CLASS` in ${file.path}\n" + fileText
        )
    }

    protected fun assertNoEmptyLinks(file: File) {
        val regex = Regex("[\"']#[\"']")
        val fileText = file.readText()
        assertFalse(
            fileText.contains(regex),
            "Unexpected empty link in ${file.path}\n" + fileText
        )
    }

    protected fun assertNoUnresolvedLinks(file: File) {
        val regex = Regex("data-unresolved-link")
        val fileText = file.readText()
        assertFalse(
            fileText.contains(regex),
            "Unexpected unresolved link in ${file.path}\n" + fileText
        )
    }

    protected fun assertNoHrefToMissingLocalFileOrDirectory(
        file: File, fileExtensions: Set<String> = setOf("html")
    ) {
        val fileText = file.readText()
        val html = Jsoup.parse(fileText)
        html.allElements.toList().forEach { element ->
            val href = (element.attr("href") ?: return@forEach)
            if (href.startsWith("https")) return@forEach
            if (href.startsWith("http")) return@forEach

            val hrefWithoutAnchors = if (href.contains("#")) {
                val hrefSplits = href.split("#")
                if (hrefSplits.count() != 2) return@forEach
                hrefSplits.first()
            } else href

            val targetFile = File(file.parent, hrefWithoutAnchors)
            if (targetFile.extension.isNotEmpty() && targetFile.extension !in fileExtensions) return@forEach

            if (
                targetFile.extension.isEmpty() || targetFile.extension == "html" && !href.startsWith("#")) {
                assertTrue(
                    targetFile.exists(),
                    "${file.relativeTo(projectDir).path}: href=\"$href\"\n" +
                            "file does not exist: ${targetFile.relativeTo(projectDir).path}"
                )
            }
        }
    }

    protected fun assertNoSuppressedMarker(file: File) {
        val fileText = file.readText()
        assertFalse(
            fileText.contains("§SUPPRESSED§"),
            "Unexpected `§SUPPRESSED§` in file ${file.path}"
        )
    }
}
