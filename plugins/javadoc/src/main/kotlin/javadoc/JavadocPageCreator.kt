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
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.DCI
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class JavadocPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    val signatureProvider: SignatureProvider,
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
        c.sourceSets.firstOrNull { it.analysisPlatform == Platform.jvm }?.let { jvm ->
            JavadocClasslikePageNode(
                name = c.name.orEmpty(),
                content = contentForClasslike(c),
                dri = setOf(c.dri),
                modifiers = listOfNotNull(c.visibility[jvm]?.name),
                signature = signatureProvider.signature(c).jvmSignature(),
                description = c.description(jvm),
                constructors = c.safeAs<WithConstructors>()?.constructors?.map { it.toJavadocFunction(jvm) }.orEmpty(),
                methods = c.functions.map { it.toJavadocFunction(jvm) },
                entries = c.safeAs<DEnum>()?.entries?.map {
                    JavadocEntryNode(
                        signatureProvider.signature(it).jvmSignature(),
                        it.description(jvm)
                    )
                }.orEmpty(),
                classlikes = c.classlikes.mapNotNull { pageForClasslike(it) },
                properties = c.properties.map {
                    JavadocPropertyNode(
                        signatureProvider.signature(it).jvmSignature(),
                        TextNode(it.description(jvm), setOf(jvm))
                    )
                },
                documentable = c,
                extras = c.safeAs<WithExtraProperties<Documentable>>()?.extra ?: PropertyContainer.empty()
            )
        }

    fun contentForModule(m: DModule): JavadocContentNode =
        JavadocContentGroup(
            setOf(m.dri),
            JavadocContentKind.OverviewSummary,
            m.sourceSets.filter { it.analysisPlatform == Platform.jvm }.toSet()
        ) {
            title(m.name, "0.0.1", dri = setOf(m.dri), kind = ContentKind.Main)
            list("Packages", "Package", setOf(m.dri), ContentKind.Packages, m.packages.sortedBy { it.name }.map { p ->
                val description = p.documentation.entries.find { (k, _) -> k.analysisPlatform == Platform.jvm }?.value?.let {
                    it.children.firstIsInstanceOrNull<Description>()?.let { description ->
                        DocTagToContentConverter.buildContent(
                            description.root,
                            DCI(setOf(p.dri), JavadocContentKind.OverviewSummary),
                            sourceSets
                        )
                    }
                }.orEmpty()
                RowJavadocListEntry(
                    LinkJavadocListEntry(p.name, setOf(p.dri), JavadocContentKind.PackageSummary, sourceSets),
                    description
                )
            })
        }

    fun contentForPackage(p: DPackage): JavadocContentNode =
        JavadocContentGroup(
            setOf(p.dri),
            JavadocContentKind.PackageSummary,
            p.sourceSets.filter { it.analysisPlatform == Platform.jvm }.toSet()
        ) {
            title(p.name, "0.0.1", dri = setOf(p.dri), kind = ContentKind.Packages)
            list("Packages", "Package", setOf(p.dri), ContentKind.Packages, p.classlikes.sortedBy { it.name }.map { c ->
                RowJavadocListEntry(
                    LinkJavadocListEntry(c.name.orEmpty(), setOf(c.dri), JavadocContentKind.Class, sourceSets),
                    listOf(signatureProvider.signature(c).jvmSignature())
                )
            })
        }

    fun contentForClasslike(c: DClasslike): JavadocContentNode =
        JavadocContentGroup(
            setOf(c.dri),
            JavadocContentKind.Class,
            c.sourceSets.filter { it.analysisPlatform == Platform.jvm }.toSet()
        ) {
            title(
                c.name.orEmpty(),
                "0.0.1",
                parent = c.dri.packageName,
                dri = setOf(c.dri),
                kind = JavadocContentKind.Class
            )
        }

    private fun signatureForProjection(p: Projection): String =
        when (p) {
            is OtherParameter -> p.name
            is TypeConstructor -> if (p.function)
                "TODO"
            else {
                val other = if (p.projections.isNotEmpty()) {
                    p.projections.joinToString(prefix = "<", postfix = ">") { signatureForProjection(it) }
                } else {
                    ""
                }
                "${p.dri.classNames.orEmpty()} $other"
            }

            is Variance -> "${p.kind} ${signatureForProjection(p.inner)}"
            is Star -> "*"
            is Nullable -> "${signatureForProjection(p.inner)}?"
            is JavaObject -> "Object"
            is Void -> "Void"
            is PrimitiveJavaType -> p.name
            is Dynamic -> "dynamic"
            is UnresolvedBound -> p.name
        }

    private fun DFunction.toJavadocFunction(sourceSetData: DokkaSourceSet) = JavadocFunctionNode(
        name = name,
        signature = signatureProvider.signature(this).jvmSignature(),
        brief = TextNode(description(sourceSetData), setOf(sourceSetData)),
        parameters = parameters.map {
            JavadocParameterNode(
                name = it.name.orEmpty(),
                type = signatureForProjection(it.type),
                description = TextNode(it.findNodeInDocumentation<Param>(sourceSetData), setOf(sourceSetData))
            )
        },
        extras = extra
    )

    fun List<ContentNode>.jvmSignature(): ContentNode =
        first { it.sourceSets.any { it.analysisPlatform == Platform.jvm } }

    private fun Documentable.description(sourceSetData: DokkaSourceSet): String =
        findNodeInDocumentation<Description>(sourceSetData)

    private inline fun <reified T : TagWrapper> Documentable.findNodeInDocumentation(sourceSetData: DokkaSourceSet): String =
        documentation[sourceSetData]?.children?.firstIsInstanceOrNull<T>()?.root?.children?.firstIsInstanceOrNull<Text>()?.body.orEmpty()
}

