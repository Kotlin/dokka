package org.jetbrains.dokka.Formats

import com.google.inject.Binder
import kotlinx.html.*
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Utilities.bind
import org.jetbrains.dokka.Utilities.lazyBind
import org.jetbrains.dokka.Utilities.toOptional
import org.jetbrains.dokka.Utilities.toType
import java.net.URI
import kotlin.reflect.KClass


abstract class JavaLayoutHtmlFormatDescriptorBase : FormatDescriptor, DefaultAnalysisComponent {

    override fun configureOutput(binder: Binder): Unit = with(binder) {
        bind<Generator>() toType generatorServiceClass
        bind<LanguageService>() toType languageServiceClass
        bind<JavaLayoutHtmlTemplateService>() toType templateServiceClass
        bind<JavaLayoutHtmlUriProvider>() toType generatorServiceClass
        lazyBind<JavaLayoutHtmlFormatOutlineFactoryService>() toOptional outlineFactoryClass
        bind<PackageListService>() toType packageListServiceClass
        bind<JavaLayoutHtmlFormatOutputBuilderFactory>() toType outputBuilderFactoryClass
    }

    val generatorServiceClass = JavaLayoutHtmlFormatGenerator::class
    abstract val languageServiceClass: KClass<out LanguageService>
    abstract val templateServiceClass: KClass<out JavaLayoutHtmlTemplateService>
    abstract val outlineFactoryClass: KClass<out JavaLayoutHtmlFormatOutlineFactoryService>?
    abstract val packageListServiceClass: KClass<out PackageListService>
    abstract val outputBuilderFactoryClass: KClass<out JavaLayoutHtmlFormatOutputBuilderFactory>
}

class JavaLayoutHtmlFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsKotlin {
    override val outputBuilderFactoryClass: KClass<out JavaLayoutHtmlFormatOutputBuilderFactory> = JavaLayoutHtmlFormatOutputBuilderFactoryImpl::class
    override val packageListServiceClass: KClass<out PackageListService> = JavaLayoutHtmlPackageListService::class
    override val languageServiceClass = KotlinLanguageService::class
    override val templateServiceClass = JavaLayoutHtmlTemplateService.Default::class
    override val outlineFactoryClass = null
}

class JavaLayoutHtmlAsJavaFormatDescriptor : JavaLayoutHtmlFormatDescriptorBase(), DefaultAnalysisComponentServices by KotlinAsJava {
    override val outputBuilderFactoryClass: KClass<out JavaLayoutHtmlFormatOutputBuilderFactory> = JavaLayoutHtmlFormatOutputBuilderFactoryImpl::class
    override val packageListServiceClass: KClass<out PackageListService> = JavaLayoutHtmlPackageListService::class
    override val languageServiceClass = NewJavaLanguageService::class
    override val templateServiceClass = JavaLayoutHtmlTemplateService.Default::class
    override val outlineFactoryClass = null
}

interface JavaLayoutHtmlFormatOutlineFactoryService {
    fun generateOutlines(outputProvider: (URI) -> Appendable, nodes: Iterable<DocumentationNode>)
}


interface JavaLayoutHtmlUriProvider {
    fun tryGetContainerUri(node: DocumentationNode): URI?
    fun tryGetMainUri(node: DocumentationNode): URI?
    fun tryGetOutlineRootUri(node: DocumentationNode): URI?
    fun containerUri(node: DocumentationNode): URI = tryGetContainerUri(node) ?: error("Unsupported ${node.kind}")
    fun mainUri(node: DocumentationNode): URI = tryGetMainUri(node) ?: error("Unsupported ${node.kind}")
    fun outlineRootUri(node: DocumentationNode): URI = tryGetOutlineRootUri(node) ?: error("Unsupported ${node.kind}")


    fun linkTo(to: DocumentationNode, from: URI): String {
        return mainUri(to).relativeTo(from).toString()
    }

    fun linkToFromOutline(to: DocumentationNode, from: URI): String {
        return outlineRootUri(to).relativeTo(from).toString()
    }

    fun mainUriOrWarn(node: DocumentationNode): URI? = tryGetMainUri(node) ?: (null).also {
        AssertionError("Not implemented mainUri for ${node.kind}").printStackTrace()
    }
}


interface JavaLayoutHtmlTemplateService {
    fun composePage(
            page: JavaLayoutHtmlFormatOutputBuilder.Page,
            tagConsumer: TagConsumer<Appendable>,
            headContent: HEAD.() -> Unit,
            bodyContent: BODY.() -> Unit
    )

    class Default : JavaLayoutHtmlTemplateService {
        override fun composePage(
                page: JavaLayoutHtmlFormatOutputBuilder.Page,
                tagConsumer: TagConsumer<Appendable>,
                headContent: HEAD.() -> Unit,
                bodyContent: BODY.() -> Unit
        ) {
            tagConsumer.html {
                head {
                    meta(charset = "UTF-8")
                    headContent()
                }
                body(block = bodyContent)
            }
        }
    }
}

val DocumentationNode.companion get() = members(NodeKind.Object).find { it.details(NodeKind.Modifier).any { it.name == "companion" } }

fun DocumentationNode.signatureForAnchor(logger: DokkaLogger): String {

    fun StringBuilder.appendReceiverIfSo() {
        detailOrNull(NodeKind.Receiver)?.let {
            append("(")
            append(it.detail(NodeKind.Type).qualifiedNameFromType())
            append(").")
        }
    }

    return when (kind) {
        NodeKind.Function, NodeKind.Constructor, NodeKind.CompanionObjectFunction -> buildString {
            if (kind == NodeKind.CompanionObjectFunction) {
                append("Companion.")
            }
            appendReceiverIfSo()
            append(name)
            details(NodeKind.Parameter).joinTo(this, prefix = "(", postfix = ")") { it.detail(NodeKind.Type).qualifiedNameFromType() }
        }
        NodeKind.Property, NodeKind.CompanionObjectProperty -> buildString {
            if (kind == NodeKind.CompanionObjectProperty) {
                append("Companion.")
            }
            appendReceiverIfSo()
            append(name)
            append(":")
            append(detail(NodeKind.Type).qualifiedNameFromType())
        }
        NodeKind.TypeParameter, NodeKind.Parameter -> this.detail(NodeKind.Signature).name // Todo Why not signatureForAnchor
        NodeKind.Field -> name
        NodeKind.EnumItem -> "ENUM_VALUE:$name"
        NodeKind.Attribute -> "attr_$name"
        else -> "Not implemented signatureForAnchor $this".also { logger.warn(it) }
    }
}

fun DocumentationNode.classNodeNameWithOuterClass(): String {
    assert(kind in NodeKind.classLike)
    return path.dropWhile { it.kind == NodeKind.Package || it.kind == NodeKind.Module }.joinToString(separator = ".") { it.name }
}
