package content.properties

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContentForClassWithParamsAndPropertiesTest : BaseAbstractTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `should work for a simple property`() {
        propertyTest { rootPage ->
            val node = rootPage.dfs { it.name == "LoadInitialParams" } as ClasslikePageNode
            val actualDocsForPlaceholdersEnabled =
                (node.documentables.firstOrNull() as DClass).constructors.first().parameters.find { it.name == "placeholdersEnabled" }
                    ?.documentation?.entries?.first()?.value
            assertEquals(DocumentationNode(listOf(docsForPlaceholdersEnabled)), actualDocsForPlaceholdersEnabled)
        }
    }

    @Test
    fun `should work for a simple with linebreak`() {
        propertyTest { rootPage ->
            val node = rootPage.dfs { it.name == "LoadInitialParams" } as ClasslikePageNode
            val actualDocsForRequestedLoadSize =
                (node.documentables.firstOrNull() as DClass).constructors.first().parameters.find { it.name == "requestedLoadSize" }
                    ?.documentation?.entries?.first()?.value
            assertEquals(DocumentationNode(listOf(docsForRequestedLoadSize)), actualDocsForRequestedLoadSize)
        }
    }

    @Test
    fun `should work with multiline property inline code`() {
        propertyTest { rootPage ->
            val node = rootPage.dfs { it.name == "LoadInitialParams" } as ClasslikePageNode

            val actualDocsForRequestedInitialKey =
                (node.documentables.firstOrNull() as DClass).constructors.first().parameters.find { it.name == "requestedInitialKey" }
                    ?.documentation?.entries?.first()?.value
            assertEquals(DocumentationNode(listOf(docsForRequestedInitialKey)), actualDocsForRequestedInitialKey)
        }
    }

    @Test
    fun `constructor should only the param and constructor tags`() {
        propertyTest { rootPage ->
            val constructorDocs = Description(
                root = CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(
                                Text("Creates an empty group.")
                            )
                        )
                    ),
                    emptyMap(), "MARKDOWN_FILE"
                )
            )
            val node = rootPage.dfs { it.name == "LoadInitialParams" } as ClasslikePageNode

            val actualDocs =
                (node.documentables.firstOrNull() as DClass).constructors.first().documentation.entries.first().value
            assertEquals(DocumentationNode(listOf(constructorDocs, docsForParam)), actualDocs)
        }
    }

    @Test
    fun `class should have all tags`() {
        propertyTest { rootPage ->
            val ownDescription = Description(
                root = CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(
                                Text("Holder object for inputs to loadInitial.")
                            )
                        )
                    ),
                    emptyMap(), "MARKDOWN_FILE"
                )
            )
            val node = rootPage.dfs { it.name == "LoadInitialParams" } as ClasslikePageNode

            val actualDocs =
                (node.documentables.firstOrNull() as DClass).documentation.entries.first().value
            assertEquals(
                DocumentationNode(
                    listOf(
                        ownDescription,
                        docsForParam,
                        docsForRequestedInitialKey,
                        docsForRequestedLoadSize,
                        docsForPlaceholdersEnabled,
                        docsForConstructor
                    )
                ),
                actualDocs
            )
        }
    }

    @Test
    fun `property should also work with own docs that override the param tag`() {
        propertyTest { rootPage ->
            val ownDescription = Description(
                root = CustomDocTag(
                    children = listOf(
                        P(
                            children = listOf(
                                Text("Own docs")
                            )
                        )
                    ),
                    emptyMap(), "MARKDOWN_FILE"
                )
            )
            val node = rootPage.dfs { it.name == "ItemKeyedDataSource" } as ClasslikePageNode

            val actualDocs =
                (node.documentables.firstOrNull() as DClass).properties.first().documentation.entries.first().value
            assertEquals(
                DocumentationNode(listOf(ownDescription)),
                actualDocs
            )
        }
    }


    private fun propertyTest(block: (RootPageNode) -> Unit) {
        testInline(
            """ |/src/main/kotlin/test/source.kt
                |package test
                |/**
                | * @property tested Docs from class
                | */
                |abstract class ItemKeyedDataSource<Key : Any, Value : Any> : DataSource<Key, Value>(ITEM_KEYED) {
                |    /** 
                |     * Own docs 
                |     */
                |    val tested = ""
                |
                |    /**
                |     * Holder object for inputs to loadInitial.
                |     *
                |     * @param Key Type of data used to query Value types out of the DataSource.
                |     * @property requestedInitialKey Load items around this key, or at the beginning of the data set
                |     * if `null` is passed.
                |     *
                |     * Note that this key is generally a hint, and may be ignored if you want to always load from
                |     * the beginning.
                |     * @property requestedLoadSize Requested number of items to load.
                |     *
                |     * Note that this may be larger than available data.
                |     * @property placeholdersEnabled Defines whether placeholders are enabled, and whether the
                |     * loaded total count will be ignored.
                |     * 
                |     * @constructor Creates an empty group.
                |     */
                |    open class LoadInitialParams<Key : Any>(
                |        @JvmField
                |        val requestedInitialKey: Key?,
                |        @JvmField
                |        val requestedLoadSize: Int,
                |        @JvmField
                |        val placeholdersEnabled: Boolean
                |    )
                |}""".trimIndent(), testConfiguration
        ) {
            pagesGenerationStage = block
        }
    }

    private val docsForPlaceholdersEnabled = Property(
        root = CustomDocTag(
            listOf(
                P(
                    children = listOf(
                        Text("Defines whether placeholders are enabled, and whether the loaded total count will be ignored.")
                    )
                )
            ), emptyMap(), "MARKDOWN_FILE"
        ),
        name = "placeholdersEnabled"
    )

    private val docsForRequestedInitialKey = Property(
        root = CustomDocTag(
            listOf(
                P(
                    children = listOf(
                        Text("Load items around this key, or at the beginning of the data set if "),
                        CodeInline(
                            listOf(
                                Text("null")
                            )
                        ),
                        Text(" is passed.")
                    ),
                    params = emptyMap()
                ),
                P(
                    children = listOf(
                        Text("Note that this key is generally a hint, and may be ignored if you want to always load from the beginning.")
                    )
                )
            ), emptyMap(), "MARKDOWN_FILE"
        ),
        name = "requestedInitialKey"
    )

    private val docsForRequestedLoadSize = Property(
        root = CustomDocTag(
            listOf(
                P(
                    children = listOf(
                        Text("Requested number of items to load.")
                    )
                ),
                P(
                    children = listOf(
                        Text("Note that this may be larger than available data.")
                    )
                )
            ), emptyMap(), "MARKDOWN_FILE"
        ),
        name = "requestedLoadSize"
    )

    private val docsForConstructor = Constructor(
        root = CustomDocTag(
            children = listOf(
                P(
                    children = listOf(
                        Text("Creates an empty group.")
                    )
                )
            ),
            emptyMap(), "MARKDOWN_FILE"
        )
    )

    private val docsForParam = Param(
        root = CustomDocTag(
            children = listOf(
                P(
                    children = listOf(
                        Text("Type of data used to query Value types out of the DataSource.")
                    )
                )
            ),
            emptyMap(), "MARKDOWN_FILE"
        ),
        name = "Key"
    )
}

