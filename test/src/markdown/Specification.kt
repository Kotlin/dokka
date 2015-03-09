package org.jetbrains.kmark.test

import org.junit.runner.*
import org.jetbrains.kmark.test.*
import org.jetbrains.dokka.*

//[RunWith(javaClass<MarkdownTestRunner>())]
class Specification : MarkdownSpecification("test/data/markdown/spec.txt", {
//    markdownToHtml(it.replace("→", "\t"))
    ""
})