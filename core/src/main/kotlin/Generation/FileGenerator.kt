package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.name.Named
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class FileGenerator @Inject constructor(@Named("outputDir") override val root: File) : NodeLocationAwareGenerator {

    @set:Inject(optional = true) var outlineService: OutlineFormatService? = null
    @set:Inject(optional = true) lateinit var formatService: FormatService
    @set:Inject(optional = true) lateinit var dokkaConfiguration: DokkaConfiguration
    @set:Inject(optional = true) var packageListService: PackageListService? = null

    private val createdFiles = mutableMapOf<File, List<String>>()

    private fun File.writeFileAndAssert(context: String, action: (File) -> Unit) {
        //TODO: there is a possible refactoring to drop FileLocation
        //TODO: aad File from API, Location#path.
        //TODO: turn [Location] into a final class,
        //TODO: Use [Location] all over the place without full
        //TODO: reference to the real target path,
        //TODO: it opens the way to safely track all files created
        //TODO: to make sure no files were overwritten by mistake
        //TODO: also, the NodeLocationAwareGenerator should be removed

        val writes = createdFiles.getOrDefault(this, listOf()) + context
        createdFiles[this] = writes
        if (writes.size > 1) {
            println("ERROR. An attempt to write ${this.relativeTo(root)} several times!")
            return
        }

        try {
            parentFile?.mkdirsOrFail()
            action(this)
        } catch (e : Throwable) {
            println("Failed to write $this. ${e.message}")
            e.printStackTrace()
        }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !exists()) {
            throw IOException("Failed to create directory $this")
        }
    }

    override fun location(node: DocumentationNode): FileLocation {
        return FileLocation(fileForNode(node, formatService.linkExtension))
    }

    private fun fileForNode(node: DocumentationNode, extension: String = ""): File {
        return File(root, relativePathToNode(node)).appendExtension(extension)
    }

    private fun locationWithoutExtension(node: DocumentationNode): FileLocation {
        return FileLocation(fileForNode(node))
    }

    override fun buildPages(nodes: Iterable<DocumentationNode>) {

        for ((file, items) in nodes.groupBy { fileForNode(it, formatService.extension) }) {
            file.writeFileAndAssert("pages") { it ->
                it.writeText(formatService.format(location(items.first()), items))
            }

            buildPages(items.filterNot { it.kind == NodeKind.AllTypes }.flatMap { it.members })
        }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        val outlineService = this.outlineService ?: return
        for ((location, items) in nodes.groupBy { locationWithoutExtension(it) }) {
            outlineService.getOutlineFileName(location).writeFileAndAssert("outlines") { file ->
                file.writeText(outlineService.formatOutline(location, items))
            }
        }
    }

    override fun buildSupportFiles() {
        formatService.enumerateSupportFiles { resource, targetPath ->
            File(root, relativePathToNode(listOf(targetPath), false)).writeFileAndAssert("support files") { file ->
                file.outputStream().use {
                    javaClass.getResourceAsStream(resource).copyTo(it)
                }
            }
        }
    }

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        if (packageListService == null) return

        for (module in nodes) {

            val moduleRoot = location(module).file.parentFile
            val packageListFile = File(moduleRoot, "package-list")

            val text = "\$dokka.format:${dokkaConfiguration.format}\n" + packageListService!!.formatPackageList(module as DocumentationModule)

            packageListFile.writeFileAndAssert("packages-list") { file ->
                file.writeText(text)
            }
        }
    }
}
