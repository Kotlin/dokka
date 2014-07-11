package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*


public class TopLevelFunctionTest {
    Test fun function() {
        verifyFiles("test/data/functions/function.kt") { model ->
            val item = model.nodes.single().members.single()
            assertEquals(DocumentationNodeKind.Function, item.kind)
            assertEquals("fn", item.name)
            assertEquals("Function fn", item.doc)
            assertTrue(item.details.none())
            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }

    Test fun functionWithParams() {
        verifyFiles("test/data/functions/functionWithParams.kt") { model ->
            val item = model.nodes.single().members.single()
            assertEquals(DocumentationNodeKind.Function, item.kind)
            assertEquals("function", item.name)
            assertEquals("""Multiline
Function
Documentation"""
            , item.doc)
            assertEquals("x", item.details.single().name)
            assertEquals(DocumentationNodeKind.Parameter, item.details.single().kind)

            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }
}