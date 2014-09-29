package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class PackageTest {
    Test fun rootPackage() {
        verifyModel("test/data/packages/rootPackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals("", name)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun simpleNamePackage() {
        verifyModel("test/data/packages/simpleNamePackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun dottedNamePackage() {
        verifyModel("test/data/packages/dottedNamePackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals("dot.name", name)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun multipleFiles() {
        verifyModel("test/data/packages/dottedNamePackage.kt", "test/data/packages/simpleNamePackage.kt") { model ->
            assertEquals(2, model.members.count())
            with(model.members.single { it.name == "simple" }) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
            with(model.members.single { it.name == "dot.name" }) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun multipleFilesSamePackage() {
        verifyModel("test/data/packages/simpleNamePackage.kt", "test/data/packages/simpleNamePackage2.kt") { model ->
            assertEquals(1, model.members.count())
            with(model.members.elementAt(0)) {
                assertEquals(DocumentationNode.Kind.Package, kind)
                assertEquals("simple", name)
                assertEquals(Content.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}