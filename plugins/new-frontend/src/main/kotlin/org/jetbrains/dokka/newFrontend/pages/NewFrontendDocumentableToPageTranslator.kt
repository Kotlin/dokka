package org.jetbrains.dokka.newFrontend.pages

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.newFrontend.pages.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.utilities.DokkaLogger

class NewFrontendDocumentableToPageTranslator(
    private val commentsToContentConverter: CommentsToContentConverter,
    private val signatureProvider: SignatureProvider,
    private val logger: DokkaLogger
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): RootPageNode =
        NewFrontendPageCreator(commentsToContentConverter, signatureProvider, logger).pageForModule(module)
}

class NewFrontendPageCreator(
    val commentsToContentConverter: CommentsToContentConverter,
    val signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    fun pageForModule(m: DModule) =
        ModulePageNode(m.name.ifEmpty { "<root>" }, m.description(), m.packages.map(::pageForPackage), contentForModule(m), emptySet(), m)

    fun pageForPackage(p: DPackage): PackagePageNode = PackagePageNode(
        name = p.name,
        children = emptyList(),
        content = contentForPackage(p),
        dri = setOf(p.dri),
        documentable = null,
        embeddedResources = emptyList()
    )

    private fun contentForModule(m: DModule): ModuleContentNode = ModuleContentNode(
        name = m.name,
        packages = m.packages.map { it.toModulePackageElement() },
        sourceSets = m.sourceSets.toDisplaySourceSets(),
        dci = DCI(setOf(m.dri), ContentKind.Main)
    )

    private fun contentForPackage(p: DPackage): PackageContentNode =
        PackageContentNode(
            dci = DCI(setOf(p.dri), ContentKind.Main),
            sourceSets = p.sourceSets.toDisplaySourceSets(),
            name = p.name,
            description = p.description()
        )

    private fun DPackage.toModulePackageElement(): ModulePackageElement =
        ModulePackageElement(
            name = name,
            dri = dri,
            sourceSets = sourceSets.toDisplaySourceSets(),
            description = description()
        )

    private fun Documentable.description(): ContentNode {
        val descriptionNodes = documentation.entries.firstOrNull()?.let {entry ->
            entry.value.children.filterIsInstance<Description>().firstOrNull()
                ?.let { commentsToContentConverter.buildContent(it.root, DCI(setOf(dri), ContentKind.Comment), sourceSets) }
        }.orEmpty()
        return ContentGroup(descriptionNodes, DCI(setOf(dri), ContentKind.Comment), sourceSets.toDisplaySourceSets(), emptySet())
    }
}