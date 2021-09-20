package content.seealso

import matchers.content.*
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import utils.*
import kotlin.test.assertEquals

class ContentForSeeAlsoTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `undocumented function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"abc" }
                                                group { }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented seealso`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"abc" }
                                                group {
                                                    group { +"Comment to abc" }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                link {
                                                    check {
                                                        assertEquals("kotlin.collections/Collection///PointingToDeclaration/", (this as ContentDRILink).address.toString())
                                                    }
                                                    +"kotlin.collections.Collection"
                                                }
                                                group { }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented seealso with stdlib link`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see Collection Comment to stdliblink
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"kotlin.collections.Collection" }
                                                group {
                                                    group { +"Comment to stdliblink" }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented seealso with stdlib link with other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * random comment
            |  * @see Collection Comment to stdliblink
            |  * @author pikinier20
            |  * @since 0.11
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { comment { +"random comment"} }
                                unnamedTag("Author") { comment { +"pikinier20" } }
                                unnamedTag("Since") { comment { +"0.11" } }

                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"kotlin.collections.Collection" }
                                                group {
                                                    group { +"Comment to stdliblink" }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented multiple see also`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see abc Comment to abc2
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"abc" }
                                                group {
                                                    group { +"Comment to abc2" }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `documented multiple see also mixed source`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @see abc Comment to abc1
            |  * @see[Collection] Comment to collection
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                header(2) { +"See also" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"abc" }
                                                group {
                                                    group { +"Comment to abc1" }
                                                }
                                            }
                                            group {
                                                //DRI should be "test//abc/#/-1/"
                                                link { +"kotlin.collections.Collection" }
                                                group { group {  +"Comment to collection" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented see also from java`(){
        testInline(
            """
            |/src/main/java/example/Source.java
            |package example;
            |
            |public interface Source {
            |   String getProperty(String k, String v);
            |
            |   /**
            |    * @see #getProperty(String, String)
            |    */
            |   String getProperty(String k);
            |}
        """.trimIndent(), testConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val functionWithSeeTag = module.packages.flatMap { it.classlikes }.flatMap { it.functions }.find { it.name == "getProperty" && it.parameters.count() == 1 }
                val seeTag = functionWithSeeTag?.docs()?.firstIsInstanceOrNull<See>()
                val expectedLinkDestinationDRI = DRI(
                    packageName = "example",
                    classNames = "Source",
                    callable = Callable(
                        name = "getProperty",
                        params = listOf(JavaClassReference("java.lang.String"), JavaClassReference("java.lang.String"))
                    )
                )

                kotlin.test.assertNotNull(seeTag)
                assertEquals("getProperty(String, String)", seeTag.name)
                assertEquals(expectedLinkDestinationDRI, seeTag.address)
                assertEquals(emptyList(), seeTag.children)
            }
        }
    }

    @Test
    fun `documented see also from java`(){
        testInline(
            """
            |/src/main/java/example/Source.java
            |package example;
            |
            |public interface Source {
            |   String getProperty(String k, String v);
            |
            |   /**
            |    * @see #getProperty(String, String) this is a reference to a method that is present on the same class.
            |    */
            |   String getProperty(String k);
            |}
        """.trimIndent(), testConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val functionWithSeeTag = module.packages.flatMap { it.classlikes }.flatMap { it.functions }.find { it.name == "getProperty" && it.parameters.size == 1 }
                val seeTag = functionWithSeeTag?.docs()?.firstIsInstanceOrNull<See>()
                val expectedLinkDestinationDRI = DRI(
                    packageName = "example",
                    classNames = "Source",
                    callable = Callable(
                        name = "getProperty",
                        params = listOf(JavaClassReference("java.lang.String"), JavaClassReference("java.lang.String"))
                    )
                )

                kotlin.test.assertNotNull(seeTag)
                assertEquals("getProperty(String, String)", seeTag.name)
                assertEquals(expectedLinkDestinationDRI, seeTag.address)
                assertEquals("this is a reference to a method that is present on the same class.", seeTag.children.first().text().trim())
                assertEquals(1, seeTag.children.size)
            }
        }
    }
}
