package renderers.html

import org.jetbrains.dokka.base.renderers.html.JsSerializer
import org.jetbrains.dokka.base.renderers.html.SearchRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties

internal class JsSerializerTest {
    val serializer = JsSerializer()
    val data = listOf(
        SearchRecord("name1", "description1", "location1", listOf("key1", "key2")),
        SearchRecord("name2", null, "location2", emptyList())
    )

    @Test
    fun `should serialize a list of Search Records`(){
        val expected = """
            [{ name: "name1", description: "description1", location: "location1", searchKeys: ["key1", "key2"]},{ name: "name2", description: null, location: "location2", searchKeys: []}]
        """.trimIndent()
        val got = serializer.serialize(data)
        assertEquals(expected, got)
    }

    @Test
    fun `should have all fields in serialized format`(){
        val expectedFields = SearchRecord::class.declaredMemberProperties.map { it.name }.sorted()
        val serialized = serializer.serialize(data.first())
        val gotFields = serialized.split(":").flatMap { it.split(" ") }.filter { it !in setOf("", "{", "}") && !it.contains("\"") }
        assertEquals(expectedFields, gotFields.sorted())
    }
}