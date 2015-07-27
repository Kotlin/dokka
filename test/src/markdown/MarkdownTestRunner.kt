package org.jetbrains.kmark.test

import org.junit.runner.*
import org.junit.runner.notification.*
import java.io.File
import org.junit.runners.ParentRunner
import java.io.Serializable
import kotlin.properties.Delegates
import org.junit.ComparisonFailure

data class MarkdownTestUniqueId(val id: Int) : Serializable {
    companion object {
        var id = 0
        fun next() = MarkdownTestUniqueId(id++)
    }
}

public open class MarkdownSpecification(val path: String, val processor: (String) -> String)


interface MarkdownTest {
    fun description(): Description
}

public open class MarkdownTestCase(val spec: MarkdownSpecification, val input: String, val expected: String) : MarkdownTest, Runner() {
    val _description by lazy {
        Description.createSuiteDescription(input, MarkdownTestUniqueId.next())!!
    }

    override fun description(): Description = _description

    override fun getDescription(): Description? = description()
    override fun run(notifier: RunNotifier?) {
        notifier!!

        notifier.fireTestStarted(_description)
        val result = spec.processor(input)
        when (result) {
            expected -> notifier.fireTestFinished(_description)
            else -> notifier.fireTestFailure(Failure(_description, ComparisonFailure("Output mismatch", expected, result)))
        }
    }
}

public open class MarkdownTestSection(val spec: MarkdownSpecification, val title: String) : MarkdownTest, ParentRunner<MarkdownTest>(spec.javaClass) {
    val children = arrayListOf<MarkdownTest>();

    val _description by lazy {
        val desc = Description.createSuiteDescription(title, MarkdownTestUniqueId.next())!!
        for (item in getChildren()!!) {
            desc.addChild(describeChild(item))
        }
        desc
    }

    override fun description(): Description = _description

    override fun getChildren(): MutableList<MarkdownTest>? = children

    override fun describeChild(child: MarkdownTest?): Description? = child!!.description()

    override fun runChild(child: MarkdownTest?, notifier: RunNotifier?) {
        notifier!!
        when (child) {
            is MarkdownTestCase -> child.run(notifier)
            is MarkdownTestSection -> {
                if (child.children.size() == 0) {
                    notifier.fireTestStarted(child.description())
                    notifier.fireTestFinished(child.description())
                } else {
                    child.run(notifier)
                }
            }
        }
    }
}

public class MarkdownTestRunner(specificationClass: Class<MarkdownSpecification>) : MarkdownTestSection(specificationClass.newInstance(), "Tests") {
    init {
        val lines = File(spec.path).readLines()
        createSections(this, lines, 1)
    }

    private fun createTests(parent: MarkdownTestSection, lines: List<String>): Int {
        val testMark = lines.takeWhile { it.trim() != "." }
        val testHtml = lines.drop(testMark.size()).drop(1).takeWhile { it.trim() != "." }
        val markdown = testMark.join("\n", postfix = "\n", prefix = "\n")
        val html = testHtml.join("\n", postfix = "\n")
        val markdownTestCase = MarkdownTestCase(spec, markdown, html)
        parent.children.add(markdownTestCase)
        return testMark.size() + testHtml.size() + 3
    }

    private fun createSections(parent: MarkdownTestSection, lines: List<String>, level: Int): Int {
        var sectionNumber = 1
        var index = 0
        while (index < lines.size()) {
            val line = lines[index]

            if (line.trim() == ".") {
                index = createTests(parent, lines.subList(index + 1, lines.lastIndex)) + index + 1
                continue
            }

            val head = line.takeWhile { it == '#' }.length()
            if (head == 0) {
                index++
                continue
            }

            if (head < level) {
                return index
            }

            if (head == level) {
                val title = lines[index].dropWhile { it == '#' }.dropWhile { it.isWhitespace() }
                sectionNumber++
                val section = MarkdownTestSection(spec, title)
                val lastIndex = createSections(section, lines.subList(index + 1, lines.lastIndex), level + 1) + index + 1
                if (section.children.size() > 0)
                    parent.children.add(section)
                val nextHead = lines[lastIndex].takeWhile { it == '#' }.length()
                if (nextHead < level) {
                    return lastIndex
                }
                index = lastIndex
                continue
            }
            index++
        }
        return lines.size()
    }
}