package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*

public class PackageTest {
    Test fun rootPackage() {
        verifyFiles("test/data/packages/rootPackage.kt") { model ->
            val item = model.nodes.single()
            assertEquals(DocumentationNodeKind.Package, item.kind)
            assertEquals("", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }

    Test fun simpleNamePackage() {
        verifyFiles("test/data/packages/simpleNamePackage.kt") { model ->
            val item = model.nodes.single()
            assertEquals(DocumentationNodeKind.Package, item.kind)
            assertEquals("simple", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }

    Test fun dottedNamePackage() {
        verifyFiles("test/data/packages/dottedNamePackage.kt") { model ->
            val item = model.nodes.single()
            assertEquals(DocumentationNodeKind.Package, item.kind)
            assertEquals("dot.name", item.name)
            assertEquals("", item.doc)
            assertTrue(item.details.none())
            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }
}