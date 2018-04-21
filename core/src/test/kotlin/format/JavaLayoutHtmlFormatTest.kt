package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatDescriptor
import org.junit.Test
import java.io.File
import java.net.URL

class JavaLayoutHtmlFormatTest : JavaLayoutHtmlFormatTestCase() {
    override val formatDescriptor = JavaLayoutHtmlFormatDescriptor()

//    @Test
//    fun simple() {
//        verifyNode("simple.kt")
//    }
//
////    @Test
////    fun topLevel() {
////        verifyPackageNode("topLevel.kt")
////    }
//
//    @Test
//    fun codeBlocks() {
//        verifyNode("codeBlocks.kt") { model ->
//            listOf(model.members.single().members.single { it.name == "foo" })
//        }
//    }
//
//    @Test
//    fun const() {
//        verifyPackageNode("const.kt", noStdlibLink = true)
//        verifyNode("const.kt", noStdlibLink = true) { model ->
//            model.members.single().members.filter { it.kind in NodeKind.classLike }
//        }
//    }
//
//    @Test
//    fun externalClassExtension() {
//        verifyPackageNode("externalClassExtension.kt")
//    }
//
//    @Test
//    fun unresolvedExternalClass() {
//        verifyNode("unresolvedExternalClass.kt", noStdlibLink = true) { model ->
//            listOf(model.members.single().members.single { it.name == "MyException" })
//        }
//    }
//
//    @Test
//    fun genericExtension() {
//        verifyNode("genericExtension.kt", noStdlibLink = true) { model ->
//            model.members.single().members(NodeKind.Class)
//        }
//    }
//
//
//    @Test
//    fun sections() {
//        verifyNode("sections.kt", noStdlibLink = true) { model ->
//            model.members.single().members.filter { it.name == "sectionsTest" }
//        }
//    }
//
//    @Test
//    fun constJava() {
//        verifyNode("ConstJava.java", noStdlibLink = true)
//    }
//
//    @Test
//    fun inboundLinksInKotlinMode() {
//        val root = "./testdata/format/java-layout-html"
//
//        val options = DocumentationOptions(
//            "",
//            "java-layout-html",
//            sourceLinks = listOf(),
//            generateClassIndexPage = false,
//            generatePackageIndexPage = false,
//            noStdlibLink = true,
//            apiVersion = null,
//            languageVersion = null,
//            perPackageOptions = listOf(PackageOptionsImpl("foo", suppress = true)),
//            externalDocumentationLinks =
//            listOf(
//                DokkaConfiguration.ExternalDocumentationLink.Builder(
//                    URL("file:///"),
//                    File(root, "inboundLinksTestPackageList").toURI().toURL()
//                ).build()
//            )
//        )
//
//
//        val sourcePath = "$root/inboundLinksInKotlinMode.kt"
//        val documentation = DocumentationModule("test")
//
//        appendDocumentation(
//            documentation,
//            contentRootFromPath(sourcePath),
//            contentRootFromPath("$root/inboundLinksInKotlinMode.Dep.kt"),
//            withJdk = false,
//            withKotlinRuntime = false,
//            options = options
//        )
//        documentation.prepareForGeneration(options)
//
//        verifyModelOutput(documentation, ".html", sourcePath) { model, output ->
//            buildPagesAndReadInto(
//                model,
//                model.members.single { it.name == "bar" }.members,
//                output
//            )
//        }
//    }
}