package org.jetbrains.dokka.tests.model

import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.SourceLinkDefinitionImpl
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Assert
import org.junit.Test
import java.io.File

class SourceLinksErrorTest {

    @Test
    fun absolutePath_notMatching() {
        val sourceLink = SourceLinkDefinitionImpl(File("testdata/nonExisting").absolutePath, "http://...", null)
        verifyNoSourceUrl(sourceLink)
    }

    @Test
    fun relativePath_notMatching() {
        val sourceLink = SourceLinkDefinitionImpl("testdata/nonExisting", "http://...", null)
        verifyNoSourceUrl(sourceLink)
    }

    private fun verifyNoSourceUrl(sourceLink: SourceLinkDefinitionImpl) {
        checkSourceExistsAndVerifyModel("testdata/sourceLinks/dummy.kt", ModelConfig(sourceLinks = listOf(sourceLink))) { model ->
            with(model.members.single().members.single()) {
                Assert.assertEquals("foo", name)
                Assert.assertEquals(NodeKind.Function, kind)
                Assert.assertTrue("should not have source urls", details(NodeKind.SourceUrl).isEmpty())
            }
        }
    }
}

