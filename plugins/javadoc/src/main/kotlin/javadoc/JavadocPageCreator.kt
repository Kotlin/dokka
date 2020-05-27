package javadoc

import javadoc.pages.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.utilities.DokkaLogger

open class JavadocPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    fun pageForModule(m: DModule): JavadocModulePageNode =
        JavadocModulePageNode(
            m.name.ifEmpty { "root" },
            contentForModule(m),
            m.packages.map { pageForPackage(it) },
            setOf(m.dri)
        )

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

    fun contentForModule(m: DModule): JavadocContentNode =
        JavadocContentGroup(
            setOf(m.dri),
            JavadocContentKind.OverviewSummary,
            m.sourceSets.filter { it.platform == Platform.jvm }.toSet()
        ) {
            title(m.name, "0.0.1", dri = setOf(m.dri), kind = ContentKind.Main)
            list("Packages", "Package", setOf(m.dri), ContentKind.Packages, m.packages.map { p ->
                val doc = p.documentation.entries.find { (k, _) -> k.platform == Platform.jvm }?.value?.let {
                    it.children.joinToString("\n") { it.root.toString() }
                }.orEmpty()
                RowJavadocListEntry(
                    LinkJavadocListEntry(p.name, setOf(p.dri), JavadocContentKind.PackageSummary, sourceSets),
                    doc
                )
            })
        }

    fun contentForPackage(p: DPackage): JavadocContentNode =
        JavadocContentGroup(
            setOf(p.dri),
            JavadocContentKind.PackageSummary,
            p.sourceSets.filter { it.platform == Platform.jvm }.toSet()
        ) {
            title(p.name, "0.0.1", dri = setOf(p.dri), kind = ContentKind.Packages)
            list("Packages", "Package", setOf(p.dri), ContentKind.Packages, p.classlikes.map { c ->
                val doc = c.documentation.entries.find { (k, _) -> k.platform == Platform.jvm }?.value?.let {
                    it.children.joinToString("\n") { it.root.toString() }
                }.orEmpty()
                RowJavadocListEntry(
                    LinkJavadocListEntry(c.name.orEmpty(), setOf(c.dri), JavadocContentKind.Class, sourceSets),
                    doc
                )
            })
        }

    fun contentForClasslike(c: DClasslike): JavadocContentNode =
        JavadocContentGroup(
            setOf(c.dri),
            JavadocContentKind.Class,
            c.sourceSets.filter { it.platform == Platform.jvm }.toSet()
        ) {
            title(
                c.name.orEmpty(),
                "0.0.1",
                parent = c.dri.packageName,
                dri = setOf(c.dri),
                kind = JavadocContentKind.Class
            )
        }
}

