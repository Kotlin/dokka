package Model

import org.jetbrains.dokka.Model.CodeNode
import org.junit.Test
import kotlin.test.assertEquals

class CodeNodeTest {

    @Test fun text_normalisesInitialWhitespace() {
        val expected = "Expected\ntext in this\ttest"
        val sut = CodeNode("\n \t \r $expected", "")
        assertEquals(expected, sut.text())
    }
}