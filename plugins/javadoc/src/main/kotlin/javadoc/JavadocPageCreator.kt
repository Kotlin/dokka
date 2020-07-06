package javadoc

import javadoc.pages.*
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.signatures.function
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.NamedTagWrapper
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class JavadocPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    private val signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {

    fun pageForModule(m: DModule): JavadocModulePageNode =
        JavadocModulePageNode(
            name = m.name.ifEmpty { "root" },
            content = contentForModule(m),
            children = m.packages.map { pageForPackage(it) },
            dri = setOf(m.dri)
        )

    fun pageForPackage(p: DPackage) =
        JavadocPackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.mapNotNull { pageForClasslike(it) } // TODO: nested classlikes
        )

    fun pageForClasslike(c: DClasslike): JavadocClasslikePageNode? =
        c.highestJvmSourceSet?.let { jvm ->
            JavadocClasslikePageNode(
                name = c.name.orEmpty(),
                content = contentForClasslike(c),
                dri = setOf(c.dri),
                signature = signatureProvider.signature(c).nodeForJvm(jvm).asJavadocNode(),
                description = c.descriptionToContentNodes(),
                constructors = (c as? WithConstructors)?.constructors?.mapNotNull { it.toJavadocFunction() }.orEmpty(),
                methods = c.functions.mapNotNull { it.toJavadocFunction() },
                entries = (c as? DEnum)?.entries?.map {
                    JavadocEntryNode(
                        signatureProvider.signature(it).nodeForJvm(jvm).asJavadocNode(),
                        it.descriptionToContentNodes(jvm)
                    )
                }.orEmpty(),
                classlikes = c.classlikes.mapNotNull { pageForClasslike(it) },
                properties = c.properties.map {
                    JavadocPropertyNode(
                        signatureProvider.signature(it).nodeForJvm(jvm).asJavadocNode(),
                        it.descriptionToContentNodes(jvm)
                    )
                },
                documentable = c,
                extras = (c as? WithExtraProperties<Documentable>)?.extra ?: PropertyContainer.empty()
            )
        }

    private fun contentForModule(m: DModule): JavadocContentNode =
        JavadocContentGroup(
            setOf(m.dri),
            JavadocContentKind.OverviewSummary,
            m.jvmSourceSets.toSet()
        ) {
            title(m.name, m.brief(), "0.0.1", dri = setOf(m.dri), kind = ContentKind.Main)
            list("Packages", "Package", setOf(m.dri), ContentKind.Packages, m.packages.sortedBy { it.name }.map { p ->
                RowJavadocListEntry(
                    LinkJavadocListEntry(p.name, setOf(p.dri), JavadocContentKind.PackageSummary, sourceSets),
                    p.brief()
                )
            })
        }

    private fun contentForPackage(p: DPackage): JavadocContentNode =
        JavadocContentGroup(
            setOf(p.dri),
            JavadocContentKind.PackageSummary,
            p.jvmSourceSets.toSet()
        ) {
            title(p.name, p.brief(), "0.0.1", dri = setOf(p.dri), kind = ContentKind.Packages)
            list("Packages", "Package", setOf(p.dri), ContentKind.Packages, p.classlikes.sortedBy { it.name }.map { c ->
                RowJavadocListEntry(
                    LinkJavadocListEntry(c.name.orEmpty(), setOf(c.dri), JavadocContentKind.Class, sourceSets),
                    c.brief()
                )
            })
        }

    private fun contentForClasslike(c: DClasslike): JavadocContentNode =
        JavadocContentGroup(
            setOf(c.dri),
            JavadocContentKind.Class,
            c.jvmSourceSets.toSet()
        ) {
            title(
                c.name.orEmpty(),
                c.brief(),
                "0.0.1",
                parent = c.dri.packageName,
                dri = setOf(c.dri),
                kind = JavadocContentKind.Class
            )
        }

    private fun DFunction.toJavadocFunction() = highestJvmSourceSet?.let { jvm ->
        JavadocFunctionNode(
            name = name,
            dri = dri,
            signature = signatureProvider.signature(this).nodeForJvm(jvm).asJavadocNode(),
            brief = brief(jvm),
            parameters = parameters.mapNotNull {
                val signature = signatureProvider.signature(it).nodeForJvm(jvm).asJavadocNode()
                signature.modifiers?.let { type ->
                    JavadocParameterNode(
                        name = it.name.orEmpty(),
                        type = type,
                        description = it.brief()
                    )
                }
            },
            extras = extra
        )
    }

    private val Documentable.jvmSourceSets
        get() = sourceSets.filter { it.analysisPlatform == Platform.jvm }

    private val Documentable.highestJvmSourceSet
        get() = jvmSourceSets.let { sources ->
            sources.firstOrNull { it != expectPresentInSet } ?: sources.firstOrNull()
        }

    private val firstSentenceRegex = Regex("^((?:[^.?!]|[.!?](?!\\s))*[.!?])")

    private inline fun <reified T : TagWrapper> Documentable.findNodeInDocumentation(sourceSetData: DokkaSourceSet?): T? =
        documentation[sourceSetData]?.firstChildOfTypeOrNull<T>()

    private fun Documentable.descriptionToContentNodes(sourceSet: DokkaSourceSet? = highestJvmSourceSet) =
        contentNodesFromType<Description>(sourceSet)

    private fun DParameter.paramsToContentNodes(sourceSet: DokkaSourceSet? = highestJvmSourceSet) =
        contentNodesFromType<Param>(sourceSet)

    private inline fun <reified T : TagWrapper> Documentable.contentNodesFromType(sourceSet: DokkaSourceSet?) =
        findNodeInDocumentation<T>(sourceSet)?.let {
            DocTagToContentConverter.buildContent(
                it.root,
                DCI(setOf(dri), JavadocContentKind.OverviewSummary),
                sourceSets.toSet()
            )
        }.orEmpty()

    fun List<ContentNode>.nodeForJvm(jvm: DokkaSourceSet): ContentNode =
        first { it.sourceSets.contains(jvm) }

    private fun Documentable.brief(sourceSet: DokkaSourceSet? = highestJvmSourceSet): List<ContentNode> =
        briefFromContentNodes(descriptionToContentNodes(sourceSet))

    private fun briefFromContentNodes(description: List<ContentNode>): List<ContentNode> {
        val contents = mutableListOf<ContentNode>()
        for (node in description) {
            if (node is ContentText && firstSentenceRegex.containsMatchIn(node.text)) {
                contents.add(node.copy(text = firstSentenceRegex.find(node.text)?.value.orEmpty()))
                break
            } else {
                contents.add(node)
            }
        }
        return contents
    }

    private fun DParameter.brief(sourceSet: DokkaSourceSet? = highestJvmSourceSet): List<ContentNode> =
        briefFromContentNodes(paramsToContentNodes(sourceSet).dropWhile { it is ContentDRILink })

    private fun ContentNode.asJavadocNode(): JavadocSignatureContentNode =
        (this as ContentGroup).children.firstOrNull() as? JavadocSignatureContentNode ?: throw IllegalStateException("JavadocPageCreator should be used only with JavadocSignatureProvider")
}

