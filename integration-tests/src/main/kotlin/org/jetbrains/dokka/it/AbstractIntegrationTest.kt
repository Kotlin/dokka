package org.jetbrains.dokka.it

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertFalse

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

    protected fun assertNoUnresolvedLInks(file: File) {
        val regex = Regex("[\"']#[\"']")
        val fileText = file.readText()
        assertFalse(
            fileText.contains(regex),
            "Unexpected unresolved link in ${file.path}\n" + fileText
        )
    }
}
