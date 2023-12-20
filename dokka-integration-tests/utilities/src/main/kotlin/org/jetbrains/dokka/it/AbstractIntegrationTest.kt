/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import org.jsoup.Jsoup
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

public abstract class AbstractIntegrationTest {

    @field:TempDir
    public lateinit var tempFolder: File

    public val projectDir: File get() = File(tempFolder, "project")

    public fun File.allDescendentsWithExtension(extension: String): Sequence<File> =
        this.walkTopDown().filter { it.isFile && it.extension == extension }

    public fun File.allHtmlFiles(): Sequence<File> = allDescendentsWithExtension("html")

    public fun File.allGfmFiles(): Sequence<File> = allDescendentsWithExtension("md")

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

    protected fun assertNoUnresolvedLinks(file: File, exceptions: Set<String> = emptySet()) {
        val fileText = file.readText()
        val regex = Regex("""data-unresolved-link="\[(.+?(?=]"))""")
        val match = regex.findAll(fileText).map { it.groups[1]!!.value }

        assertTrue(
            match.filterNot { it in exceptions }.toList().isEmpty(),
            "Unexpected unresolved link in ${file.path}\n" + fileText
        )
    }

    protected fun assertNoHrefToMissingLocalFileOrDirectory(
        file: File, fileExtensions: Set<String> = setOf("html")
    ) {
        val fileText = file.readText()
        val html = Jsoup.parse(fileText)
        html.allElements.toList().forEach { element ->
            val href = element.attr("href")
            if (href.startsWith("https")) return@forEach
            if (href.startsWith("http")) return@forEach

            val hrefWithoutAnchors = if (href.contains("#")) {
                val hrefSplits = href.split("#")
                if (hrefSplits.count() != 2) return@forEach
                hrefSplits.first()
            } else href

            val targetFile = if (href.startsWith("file:/")) {
                File(URL(hrefWithoutAnchors).path)
            } else {
                File(file.parent, hrefWithoutAnchors)
            }

            if (targetFile.extension.isNotEmpty() && targetFile.extension !in fileExtensions) return@forEach

            if (targetFile.extension.isEmpty() || targetFile.extension == "html" && !href.startsWith("#")) {
                assertTrue(
                    targetFile.exists(),
                    "${file.relativeTo(projectDir).path}: href=\"$href\"\n" +
                            "file does not exist: ${targetFile.path}"
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

    protected fun assertNoEmptySpans(file: File) {
        val fileText = file.readText()
        assertFalse(
            fileText.contains(Regex("""<span>\s*</span>""")),
            "Unexpected empty <span></span> in file ${file.path}"
        )
    }

    protected fun assertNoUnsubstitutedTemplatesInHtml(file: File) {
        val parsedFile = Jsoup.parse(file, "UTF-8")
        assertTrue(
            parsedFile.select("dokka-template-command").isEmpty(),
            "Expected all templates to be substituted"
        )
    }

    /**
     * Asserts that [contentFiles] have no pages where content contains special visibility markers,
     * such as §INTERNAL§ for `internal`, §PROTECTED§ for `protected` and §PRIVATE§ for `private` modifiers
     *
     * This can be used to check whether actual documented code corresponds to configured documented visibility
     *
     * @param contentFiles any readable content file such as html/md/rst/etc
     */
    protected fun assertContentVisibility(
        contentFiles: List<File>,
        documentPublic: Boolean,
        documentProtected: Boolean,
        documentInternal: Boolean,
        documentPrivate: Boolean
    ) {
        val hasPublic = contentFiles.any { file -> "§PUBLIC§" in file.readText() }
        assertEquals(documentPublic, hasPublic, "Expected content visibility and file content do not match for public")

        val hasInternal = contentFiles.any { file -> "§INTERNAL§" in file.readText() }
        assertEquals(
            documentInternal,
            hasInternal,
            "Expected content visibility and file content do not match for internal"
        )

        val hasProtected = contentFiles.any { file -> "§PROTECTED§" in file.readText() }
        assertEquals(
            documentProtected,
            hasProtected,
            "Expected content visibility and file content do not match for protected"
        )

        val hasPrivate = contentFiles.any { file -> "§PRIVATE§" in file.readText() }
        assertEquals(
            documentPrivate,
            hasPrivate,
            "Expected content visibility and file content do not match for private"
        )
    }

    /**
     * Check that [outputFiles] contain specific file paths provided in [expectedFilePaths].
     * Can be used for checking whether expected folders/pages have been created.
     */
    protected fun assertContainsFilePaths(outputFiles: List<File>, expectedFilePaths: List<Regex>) {
        expectedFilePaths.forEach { pathRegex ->
            assertNotNull(
                outputFiles.any { it.absolutePath.contains(pathRegex) },
                "Expected to find a file with path regex $pathRegex, but found nothing"
            )
        }
    }
}
