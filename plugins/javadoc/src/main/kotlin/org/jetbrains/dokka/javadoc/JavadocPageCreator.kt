package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Index
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.reflect.KClass

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
                brief = c.brief(),
                signature = signatureForNode(c, jvm),
                description = c.descriptionToContentNodes(),
                constructors = (c as? WithConstructors)?.constructors?.mapNotNull { it.toJavadocFunction() }.orEmpty(),
                methods = c.functions.mapNotNull { it.toJavadocFunction() },
                entries = (c as? DEnum)?.entries?.map {
                    JavadocEntryNode(
                        it.dri,
                        it.name,
                        signatureForNode(it, jvm),
                        it.descriptionToContentNodes(jvm),
                        PropertyContainer.withAll(it.indexesInDocumentation())
                    )
                }.orEmpty(),
                classlikes = c.classlikes.mapNotNull { pageForClasslike(it) },
                properties = c.properties.map {
                    JavadocPropertyNode(
                        it.dri,
                        it.name,
                        signatureForNode(it, jvm),
                        it.descriptionToContentNodes(jvm),
                        PropertyContainer.withAll(it.indexesInDocumentation())
                    )
                },
                documentable = c,
                extra = ((c as? WithExtraProperties<Documentable>)?.extra ?: PropertyContainer.empty()) + c.indexesInDocumentation()
            )
        }

    private fun contentForModule(m: DModule): JavadocContentNode =
        JavadocContentGroup(
            setOf(m.dri),
            JavadocContentKind.OverviewSummary,
            m.jvmSourceSets.toSet()
        ) {
            title(m.name, m.brief(), "0.0.1", dri = setOf(m.dri), kind = ContentKind.Main)
            leafList(setOf(m.dri),
                ContentKind.Packages, JavadocList(
                    "Packages", "Package",
                    m.packages.sortedBy { it.name }.map { p ->
                        RowJavadocListEntry(
                            LinkJavadocListEntry(p.name, setOf(p.dri), JavadocContentKind.PackageSummary, sourceSets),
                            p.brief()
                        )
                    }
                ))
        }

    private fun contentForPackage(p: DPackage): JavadocContentNode =
        JavadocContentGroup(
            setOf(p.dri),
            JavadocContentKind.PackageSummary,
            p.jvmSourceSets.toSet()
        ) {
            title(p.name, p.brief(), "0.0.1", dri = setOf(p.dri), kind = ContentKind.Packages)
            val rootList = p.classlikes.groupBy { it::class }.map { (key, value) ->
                JavadocList(key.tabTitle, key.colTitle, value.map { c ->
                    RowJavadocListEntry(
                        LinkJavadocListEntry(c.name ?: "", setOf(c.dri), JavadocContentKind.Class, sourceSets),
                        c.brief()
                    )
                })
            }
            rootList(setOf(p.dri), JavadocContentKind.Class, rootList)
        }

    private val KClass<out DClasslike>.colTitle: String
        get() = when(this) {
            DClass::class -> "Class"
            DObject::class -> "Object"
            DAnnotation::class -> "Annotation"
            DEnum::class -> "Enum"
            DInterface::class -> "Interface"
            else -> ""
        }

    private val KClass<out DClasslike>.tabTitle: String
        get() = "$colTitle Summary"

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
            signature = signatureForNode(this, jvm),
            brief = brief(jvm),
            parameters = parameters.mapNotNull {
                val signature = signatureForNode(it, jvm)
                signature.modifiers?.let { type ->
                    JavadocParameterNode(
                        name = it.name.orEmpty(),
                        type = type,
                        description = it.brief(),
                        typeBound = it.type,
                        dri = it.dri,
                        extra = PropertyContainer.withAll(it.indexesInDocumentation())
                    )
                }
            },
            extra = extra + indexesInDocumentation()
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
        (this as ContentGroup).firstChildOfTypeOrNull<JavadocSignatureContentNode>()
            ?: throw IllegalStateException("No content for javadoc signature found")

    private fun signatureForNode(documentable: Documentable, sourceSet: DokkaSourceSet): JavadocSignatureContentNode =
        signatureProvider.signature(documentable).nodeForJvm(sourceSet).asJavadocNode()

    private fun Documentable.indexesInDocumentation(): JavadocIndexExtra {
        val indexes = documentation[highestJvmSourceSet]?.withDescendants()?.filterIsInstance<Index>()?.toList().orEmpty()
        return JavadocIndexExtra(
            indexes.map {
                ContentGroup(
                    children = DocTagToContentConverter.buildContent(
                        it,
                        DCI(setOf(dri), JavadocContentKind.OverviewSummary),
                        sourceSets.toSet()
                    ),
                    dci = DCI(setOf(dri), JavadocContentKind.OverviewSummary),
                    sourceSets = sourceSets.toSet(),
                    style = emptySet(),
                    extra = PropertyContainer.empty()
                )
            }
        )
    }
}

