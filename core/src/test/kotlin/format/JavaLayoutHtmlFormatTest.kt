package org.jetbrains.dokka.tests

import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatDescriptor
import org.jetbrains.dokka.NodeKind
import org.junit.Test

class JavaLayoutHtmlFormatTest : JavaLayoutHtmlFormatTestCase() {
    override val formatDescriptor = JavaLayoutHtmlFormatDescriptor()

    @Test
    fun simple() {
        verifyNode("simple.kt")
    }

    @Test
    fun topLevel() {
        verifyPackageNode("topLevel.kt")
    }

    @Test
    fun codeBlocks() {
        verifyNode("codeBlocks.kt") { model ->
            listOf(model.members.single().members.single { it.name == "foo" })
        }
    }

    @Test
    fun const() {
        verifyPackageNode("const.kt", noStdlibLink = true)
        verifyNode("const.kt", noStdlibLink = true) { model ->
            model.members.single().members.filter { it.kind in NodeKind.classLike }
        }
    }

    @Test
    fun externalClassExtension() {
        verifyPackageNode("externalClassExtension.kt")
    }

    @Test
    fun unresolvedExternalClass() {
        verifyNode("unresolvedExternalClass.kt", noStdlibLink = true) { model ->
            listOf(model.members.single().members.single { it.name == "MyException" })
        }
    }

    @Test
    fun genericExtension() {
        verifyNode("genericExtension.kt", noStdlibLink = true) { model ->
            model.members.single().members(NodeKind.Class)
        }
    }

    @Test
    fun constJava() {
        verifyNode("ConstJava.java", noStdlibLink = true)
    }
}