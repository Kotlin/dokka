package translators

import org.junit.jupiter.api.Assertions.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test

class Issue2310 : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun `verify that getters & setters have the correct names`() {
        testInline(
            """
            |/src/main/kotlin/sample/TestCase.kt
            |package sample
            |
            |data class TestCase(
            |   var standardString: String,
            |   var standardBoolean: Boolean,
            |   var issuesFetched: Int,
            |   var issuesWereDisplayed: Boolean,
            |   
            |   var isFoo: String,
            |   var isBar: Boolean,
            |   var is_underscoreA: String,
            |   var is_underscoreB: Boolean,
            |   var is1of: String,
            |   var is2of: Boolean,
            |
            |   var a: Boolean,
            |   var ab: Boolean,
            |   var abc: Boolean,
            |   var isA: Int,
            |   var isB: Boolean,
            |)
            """.trimIndent(),
            configuration
        ) {

            documentablesMergingStage = { module ->
                val properties = module.packages.single().classlikes.first().properties

                assertEquals(15, properties.size)

                // The type shouldn't matter, so we double check all names with both a boolean and non-boolean type

                // These should follow normal construction rules for properties
                properties.first { it.name == "standardString" }.let {
                    assertEquals("getStandardString", it.getter!!.name)
                    assertEquals("setStandardString", it.setter!!.name)
                }
                properties.first { it.name == "standardBoolean" }.let {
                    assertEquals("getStandardBoolean", it.getter!!.name)
                    assertEquals("setStandardBoolean", it.setter!!.name)
                }
                properties.first { it.name == "issuesFetched" }.let {
                    assertEquals("getIssuesFetched", it.getter!!.name)
                    assertEquals("setIssuesFetched", it.setter!!.name)
                }
                properties.first { it.name == "issuesWereDisplayed" }.let {
                    assertEquals("getIssuesWereDisplayed", it.getter!!.name)
                    assertEquals("setIssuesWereDisplayed", it.setter!!.name)
                }

                // These are special cases where kotlin keeps the "is"
                properties.first { it.name == "isFoo" }.let {
                    assertEquals("isFoo", it.getter!!.name)
                    assertEquals("setFoo", it.setter!!.name)
                }
                properties.first { it.name == "isBar" }.let {
                    assertEquals("isBar", it.getter!!.name)
                    assertEquals("setBar", it.setter!!.name)
                }
                properties.first { it.name == "is_underscoreA" }.let {
                    assertEquals("is_underscoreA", it.getter!!.name)
                    assertEquals("set_underscoreA", it.setter!!.name)
                }
                properties.first { it.name == "is_underscoreB" }.let {
                    assertEquals("is_underscoreB", it.getter!!.name)
                    assertEquals("set_underscoreB", it.setter!!.name)
                }
                properties.first { it.name == "is1of" }.let {
                    assertEquals("is1of", it.getter!!.name)
                    assertEquals("set1of", it.setter!!.name)
                }
                properties.first { it.name == "is2of" }.let {
                    assertEquals("is2of", it.getter!!.name)
                    assertEquals("set2of", it.setter!!.name)
                }

                // For sanity checking my indexing on short names
                properties.first { it.name == "a" }.let {
                    assertEquals("getA", it.getter!!.name)
                    assertEquals("setA", it.setter!!.name)
                }
                properties.first { it.name == "ab" }.let {
                    assertEquals("getAb", it.getter!!.name)
                    assertEquals("setAb", it.setter!!.name)
                }
                properties.first { it.name == "abc" }.let {
                    assertEquals("getAbc", it.getter!!.name)
                    assertEquals("setAbc", it.setter!!.name)
                }
                properties.first { it.name == "isA" }.let {
                    assertEquals("isA", it.getter!!.name)
                    assertEquals("setA", it.setter!!.name)
                }
                properties.first { it.name == "isB" }.let {
                    assertEquals("isB", it.getter!!.name)
                    assertEquals("setB", it.setter!!.name)
                }
            }
        }
    }
}