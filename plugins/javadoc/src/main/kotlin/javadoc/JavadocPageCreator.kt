package javadoc

import javadoc.pages.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class JavadocPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    val pageContentBuilder = PageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    fun pageForModule(m: DModule): JavadocModulePageNode =
        JavadocModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m.packages.map { pageForPackage(it) }, setOf(m.dri))

    fun pageForPackage(p: DPackage) =
        JavadocPackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.map { pageForClasslike(it) } // TODO: nested classlikes
        ).also {
            it
        }

    fun pageForClasslike(c: DClasslike): JavadocClasslikePageNode {
        val constructors = when (c) {
            is DClass -> c.constructors
            is DEnum -> c.constructors
            else -> emptyList()
        }

        return JavadocClasslikePageNode(c.name.orEmpty(), contentForClasslike(c), setOf(c.dri), c, emptyList())
    }

    fun pageForMember(m: Callable): MemberPageNode =
        throw IllegalStateException("$m should not be present here")


    //    fun contentForModule(m: DModule): ContentNode = pageContentBuilder.contentFor(m, ContentKind.Packages) {
//        header(1, ContentKind.Main) {
//            text(m.name)
//            text("0.0.1") // todo version
//        }
//        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData.toSet()) {
//            link(it.name, it.dri)
//            val doc = this.mainPlatformData.find { it.platformType == Platform.jvm }
//                ?.let { pd -> it.documentation[pd] }
//            if (doc != null) text(doc.children.joinToString("\n") { it.root.docTagSummary() })
//            else text("")
//        }
//    }
    fun contentForModule(m: DModule): JavadocContentNode = JavadocContentGroup(setOf(m.dri), ContentKind.Main) {
        title(m.name, "0.0.1", setOf(m.dri), ContentKind.Main)
        list("Packages", "Package", setOf(m.dri), ContentKind.Packages, m.packages.map { p ->
            val doc = p.documentation.map.entries.find { (k, _) -> k.platformType == Platform.jvm }?.value?.let {
                it.children.joinToString("\n") { it.root.docTagSummary() }
            }.orEmpty()
            CompoundJavadocListEntry(
                "row", listOf(
                    LinkJavadocListEntry(p.name, setOf(p.dri), ContentKind.Packages, p.platformData),
                    SimpleJavadocListEntry(doc)
                )
            )
        })
    }

//    fun contentForPackage(p: DPackage): ContentNode = pageContentBuilder.contentFor(p, ContentKind.Classlikes) {
//        header(1, ContentKind.Packages) { text(p.name) }
//        block("Class Summary", 2, ContentKind.Classlikes, p.classlikes, p.platformData.toSet()) {
//            link(it.name.orEmpty(), it.dri)
//            val doc = this.mainPlatformData.find { it.platformType == Platform.jvm }
//                ?.let { pd -> it.documentation[pd] }
//            if (doc != null) text(doc.children.joinToString("\n") { it.root.docTagSummary() })
//            else text("")
//        }
//    }

    fun contentForPackage(p: DPackage): JavadocContentNode = JavadocContentGroup(setOf(p.dri), ContentKind.Packages) {
        title(p.name, "0.0.1", setOf(p.dri), ContentKind.Packages)
        list("Packages", "Package", setOf(p.dri), ContentKind.Packages, p.classlikes.map { c ->
            val doc = c.documentation.map.entries.find { (k, _) -> k.platformType == Platform.jvm }?.value?.let {
                it.children.joinToString("\n") { it.root.docTagSummary() }
            }.orEmpty()
            CompoundJavadocListEntry(
                "row", listOf(
                    LinkJavadocListEntry(c.name.orEmpty(), setOf(c.dri), ContentKind.Packages, c.platformData),
                    SimpleJavadocListEntry(doc)
                )
            )
        })
    }

    fun contentForClasslike(c: DClasslike): JavadocContentNode = JavadocContentGroup(setOf(c.dri), ContentKind.Classlikes) {
        title(c.name.orEmpty(), "0.0.1", setOf(c.dri), ContentKind.Classlikes)
    }
}

