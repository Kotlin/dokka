package utilities

import org.jetbrains.dokka.utilities.serializeAsCompactJson
import org.jetbrains.dokka.utilities.serializeAsPrettyJson
import kotlin.test.assertEquals
import kotlin.test.Test

class JsonTest {

    @Test
    fun `should serialize an object as compact json`() {
        val testObject = SimpleTestDataClass(
            someString = "Foo",
            someInt = 42,
            someDouble = 42.0
        )

        val actual = serializeAsCompactJson(testObject)
        val expected = "{\"someString\":\"Foo\",\"someInt\":42,\"someIntWithDefaultValue\":42,\"someDouble\":42.0}"

        assertEquals(expected, actual)
    }

    @Test
    fun `should serialize an object as pretty json`() {
        val testObject = SimpleTestDataClass(
            someString = "Foo",
            someInt = 42,
            someDouble = 42.0
        )

        val actual = serializeAsPrettyJson(testObject)
        val expected = """
            {
              "someString" : "Foo",
              "someInt" : 42,
              "someIntWithDefaultValue" : 42,
              "someDouble" : 42.0
            }
        """.trimIndent()

        assertEquals(expected, actual)
    }
}

data class SimpleTestDataClass(
    val someString: String,
    val someInt: Int,
    val someIntWithDefaultValue: Int = 42,
    val someDouble: Double
)
