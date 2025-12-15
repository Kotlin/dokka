/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package parsers.javadoc

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.doc.Pre
import org.jetbrains.dokka.model.doc.Text
import utils.OnlySymbols
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class SnippetTest : BaseAbstractTest() {
    private val testDataDir = getTestDataDir("parsers/javadoc").toAbsolutePath()

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    private val snippetPathConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                samples = listOf(
                    Paths.get("$testDataDir/snippets").toString(),
                )
            }
        }
    }

    @Test
    fun `plain inline snippet`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example
            |
            | /**
            | * {@snippet :
            | * private void snippet() {
            | *     Test test = new Test();
            | *     System.out.println("test");
            | * }}
            | */
            | public class Test {}
            """.trimIndent()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |private void snippet() {
                                    |    Test test = new Test();
                                    |    System.out.println("test");
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files using file attribute`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="MySnippet.java"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/MySnippet.java
            |private void exampleMethod() {
            |    String message = "Hello from external snippet";
            |    System.out.println(message);
            |}
            """.trimMargin()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |private void exampleMethod() {
                                    |    String message = "Hello from external snippet";
                                    |    System.out.println(message);
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files using class attribute`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="CalculatorSnippet"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/CalculatorSnippet.java
            |public class CalculatorSnippet {
            |    public int add(int a, int b) {
            |        return a + b;
            |    }
            |}
            """.trimMargin()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |public class CalculatorSnippet {
                                    |    public int add(int a, int b) {
                                    |        return a + b;
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files with region using file attribute and @end with region name`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="RegionExample.java" region="calculation"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/RegionExample.java
            |public class RegionExample {
            |    // @start region="calculation"
            |    int result = multiply(5, 3);
            |    // @end region="calculation"
            |
            |    private int multiply(int x, int y) {
            |        return x * y;
            |    }
            |}
            """.trimMargin()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |int result = multiply(5, 3);
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files with region using class attribute and anonymous @end`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="AnonymousRegionExample" region="process"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/AnonymousRegionExample.java
            |public class AnonymousRegionExample {
            |    // @start region="process"
            |    String data = "test";
            |    data = data.toUpperCase();
            |    // @end
            |}
            """.trimMargin()
        testInline(
            source,
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |String data = "test";
                                    |data = data.toUpperCase();
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @OnlySymbols // TODO check this, failing for descriptors
    @Test
    fun `external snippet from snippet-path using file attribute`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="WholeFileSnippet.java"}
            | */
            | public class Test {}
            """.trimMargin()
        testInline(
            source,
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |/*
                                    | * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
                                    | */
                                    |
                                    |public class WholeFileSnippet {
                                    |    public static void processData() {
                                    |        List<String> items = new ArrayList<>();
                                    |        items.add("item1");
                                    |        items.add("item2");
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @OnlySymbols // TODO check this, failing for descriptors
    @Test
    fun `external snippet from snippet-path using class attribute`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="WholeFileSnippet"}
            | */
            | public class Test {}
            """.trimMargin()
        testInline(
            source,
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |/*
                                    | * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
                                    | */
                                    |
                                    |public class WholeFileSnippet {
                                    |    public static void processData() {
                                    |        List<String> items = new ArrayList<>();
                                    |        items.add("item1");
                                    |        items.add("item2");
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @OnlySymbols // TODO check this, failing for descriptors
    @Test
    fun `external snippet from snippet-path with region using file attribute and @end with region name`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="ExternalSnippets.java" region="configSetup"}
            | */
            | public class Test {}
            """.trimMargin()
        testInline(
            source,
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |Config config = new Config();
                                    |config.setEnabled(true);
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @OnlySymbols // TODO check this, failing for descriptors
    @Test
    fun `external snippet from snippet-path with region using class attribute and anonymous @end`() {
        val source = """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="ExternalSnippets" region="validation"}
            | */
            | public class Test {}
            """.trimMargin()
        testInline(
            source,
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val docs = modules.first().packages.first().classlikes.single().documentation.values.first()
                val root = docs.children.first().root

                assertEquals(
                    listOf(
                        Pre(
                            children = listOf(
                                Text(
                                    """
                                    |if (value != null && value.length() > 0) {
                                    |    return true;
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    // TODO add tests for markup, hybrid snippets, lang attribute
}
