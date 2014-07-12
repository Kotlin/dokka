package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class FunctionTest {
    Test fun function() {
        verifyModel("test/data/functions/function.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("Function fn", doc.summary)
                assertEquals("Unit", detail(DocumentationNode.Kind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun functionWithReceiver() {
        verifyModel("test/data/functions/functionWithReceiver.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("Function with receiver", doc.summary)
                assertEquals("Unit", details.elementAt(0).name)

                assertEquals(4, details.count())
                assertEquals("final", details.elementAt(1).name)
                assertEquals("internal", details.elementAt(2).name)
                with(details.elementAt(3)) {
                    assertEquals("<this>", name)
                    assertEquals(DocumentationNode.Kind.Receiver, kind)
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
            with(model.members.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("generic function", doc.summary)

                assertEquals(4, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                assertEquals("final", details.elementAt(1).name)
                assertEquals("private", details.elementAt(2).name)
                with(details.elementAt(3)) {
                    assertEquals("T", name)
                    assertEquals(DocumentationNode.Kind.TypeParameter, kind)
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
            with(model.members.single().members.single()) {
                assertEquals("generic", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("generic function", doc.summary)

                assertEquals(5, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                assertEquals("final", details.elementAt(1).name)
                assertEquals("public", details.elementAt(2).name)
                with(details.elementAt(3)) {
                    assertEquals("T", name)
                    assertEquals(DocumentationNode.Kind.TypeParameter, kind)
                    assertEquals(DocumentationContent.Empty, doc)
                    with(details.single()) {
                        assertEquals("R", name)
                        assertEquals(DocumentationNode.Kind.UpperBound, kind)
                        assertEquals(DocumentationContent.Empty, doc)
                        assertTrue(details.none())
                        assertTrue(members.none())
                        assertTrue(links.none())
                    }
                    assertTrue(members.none())
                    assertTrue(links.none())
                }
                with(details.elementAt(4)) {
                    assertEquals("R", name)
                    assertEquals(DocumentationNode.Kind.TypeParameter, kind)
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
            with(model.members.single().members.single()) {
                assertEquals("function", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("""Multiline
Function
Documentation"""
                             , doc.summary)

                assertEquals(4, details.count())
                assertEquals("Unit", details.elementAt(0).name)
                assertEquals("final", details.elementAt(1).name)
                assertEquals("internal", details.elementAt(2).name)
                with(details.elementAt(3)) {
                    assertEquals("x", name)
                    assertEquals(DocumentationNode.Kind.Parameter, kind)
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