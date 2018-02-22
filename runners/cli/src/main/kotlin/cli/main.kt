package org.jetbrains.dokka


import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

class DokkaArguments {
    @set:Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    var src: String = ""

    @set:Argument(value = "srcLink", description = "Mapping between a source directory and a Web site for browsing the code")
    var srcLink: String = ""

    @set:Argument(value = "include", description = "Markdown files to load (allows many paths separated by the system path separator)")
    var include: String = ""

    @set:Argument(value = "samples", description = "Source root for samples")
    var samples: String = ""

    @set:Argument(value = "output", description = "Output directory path")
    var outputDir: String = "out/doc/"

    @set:Argument(value = "format", description = "Output format (text, html, markdown, jekyll, kotlin-website)")
    var outputFormat: String = "html"

    @set:Argument(value = "module", description = "Name of the documentation module")
    var moduleName: String = ""

    @set:Argument(value = "classpath", description = "Classpath for symbol resolution")
    var classpath: String = ""

    @set:Argument(value = "nodeprecated", description = "Exclude deprecated members from documentation")
    var nodeprecated: Boolean = false

    @set:Argument(value = "jdkVersion", description = "Version of JDK to use for linking to JDK JavaDoc")
    var jdkVersion: Int = 6

    @set:Argument(value = "impliedPlatforms", description = "List of implied platforms (comma-separated)")
    var impliedPlatforms: String = ""

    @set:Argument(value = "packageOptions", description = "List of package options in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" ")
    var packageOptions: String = ""

    @set:Argument(value = "links", description = "External documentation links in format url^packageListUrl^^url2...")
    var links: String = ""

    @set:Argument(value = "noStdlibLink", description = "Disable documentation link to stdlib")
    var noStdlibLink: Boolean = false

    @set:Argument(value = "cacheRoot", description = "Path to cache folder, or 'default' to use ~/.cache/dokka, if not provided caching is disabled")
    var cacheRoot: String? = null

    @set:Argument(value = "languageVersion", description = "Language Version to pass to Kotlin Analysis")
    var languageVersion: String? = null

    @set:Argument(value = "apiVersion", description = "Kotlin Api Version to pass to Kotlin Analysis")
    var apiVersion: String? = null

}


object MainKt {

    fun parseLinks(links: String): List<ExternalDocumentationLink> {
        val (parsedLinks, parsedOfflineLinks) = links.split("^^")
                .map { it.split("^").map { it.trim() }.filter { it.isNotBlank() } }
                .filter { it.isNotEmpty() }
                .partition { it.size == 1 }

        return parsedLinks.map { (root) -> ExternalDocumentationLink.Builder(root).build() } +
                parsedOfflineLinks.map { (root, packageList) ->
                    val rootUrl = URL(root)
                    val packageListUrl =
                            try {
                                URL(packageList)
                            } catch (ex: MalformedURLException) {
                                File(packageList).toURI().toURL()
                            }
                    ExternalDocumentationLink.Builder(rootUrl, packageListUrl).build()
                }
    }

    @JvmStatic
    fun entry(args: Array<String>) {
        val arguments = DokkaArguments()
        val freeArgs: List<String> = Args.parse(arguments, args) ?: listOf()
        val sources = if (arguments.src.isNotEmpty()) arguments.src.split(File.pathSeparatorChar).toList() + freeArgs else freeArgs
        val samples = if (arguments.samples.isNotEmpty()) arguments.samples.split(File.pathSeparatorChar).toList() else listOf()
        val includes = if (arguments.include.isNotEmpty()) arguments.include.split(File.pathSeparatorChar).toList() else listOf()

        val sourceLinks = if (arguments.srcLink.isNotEmpty() && arguments.srcLink.contains("="))
            listOf(SourceLinkDefinitionImpl.parseSourceLinkDefinition(arguments.srcLink))
        else {
            if (arguments.srcLink.isNotEmpty()) {
                println("Warning: Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
            }
            listOf()
        }

        val classPath = arguments.classpath.split(File.pathSeparatorChar).toList()

        val documentationOptions = DocumentationOptions(
                arguments.outputDir.let { if (it.endsWith('/')) it else it + '/' },
                arguments.outputFormat,
                skipDeprecated = arguments.nodeprecated,
                sourceLinks = sourceLinks,
                impliedPlatforms = arguments.impliedPlatforms.split(','),
                perPackageOptions = parsePerPackageOptions(arguments.packageOptions),
                jdkVersion = arguments.jdkVersion,
                externalDocumentationLinks = parseLinks(arguments.links),
                noStdlibLink = arguments.noStdlibLink,
                cacheRoot = arguments.cacheRoot,
                languageVersion = arguments.languageVersion,
                apiVersion = arguments.apiVersion
        )

        val generator = DokkaGenerator(
                DokkaConsoleLogger,
                classPath,
                sources.map(SourceRootImpl.Companion::parseSourceRoot),
                samples,
                includes,
                arguments.moduleName,
                documentationOptions)

        generator.generate()
        DokkaConsoleLogger.report()
    }

    fun findToolsJar(): File {
        val javaHome = System.getProperty("java.home")
        val default = File(javaHome, "../lib/tools.jar")
        val mac = File(javaHome, "../Classes/classes.jar")
        when {
            default.exists() -> return default
            mac.exists() -> return mac
            else -> {
                throw Exception("tools.jar not found, please check it, also you can provide it manually, using -cp")
            }
        }
    }

    fun createClassLoaderWithTools(): ClassLoader {
        val toolsJar = findToolsJar().canonicalFile.toURI().toURL()
        val originalUrls = (javaClass.classLoader as? URLClassLoader)?.urLs
        val dokkaJar = javaClass.protectionDomain.codeSource.location
        val urls = if (originalUrls != null) arrayOf(toolsJar, *originalUrls) else arrayOf(toolsJar, dokkaJar)
        return URLClassLoader(urls, ClassLoader.getSystemClassLoader().parent)
    }

    fun startWithToolsJar(args: Array<String>) {
        try {
            javaClass.classLoader.loadClass("com.sun.tools.doclets.formats.html.HtmlDoclet")
            entry(args)
        } catch (e: ClassNotFoundException) {
            val classLoader = createClassLoaderWithTools()
            classLoader.loadClass("org.jetbrains.dokka.MainKt")
                    .methods.find { it.name == "entry" }!!
                    .invoke(null, args)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val arguments = DokkaArguments()
        Args.parse(arguments, args)

        if (arguments.outputFormat == "javadoc")
            startWithToolsJar(args)
        else
            entry(args)
    }
}



