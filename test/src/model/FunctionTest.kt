package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class FunctionTest {
    Test fun function() {
        verifyModel("test/data/functions/function.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("Function fn", doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun genericFunction() {
        verifyModel("test/data/functions/genericFunction.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("generic function", doc)

                with(details.single()) {
                    assertEquals("T", name)
                    assertEquals(DocumentationNodeKind.TypeParameter, kind)
                    assertEquals("", doc)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun functionWithParams() {
        verifyModel("test/data/functions/functionWithParams.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("function", name)
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("""Multiline
Function
Documentation"""
                             , doc)
                with(details.single()) {
                    assertEquals("x", name)
                    assertEquals(DocumentationNodeKind.Parameter, kind)
                    assertEquals("parameter", doc)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}