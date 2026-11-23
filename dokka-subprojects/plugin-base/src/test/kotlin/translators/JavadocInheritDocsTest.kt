/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Text
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class JavadocInheritDocsTest : BaseAbstractTest() {
    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/java")
            }
        }
    }

    @Test
    fun `work when whole description is inherited`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Superclass docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* {@inheritDoc}
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Superclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work when inherited part is inside description`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Superclass docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Superclass docs End of subclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work when inherited part is empty`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. End of subclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    @Ignore // This should be enabled when we have proper tag inheritance in javadoc parser
    fun `work when inherited part is empty in supertype but present in its supertype`() {
        testInline(
            """
            |/src/main/java/sample/SuperSuperclass.java
            |package sample;
            |/**
            |* Super super docs
            |*/
            |public class SuperSuperclass { }
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass extends SuperSuperClass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Super super docs End of subclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    //Original javadoc doesn't treat interfaces as valid candidates for inherit doc
    fun `work with interfaces`() {
        testInline(
            """
            |/src/main/java/sample/SuperInterface.java
            |package sample;
            |/**
            |* Super super docs
            |*/
            |public interface SuperInterface { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public interface Subclass extends SuperInterface { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. End of subclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }


    @Test
    fun `work with multiple supertypes`() {
        testInline(
            """
            |/src/main/java/sample/SuperInterface.java
            |package sample;
            |/**
            |* Super interface docs
            |*/
            |public interface SuperInterface { }
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* Super class docs
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* Subclass docs. {@inheritDoc} End of subclass docs
            |*/
            |public class Subclass extends Superclass implements SuperInterface { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Subclass docs. Super class docs End of subclass docs"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `work with methods`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |public class Superclass {
            |/**
            |* Sample super method
            |*
            |* @return super string
            |* @throws RuntimeException super throws
            |* @see java.lang.String super see
            |* @deprecated super deprecated
            |*/
            |public String test() {
            |    return "";
            |}
            |}
            |/src/main/java/sample/Subclass.java
            |package sample;
            |public class Subclass extends Superclass {
            |    /**
            |     * Sample sub method. {@inheritDoc}
            |     */
            |    @Override
            |    public String test() {
            |        return super.test();
            |    }
            |}
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val function = module.findFunction("sample", "Subclass", "test")
                val descriptionGot = function.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text("Sample sub method. Sample super method"))
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `works for inline dogtags`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            |* {@link Superclass} docs with {@code code}
            |*/
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            |* {@inheritDoc}
            |*/
            |public class Subclass extends Superclass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(
                                    DocumentationLink(
                                        dri = DRI(packageName = "sample", classNames = "Superclass"),
                                        children = listOf(Text("Superclass")),
                                    ),
                                    Text(" docs with "),
                                    CodeInline(
                                        children = listOf(Text("code")),
                                        params = mapOf("lang" to "java"),
                                    )
                                )
                            )
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `works for recursive inherited docs`() {
        testInline(
            """
            |/src/main/java/sample/GrandparentClass.java
            |package sample;
            |/**
            |* Grandparent docs
            |*/
            |public class GrandparentClass { }
            |
            |/src/main/java/sample/ParentClass.java
            |package sample;
            |/**
            |* Parent docs
            |* <p>
            |* {@inheritDoc}
            |*/
            |public class ParentClass extends GrandparentClass { }
            |
            |/src/main/java/sample/ChildClass.java
            |package sample;
            |/**
            |* Child docs
            |* <p>
            |* {@inheritDoc}
            |*/
            |public class ChildClass extends ParentClass { }
            """.trimIndent(), configuration
        ) {
            documentablesMergingStage = { module ->
                val childClass = module.findClasslike("sample", "ChildClass")
                val descriptionGot = childClass.documentation.values.first().children.first()
                val expectedDescription = Description(
                    CustomDocTag(
                        children = listOf(
                            P(children = listOf(Text("Child docs "))),
                            P(children = listOf(Text(" Parent docs "))),
                            P(children = listOf(Text(" Grandparent docs"))),
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }

    @Test
    fun `should inherit @since tag via inheritDoc on methods and classes`() {
        testInline(
            """
            |/src/main/java/sample/Superclass.java
            |package sample;
            |/**
            | * @since 1.0
            | */
            |public class Superclass { }
            |
            |/src/main/java/sample/Subclass.java
            |package sample;
            |/**
            | * @since {@inheritDoc}
            | */
            |public class Subclass extends Superclass { }
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val subclass = module.findClasslike("sample", "Subclass")
                val descriptionGot = subclass.documentation.values.first().children.first()
                val expectedDescription = Since(
                    CustomDocTag(
                        children = listOf(
                            P(children = listOf(Text("1.0")))
                        ),
                        name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )

                assertEquals(expectedDescription, descriptionGot)
            }
        }
    }
}
