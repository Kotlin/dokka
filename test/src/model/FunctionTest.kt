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
                assertEquals("Function fn", doc.summary)
                assertEquals("Unit", details.single().name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun functionWithReceiver() {
        verifyModel("test/data/functions/functionWithReceiver.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("Function with receiver", doc.summary)
                assertEquals("Unit", details.elementAt(0).name)

                assertEquals(2, details.count())
                with(details.elementAt(1)) {
                    assertEquals("<this>", name)
                    assertEquals(DocumentationNodeKind.Receiver, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertEquals("String", details.single().name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
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
                assertEquals("generic function", doc.summary)

                assertEquals(2, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                with(details.elementAt(1)) {
                    assertEquals("T", name)
                    assertEquals(DocumentationNodeKind.TypeParameter, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    assertTrue(details.none())
                    assertTrue(members.none())
                    assertTrue(links.none())
                }

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
    Test fun genericFunctionWithConstraints() {
        verifyModel("test/data/functions/genericFunctionWithConstraints.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("generic function", doc.summary)

                assertEquals(3, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                with(details.elementAt(1)) {
                    assertEquals("T", name)
                    assertEquals(DocumentationNodeKind.TypeParameter, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    with(details.single()) {
                        assertEquals("R", name)
                        assertEquals(DocumentationNodeKind.UpperBound, kind)
                        assertEquals(DocumentationContent.Empty, doc)
                        assertTrue(details.none())
                        assertTrue(members.none())
                        assertTrue(links.none())
                    }
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(details.elementAt(2)) {
                    assertEquals("R", name)
                    assertEquals(DocumentationNodeKind.TypeParameter, kind)
                    assertEquals(DocumentationContent.Empty, doc)
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
                             , doc.summary)

                assertEquals(2, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                with(details.elementAt(1)) {
                    assertEquals("x", name)
                    assertEquals(DocumentationNodeKind.Parameter, kind)
                    assertEquals("parameter", doc.summary)
                    assertEquals("Int", details.single().name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                }

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}