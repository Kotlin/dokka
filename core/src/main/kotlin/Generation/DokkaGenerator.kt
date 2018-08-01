package org.jetbrains.dokka

import com.google.inject.Guice
import com.google.inject.Injector
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.dokka.Utilities.DokkaAnalysisModule
import org.jetbrains.dokka.Utilities.DokkaOutputModule
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.system.measureTimeMillis

class DokkaGenerator(val dokkaConfiguration: DokkaConfiguration,
                     val logger: DokkaLogger) {

    private val documentationModules: MutableList<DocumentationModule> = mutableListOf()


    fun generate() = with(dokkaConfiguration) {


        for (pass in passesConfigurations) {
            val documentationModule = DocumentationModule(pass.moduleName)
            appendSourceModule(pass, documentationModule)
            documentationModules.add(documentationModule)
        }

        val totalDocumentationModule = DocumentationMerger(documentationModules).merge()
        totalDocumentationModule.prepareForGeneration(dokkaConfiguration)

        val timeBuild = measureTimeMillis {
            logger.info("Generating pages... ")
            val outputInjector = Guice.createInjector(DokkaOutputModule(dokkaConfiguration, logger))
            outputInjector.getInstance(Generator::class.java).buildAll(totalDocumentationModule)
        }
        logger.info("done in ${timeBuild / 1000} secs")
    }

    private fun appendSourceModule(
        passConfiguration: DokkaConfiguration.PassConfiguration,
        documentationModule: DocumentationModule
    ) = with(passConfiguration) {

        val sourcePaths = passConfiguration.sourceRoots.map { it.path }
        val environment = createAnalysisEnvironment(sourcePaths, passConfiguration)

        logger.info("Module: $moduleName")
        logger.info("Output: ${File(dokkaConfiguration.outputDir)}")
        logger.info("Sources: ${sourcePaths.joinToString()}")
        logger.info("Classpath: ${environment.classpath.joinToString()}")

        logger.info("Analysing sources and libraries... ")
        val startAnalyse = System.currentTimeMillis()

        val defaultPlatformAsList =  listOf(passConfiguration.analysisPlatform.key)
        val defaultPlatformsProvider = object : DefaultPlatformsProvider {
            override fun getDefaultPlatforms(descriptor: DeclarationDescriptor): List<String> {
                val containingFilePath = descriptor.sourcePsi()?.containingFile?.virtualFile?.canonicalPath
                        ?.let { File(it).absolutePath }
                val sourceRoot = containingFilePath?.let { path -> sourceRoots.find { path.startsWith(it.path) } }
                return sourceRoot?.platforms ?: defaultPlatformAsList
            }
        }

        val injector = Guice.createInjector(
                DokkaAnalysisModule(environment, dokkaConfiguration, defaultPlatformsProvider, documentationModule.nodeRefGraph, passConfiguration, logger))

        buildDocumentationModule(injector, documentationModule, { isNotSample(it, passConfiguration.samples) }, includes)
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

   fun createAnalysisEnvironment(sourcePaths: List<String>, passConfiguration: DokkaConfiguration.PassConfiguration): AnalysisEnvironment {
        val environment = AnalysisEnvironment(DokkaMessageCollector(logger), passConfiguration.analysisPlatform)

        environment.apply {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            //   addClasspath(PathUtil.getKotlinPathsForCompiler().getRuntimePath())
            for (element in passConfiguration.classpath) {
                addClasspath(File(element))
            }

            addSources(sourcePaths)
            addSources(passConfiguration.samples)

            loadLanguageVersionSettings(passConfiguration.languageVersion, passConfiguration.apiVersion)
        }

        return environment
    }

   fun isNotSample(file: PsiFile, samples: List<String>): Boolean {
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