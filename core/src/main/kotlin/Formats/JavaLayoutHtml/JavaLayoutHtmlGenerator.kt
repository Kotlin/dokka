package org.jetbrains.dokka.Formats

import com.google.inject.Inject
import com.google.inject.name.Named
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Formats.JavaLayoutHtmlFormatOutputBuilder.Page
import org.jetbrains.dokka.NodeKind.Companion.classLike
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.io.BufferedWriter
import java.io.File
import java.net.URI

class JavaLayoutHtmlFormatGenerator @Inject constructor(
        @Named("outputDir") val root: File,
        val packageListService: PackageListService,
        val outputBuilderFactoryService: JavaLayoutHtmlFormatOutputBuilderFactory,
        private val options: DocumentationOptions,
        val logger: DokkaLogger,
        @Named("outlineRoot") val outlineRoot: String
) : Generator, JavaLayoutHtmlUriProvider {

    @set:Inject(optional = true)
    var outlineFactoryService: JavaLayoutHtmlFormatOutlineFactoryService? = null

    fun createOutputBuilderForNode(node: DocumentationNode, output: Appendable) = outputBuilderFactoryService.createOutputBuilder(output, node)

    fun DocumentationNode.getOwnerOrReport() = owner ?: run {
        error("Owner not found for $this")
    }

    override fun tryGetContainerUri(node: DocumentationNode): URI? {
        return when (node.kind) {
            NodeKind.Module -> URI("/").resolve(node.name + "/")
            NodeKind.Package -> tryGetContainerUri(node.getOwnerOrReport())?.resolve(node.name.replace('.', '/') + '/')
            in NodeKind.classLike -> tryGetContainerUri(node.getOwnerOrReport())?.resolve("${node.classNodeNameWithOuterClass()}.html")
            else -> null
        }
    }

    override fun tryGetMainUri(node: DocumentationNode): URI? {
        return when (node.kind) {
            NodeKind.Package -> tryGetContainerUri(node)?.resolve("package-summary.html")
            in NodeKind.classLike -> tryGetContainerUri(node)?.resolve("#")
            in NodeKind.memberLike -> {
                val owner = if (node.owner?.kind != NodeKind.ExternalClass) node.owner else node.owner?.owner
                if (owner!!.kind in classLike &&
                        (node.kind == NodeKind.CompanionObjectProperty || node.kind == NodeKind.CompanionObjectFunction) &&
                        owner.companion != null
                ) {
                    val signature = node.detail(NodeKind.Signature)
                    val originalFunction = owner.companion!!.members.first { it.detailOrNull(NodeKind.Signature)?.name == signature.name }
                    tryGetMainUri(owner.companion!!)?.resolveInPage(originalFunction)
                } else {
                    tryGetMainUri(owner)?.resolveInPage(node)
                }
            }
            NodeKind.TypeParameter, NodeKind.Parameter -> node.path.asReversed().drop(1).firstNotNullResult(this::tryGetMainUri)?.resolveInPage(node)
            NodeKind.AllTypes -> outlineRootUri(node).resolve ("classes.html")
            else -> null
        }
    }

    override fun tryGetOutlineRootUri(node: DocumentationNode): URI? {
        return when(node.kind) {
            NodeKind.AllTypes -> tryGetContainerUri(node.getOwnerOrReport())
            else -> tryGetContainerUri(node)
        }?.resolve(outlineRoot)
    }

    fun URI.resolveInPage(node: DocumentationNode): URI = resolve("#${node.signatureForAnchor(logger).anchorEncoded()}")

    fun buildClass(node: DocumentationNode, parentDir: File) {
        val fileForClass = parentDir.resolve(node.classNodeNameWithOuterClass() + ".html")
        fileForClass.bufferedWriter().use {
            createOutputBuilderForNode(node, it).generatePage(Page.ClassPage(node))
        }
        for (memberClass in node.members.filter { it.kind in NodeKind.classLike }) {
            buildClass(memberClass, parentDir)
        }
    }

    fun buildPackage(node: DocumentationNode, parentDir: File) {
        assert(node.kind == NodeKind.Package)
        val members = node.members
        val directoryForPackage = parentDir.resolve(node.name.replace('.', File.separatorChar))
        directoryForPackage.mkdirsOrFail()

        directoryForPackage.resolve("package-summary.html").bufferedWriter().use {
            createOutputBuilderForNode(node, it).generatePage(Page.PackagePage(node))
        }

        members.filter { it.kind in NodeKind.classLike }.forEach {
            buildClass(it, directoryForPackage)
        }
    }

    fun buildClassIndex(node: DocumentationNode, parentDir: File) {
        val file = parentDir.resolve("classes.html")
        file.bufferedWriter().use {
            createOutputBuilderForNode(node, it).generatePage(Page.ClassIndex(node))
        }
    }

    fun buildPackageIndex(module: DocumentationNode, nodes: List<DocumentationNode>, parentDir: File) {
        val file = parentDir.resolve("packages.html")
        file.bufferedWriter().use {
            val uri = outlineRootUri(module).resolve("packages.html")
            outputBuilderFactoryService.createOutputBuilder(it, uri)
                .generatePage(Page.PackageIndex(nodes))
        }
    }

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val module = nodes.single()

        val moduleRoot = root.resolve(module.name)
        val packages = module.members.filter { it.kind == NodeKind.Package }
        packages.forEach { buildPackage(it, moduleRoot) }
        val outlineRootFile = moduleRoot.resolve(outlineRoot)
        if (options.generateClassIndexPage) {
            buildClassIndex(module.members.single { it.kind == NodeKind.AllTypes }, outlineRootFile)
        }

        if (options.generatePackageIndexPage) {
            buildPackageIndex(module, packages, outlineRootFile)
        }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        val uriToWriter = mutableMapOf<URI, BufferedWriter>()

        fun provideOutput(uri: URI): BufferedWriter {
            val normalized = uri.normalize()
            uriToWriter[normalized]?.let { return it }
            val file = root.resolve(normalized.path.removePrefix("/"))
            val writer = file.bufferedWriter()
            uriToWriter[normalized] = writer
            return writer
        }

        outlineFactoryService?.generateOutlines(::provideOutput, nodes)

        uriToWriter.values.forEach { it.close() }
    }

    override fun buildSupportFiles() {}

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        nodes.filter { it.kind == NodeKind.Module }.forEach { module ->
            val moduleRoot = root.resolve(module.name)
            val packageListFile = moduleRoot.resolve("package-list")
            packageListFile.writeText(packageListService.formatPackageList(module as DocumentationModule))
        }
    }
}

interface JavaLayoutHtmlFormatOutputBuilderFactory {
    fun createOutputBuilder(output: Appendable, uri: URI): JavaLayoutHtmlFormatOutputBuilder
    fun createOutputBuilder(output: Appendable, node: DocumentationNode): JavaLayoutHtmlFormatOutputBuilder
}
