package org.jetbrains.dokka

import com.google.inject.Inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class FileGenerator @Inject constructor(val locationService: FileLocationService) : Generator {

    @set:Inject(optional = true) var outlineService: OutlineFormatService? = null
    @set:Inject(optional = true) lateinit var formatService: FormatService
    @set:Inject(optional = true) lateinit var options: DocumentationOptions
    @set:Inject(optional = true) var packageListService: PackageListService? = null

    override fun buildPages(nodes: Iterable<DocumentationNode>) {
        val specificLocationService = locationService.withExtension(formatService.extension)

        for ((location, items) in nodes.groupBy { specificLocationService.location(it) }) {
            val file = location.file
            file.parentFile?.mkdirsOrFail()
            FileOutputStream(file).use {
                OutputStreamWriter(it, Charsets.UTF_8).use {
                    it.write(formatService.format(location, items))
                }
            }
            buildPages(items.flatMap { it.members })
        }
    }

    override fun buildOutlines(nodes: Iterable<DocumentationNode>) {
        val outlineService = this.outlineService ?: return
        for ((location, items) in nodes.groupBy { locationService.location(it) }) {
            val file = outlineService.getOutlineFileName(location)
            file.parentFile?.mkdirsOrFail()
            FileOutputStream(file).use {
                OutputStreamWriter(it, Charsets.UTF_8).use {
                    it.write(outlineService.formatOutline(location, items))
                }
            }
        }
    }

    override fun buildSupportFiles() {
        formatService.enumerateSupportFiles { resource, targetPath ->
            FileOutputStream(locationService.location(listOf(targetPath), false).file).use {
                javaClass.getResourceAsStream(resource).copyTo(it)
            }
        }
    }

    override fun buildPackageList(nodes: Iterable<DocumentationNode>) {
        if (packageListService == null) return

        for (module in nodes) {

            val moduleRoot = locationService.location(module).file.parentFile
            val packageListFile = File(moduleRoot, "package-list")

            packageListFile.writeText("\$dokka.format:${options.outputFormat}\n" +
                    packageListService!!.formatPackageList(module as DocumentationModule))
        }

    }

}

private fun File.mkdirsOrFail() {
    if (!mkdirs() && !exists()) {
        throw IOException("Failed to create directory $this")
    }
}