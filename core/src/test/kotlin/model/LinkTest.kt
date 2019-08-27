package org.jetbrains.dokka.tests

import org.jetbrains.dokka.ContentBlock
import org.jetbrains.dokka.ContentNodeLazyLink
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class BaseLinkTest(val analysisPlatform: Platform) {
    private val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)
    @Test fun linkToSelf() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToSelf.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo -> Class:Foo]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToExternalSite() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToExternalSite.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to http://example.com/#example", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToMember() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToMember.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToConstantWithUnderscores() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToConstantWithUnderscores.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [MY_CONSTANT_VALUE -> CompanionObjectProperty:MY_CONSTANT_VALUE]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToQualifiedMember() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToQualifiedMember.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo.member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToParam() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToParam.kt", defaultModelConfig) { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("This is link to [param -> Parameter:param]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToPackage() {
        checkSourceExistsAndVerifyModel("testdata/links/linkToPackage.kt", defaultModelConfig) { model ->
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

class JSLinkTest: BaseLinkTest(Platform.js)
class JVMLinkTest: BaseLinkTest(Platform.jvm)
class CommonLinkTest: BaseLinkTest(Platform.common)