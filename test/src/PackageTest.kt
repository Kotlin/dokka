package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*

public class PackageTest {
    Test fun rootPackage() {
        verifyModel("test/data/packages/rootPackage.kt") { model ->
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
        verifyModel("test/data/packages/simpleNamePackage.kt") { model ->
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
        verifyModel("test/data/packages/dottedNamePackage.kt") { model ->
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