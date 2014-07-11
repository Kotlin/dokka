package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*


public class FunctionTest {
    Test fun function() {
        verifyModel("test/data/functions/function.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("fn", name)
                assertEquals("Function fn", doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun functionWithParams() {
        verifyModel("test/data/functions/functionWithParams.kt") { model ->
            with(model.nodes.single().members.single()) {
                assertEquals(DocumentationNodeKind.Function, kind)
                assertEquals("function", name)
                assertEquals("""Multiline
Function
Documentation"""
                             , doc)
                assertEquals("x", details.single().name)
                assertEquals(DocumentationNodeKind.Parameter, details.single().kind)

                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}