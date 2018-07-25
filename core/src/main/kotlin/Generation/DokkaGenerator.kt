package org.jetbrains.dokka

import com.google.inject.Guice
import com.google.inject.Injector
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.dokka.DokkaConfiguration.SourceRoot
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.KotlinSourceRoot
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.system.measureTimeMillis

class DokkaGenerator(val logger: DokkaLogger,
                     val classpath: List<String>,
                     val sources: List<SourceRoot>,
                     val samples: List<String>,
                     val includes: List<String>,
                     val moduleName: String,
                     val options: DocumentationOptions) {

    private val documentationModules: MutableList<DocumentationModule> = mutableListOf()

    fun generate() {
        val sourcesGroupedByPlatform = sources.groupBy { it.platforms.firstOrNull() to it.analysisPlatform }
        for ((platformsInfo, roots) in sourcesGroupedByPlatform) {
            val (platform, analysisPlatform) = platformsInfo

            val documentationModule = DocumentationModule(moduleName)
            appendSourceModule(platform, analysisPlatform, roots, documentationModule)
            documentationModules.add(documentationModule)
        }

        val totalDocumentationModule = DocumentationMerger(documentationModules).merge()
        totalDocumentationModule.prepareForGeneration(options)

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            val outputInjector = Guice.createInjector(DokkaOutputModule(options, logger))
            outputInjector.getInstance(Generator::class.java).buildAll(totalDocumentationModule)
        }
        logger.info("done in ${timeBuild / 1000} secs")
    }

    private fun appendSourceModule(
        defaultPlatform: String?,
        analysisPlatform: Platform,
        sourceRoots: List<SourceRoot>,
        documentationModule: DocumentationModule
    ) {

        val sourcePaths = sourceRoots.map { it.path }
        val environment = createAnalysisEnvironment(sourcePaths, analysisPlatform)

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(options.outputDir)}")
        logger.info("Sources: ${sourcePaths.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val defaultPlatformAsList = defaultPlatform?.let { listOf(it) }.orEmpty()
        val defaultPlatformsProvider = object : DefaultPlatformsProvider {
            override fun getDefaultPlatforms(descriptor: DeclarationDescriptor): List<String> {
                val containingFilePath = descriptor.sourcePsi()?.containingFile?.virtualFile?.canonicalPath
                        ?.let { File(it).absolutePath }
                val sourceRoot = containingFilePath?.let { path -> sourceRoots.find { path.startsWith(it.path) } }
                return sourceRoot?.platforms ?: defaultPlatformAsList
            }
        }

        val injector = Guice.createInjector(
                DokkaAnalysisModule(environment, options, defaultPlatformsProvider, documentationModule.nodeRefGraph, logger))

        buildDocumentationModule(injector, documentationModule, { isNotSample(it) }, includes)
        documentationModule.nodeRefGraph.nodeMapView.forEach { (_, node) ->
            node.addReferenceTo(
                DocumentationNode(analysisPlatform.key, Content.Empty, NodeKind.Platform),
                RefKind.Platform
            )
        }

        val timeAnalyse = System.currentTimeMillis() - startAnalyse
        logger.info("done in ${timeAnalyse / 1000} secs")

        Disposer.dispose(environment)
    }

    fun createAnalysisEnvironment(sourcePaths: List<String>, analysisPlatform: Platform): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger), analysisPlatform)

        environment.apply {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
            for (element in this@DokkaGenerator.classpath) {
                addClasspath(File(element))
            }

            addSources(sourcePaths)
            addSources(this@DokkaGenerator.samples)

            loadLanguageVersionSettings(options.languageVersion, options.apiVersion)
        }

        return environment
    }

    fun isNotSample(file: PsiFile): Boolean {
        val sourceFile = File(file.virtualFile!!.path)
        return samples.none { sample ->
            val canonicalSample = File(sample).canonicalPath
            val canonicalSource = sourceFile.canonicalPath
            canonicalSource.startsWith(canonicalSample)
        }
    }
}

class DokkaMessageCollector(val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

fun buildDocumentationModule(injector: Injector,
                             documentationModule: DocumentationModule,
                             filesToDocumentFilter: (PsiFile) -> Boolean = { file -> true },
                             includes: List<String> = listOf()) {

    val coreEnvironment = injector.getInstance(KotlinCoreEnvironment::class.java)
    val fragmentFiles = coreEnvironment.getSourceFiles().filter(filesToDocumentFilter)

    val resolutionFacade = injector.getInstance(DokkaResolutionFacade::class.java)
    val analyzer = resolutionFacade.getFrontendService(LazyTopDownAnalyzer::class.java)
    analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, fragmentFiles)

    val fragments = fragmentFiles
            .map { resolutionFacade.resolveSession.getPackageFragment(it.packageFqName) }
            .filterNotNull()
            .distinct()

    val packageDocs = injector.getInstance(PackageDocs::class.java)
    for (include in includes) {
        packageDocs.parse(include, fragments)
    }
    if (documentationModule.content.isEmpty()) {
        documentationModule.updateContent {
            for (node in packageDocs.moduleContent.children) {
                append(node)
            }
        }
    }

    parseJavaPackageDocs(packageDocs, coreEnvironment)

    with(injector.getInstance(DocumentationBuilder::class.java)) {
        documentationModule.appendFragments(fragments, packageDocs.packageContent,
                injector.getInstance(PackageDocumentationBuilder::class.java))

        propagateExtensionFunctionsToSubclasses(fragments, resolutionFacade)
    }

    val javaFiles = coreEnvironment.getJavaSourceFiles().filter(filesToDocumentFilter)
    with(injector.getInstance(JavaDocumentationBuilder::class.java)) {
        javaFiles.map { appendFile(it, documentationModule, packageDocs.packageContent) }
    }
}

fun parseJavaPackageDocs(packageDocs: PackageDocs, coreEnvironment: KotlinCoreEnvironment) {
    val contentRoots = coreEnvironment.configuration.get(JVMConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.map { it.file }
            ?: listOf()
    contentRoots.forEach { root ->
        root.walkTopDown().filter { it.name == "overview.html" }.forEach {
            packageDocs.parseJava(it.path, it.relativeTo(root).parent.replace("/", "."))
        }
    }
}


fun KotlinCoreEnvironment.getJavaSourceFiles(): List<PsiJavaFile> {
    val sourceRoots = configuration.get(JVMConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.map { it.file }
            ?: listOf()

    val result = arrayListOf<PsiJavaFile>()
    val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")
    sourceRoots.forEach { sourceRoot ->
        sourceRoot.absoluteFile.walkTopDown().forEach {
            val vFile = localFileSystem.findFileByPath(it.path)
            if (vFile != null) {
                val psiFile = PsiManager.getInstance(project).findFile(vFile)
                if (psiFile is PsiJavaFile) {
                    result.add(psiFile)
                }
            }
        }
    }
    return result
}

class DocumentationMerger(
    private val documentationModules: List<DocumentationModule>
) {
    private val producedNodeRefGraph: NodeReferenceGraph
    private val signatureMap: Map<DocumentationNode, String>

    init {
        signatureMap = documentationModules
            .flatMap { it.nodeRefGraph.nodeMapView.entries }
            .map { (k, v) -> v to k }
            .toMap()

        producedNodeRefGraph = NodeReferenceGraph()
        documentationModules.map { it.nodeRefGraph }
            .flatMap { it.references }
            .distinct()
            .forEach { producedNodeRefGraph.addReference(it) }
    }

    private fun splitReferencesByKind(
        source: List<DocumentationReference>,
        kind: RefKind
    ): Pair<List<DocumentationReference>, List<DocumentationReference>> =
        Pair(source.filter { it.kind == kind }, source.filterNot { it.kind == kind })


    private fun mergePackageReferences(
        from: DocumentationNode,
        packages: List<DocumentationReference>
    ): List<DocumentationReference> {
        val packagesByName = packages
            .map { it.to }
            .groupBy { it.name }

        val mutableList: MutableList<DocumentationReference> = mutableListOf()
        for ((name, listOfPackages) in packagesByName) {
            val producedPackage = mergePackagesWithEqualNames(from, listOfPackages)
            updatePendingReferences(name, producedPackage)

            mutableList.add(
                DocumentationReference(from, producedPackage, RefKind.Member)
            )
        }

        return mutableList
    }

    private fun mergePackagesWithEqualNames(
        from: DocumentationNode,
        packages: List<DocumentationNode>
    ): DocumentationNode {
        val references = packages.flatMap { it.allReferences() }

        val mergedPackage =
            DocumentationNode(
                packages.first().name,
                Content.Empty,
                NodeKind.Package
            )

        val mergedReferences = mergeReferences(mergedPackage, references)

        for (packageNode in packages) {
            mergedPackage.updateContent {
                for (otherChild in packageNode.content.children) {
                    children.add(otherChild)
                }
            }
        }

        mergedPackage.dropReferences { true } // clear()
        for (ref in mergedReferences.distinct()) {
            mergedPackage.addReference(ref)
        }

        from.append(mergedPackage, RefKind.Member)

        return mergedPackage
    }


    private fun mergeMembers(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val membersBySignature: Map<String, List<DocumentationNode>> = refs.map { it.to }
            .filter { signatureMap.containsKey(it) }
            .groupBy { signatureMap[it]!! }

        val mergedMembers: MutableList<DocumentationReference> = mutableListOf()
        for ((signature, members) in membersBySignature) {
            val newNode = mergeMembersWithEqualSignature(signature, from, members)

            producedNodeRefGraph.register(signature, newNode)
            updatePendingReferences(signature, newNode)
            from.append(newNode, RefKind.Member)

            mergedMembers.add(DocumentationReference(from, newNode, RefKind.Member))
        }

        return mergedMembers
    }

    private fun mergeMembersWithEqualSignature(
        signature: String,
        from: DocumentationNode,
        refs: List<DocumentationNode>
    ): DocumentationNode {
        if (refs.size == 1) {
            val singleNode = refs.single()
            singleNode.owner?.let { owner ->
                singleNode.dropReferences { it.to == owner && it.kind == RefKind.Owner }
            }
            return singleNode
        }
        val groupNode = DocumentationNode(refs.first().name, Content.Empty, NodeKind.GroupNode)
        groupNode.appendTextNode(signature, NodeKind.Signature, RefKind.Detail)

        for (node in refs) {
            if (node != groupNode) {
                node.owner?.let { owner ->
                    node.dropReferences { it.to == owner && it.kind == RefKind.Owner }
                    from.dropReferences { it.to == node && it.kind == RefKind.Member }
                }
                groupNode.append(node, RefKind.Member)
            }
        }
        return groupNode
    }


    private fun mergeReferences(
        from: DocumentationNode,
        refs: List<DocumentationReference>
    ): List<DocumentationReference> {
        val allRefsToPackages = refs.map { it.to }
            .all { it.kind == NodeKind.Package }

        if (allRefsToPackages) {
            return mergePackageReferences(from, refs)
        }

        val (memberRefs, notMemberRefs) = splitReferencesByKind(refs, RefKind.Member)
        val mergedMembers = mergeMembers(from, memberRefs)

        return (mergedMembers + notMemberRefs).distinctBy {
            it.kind to it.to.name
        }
    }


    private fun updatePendingReferences(
        signature: String,
        nodeToUpdate: DocumentationNode
    ) {
        producedNodeRefGraph.references.forEach {
            it.lazyNodeFrom.update(signature, nodeToUpdate)
            it.lazyNodeTo.update(signature, nodeToUpdate)
        }
    }

    private fun NodeResolver.update(signature: String, nodeToUpdate: DocumentationNode) {
        when (this) {
            is NodeResolver.BySignature -> update(signature, nodeToUpdate)
            is NodeResolver.Exact -> update(signature, nodeToUpdate)
        }
    }

    private fun NodeResolver.BySignature.update(signature: String, nodeToUpdate: DocumentationNode) {
        if (signature == nodeToUpdate.name) {
            nodeMap = producedNodeRefGraph.nodeMapView
        }
    }

    private fun NodeResolver.Exact.update(signature: String, nodeToUpdate: DocumentationNode) {
        exactNode?.let { it ->
            val equalSignature =
                it.anyReference { ref -> ref.to.kind == NodeKind.Signature && ref.to.name == signature }

            if (equalSignature) {
                exactNode = nodeToUpdate
            }
        }
    }

    fun merge(): DocumentationModule {
        val refs = documentationModules.flatMap {
            it.allReferences()
        }
        val mergedDocumentationModule = DocumentationModule(
            name = documentationModules.first().name,
            nodeRefGraph = producedNodeRefGraph
        )

        mergeReferences(mergedDocumentationModule, refs)

        return mergedDocumentationModule
    }


}