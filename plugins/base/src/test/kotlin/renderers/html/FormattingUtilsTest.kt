package renderers.html

import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.buildBreakableText
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FormattingUtilsTest {
    @Test
    fun `should build breakable text`(){
        val testedText = "kotlinx.collections.immutable"
        val expectedHtml = """
            <html>
              <body><span>kotlinx.</span><wbr></wbr><span>collections.</span><wbr></wbr><span>immutable</span></body>
            </html>
        """.trimIndent()

        val html = createHTML(prettyPrint = true).html {
            body {
                buildBreakableText(testedText)
            }
        }

        assertEquals(expectedHtml.trim(), html.trim())
    }

    @Test
    fun `should build breakable text without empty spans`(){
        val testedText = "Package org.jetbrains.dokka.it.moduleC"
        val expectedHtml = """
            <html>
              <body><span><span>Package</span></span> <span>org.</span><wbr></wbr><span>jetbrains.</span><wbr></wbr><span>dokka.</span><wbr></wbr><span>it.</span><wbr></wbr><span>moduleC</span></body>
            </html>
        """.trimIndent()

        val html = createHTML(prettyPrint = true).html {
            body {
                buildBreakableText(testedText)
            }
        }

        assertEquals(expectedHtml.trim(), html.trim())
    }

    @Test
    fun `should build breakable text for text with braces`(){
        val testedText = "[Common]kotlinx.collections.immutable"
        val expectedHtml = """
            <html>
              <body><span>[Common]kotlinx.</span><wbr></wbr><span>collections.</span><wbr></wbr><span>immutable</span></body>
            </html>
        """.trimIndent()

        val html = createHTML(prettyPrint = true).html {
            body {
                buildBreakableText(testedText)
            }
        }

        assertEquals(expectedHtml.trim(), html.trim())
    }

    @Test
    fun `should build breakable text for camel case notation`(){
        val testedText = "DokkkkkkkaIsTheBest"
        val expectedHtml = """
            <html>
              <body><span>Dokkkkkkka</span><wbr></wbr><span>Is</span><wbr></wbr><span>The</span><wbr></wbr><span><span>Best</span></span></body>
            </html>
        """.trimIndent()

        val html = createHTML(prettyPrint = true).html {
            body {
                buildBreakableText(testedText)
            }
        }

        assertEquals(expectedHtml.trim(), html.trim())
    }
}