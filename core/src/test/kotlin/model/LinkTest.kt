package org.jetbrains.dokka.tests

import org.jetbrains.dokka.ContentBlock
import org.jetbrains.dokka.ContentNodeLazyLink
import org.jetbrains.dokka.NodeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkTest {
    @Test fun linkToSelf() {
        verifyModel("testdata/links/linkToSelf.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo -> Class:Foo]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToMember() {
        verifyModel("testdata/links/linkToMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToConstantWithUnderscores() {
        verifyModel("testdata/links/linkToConstantWithUnderscores.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [MY_CONSTANT_VALUE -> CompanionObjectProperty:MY_CONSTANT_VALUE]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToQualifiedMember() {
        verifyModel("testdata/links/linkToQualifiedMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo.member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToParam() {
        verifyModel("testdata/links/linkToParam.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("This is link to [param -> Parameter:param]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToPackage() {
        verifyModel("testdata/links/linkToPackage.kt") { model ->
            val packageNode = model.members.single()
            with(packageNode) {
                assertEquals(this.name, "test.magic")
            }
            with(packageNode.members.single()) {
                assertEquals("Magic", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("Basic implementations of [Magic -> Class:Magic] are located in [test.magic -> Package:test.magic] package", content.summary.toTestString())
                assertEquals(packageNode, ((this.content.summary as ContentBlock).children.filterIsInstance<ContentNodeLazyLink>().last()).lazyNode.invoke())
            }
        }
    }

}