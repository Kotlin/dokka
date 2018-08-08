package org.jetbrains.dokka

import kotlinx.cli.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

data class Arguments(
    override var moduleName: String = "",
    override var classpath: MutableList<String> = mutableListOf(),
    override var sourceRoots: MutableList<DokkaConfiguration.SourceRoot> = mutableListOf(),
    override var samples: MutableList<String> = mutableListOf(),
    override var includes: MutableList<String> = mutableListOf(),
    override var includeNonPublic: Boolean = false,
    override var includeRootPackage: Boolean = false,
    override var reportUndocumented: Boolean = false,
    override var skipEmptyPackages: Boolean = false,
    override var skipDeprecated: Boolean = false,
    override var jdkVersion: Int = 6,
    override var sourceLinks: List<DokkaConfiguration.SourceLinkDefinition> = listOf(),
    override var perPackageOptions: List<DokkaConfiguration.PackageOptions> = listOf(),
    override var externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink> = listOf(),
    override var languageVersion: String? = "",
    override var apiVersion: String? = "",
    override var noStdlibLink: Boolean = false,
    override var noJdkLink: Boolean = false,
    override var suppressedFiles: MutableList<String> = mutableListOf(),
    override var collectInheritedExtensionsFromLibraries: Boolean = false,
    override var analysisPlatform: Platform = Platform.DEFAULT,
    override var targets: MutableList<String> = mutableListOf(),
    var rawPerPackageOptions: MutableList<String> = mutableListOf()
) : DokkaConfiguration.PassConfiguration


data class GlobalArguments(
    override var outputDir: String = "",
    override var format: String = "",
    override var generateIndexPages: Boolean = false,
    override var cacheRoot: String? = null,
    override var passesConfigurations: List<Arguments> = listOf(),
    override var impliedPlatforms: MutableList<String> = mutableListOf()
) : DokkaConfiguration

class DokkaArgumentsParser {
    companion object {
        fun CommandLineInterface.registerSingleAction(
            keys: List<String>,
            help: String,
            invoke: (String) -> Unit
        ) = registerAction(
            object : FlagActionBase(keys, help) {
                override fun invoke(arguments: ListIterator<String>) {
                    if (arguments.hasNext()) {
                        val msg = arguments.next()
                        invoke(msg)
                    }
                }

                override fun invoke() {
                    error("should be never called")
                }
            }

        )

        fun CommandLineInterface.registerRepeatingAction(
            keys: List<String>,
            help: String,
            invoke: (String) -> Unit
        ) = registerAction(
            object : FlagActionBase(keys, help) {
                override fun invoke(arguments: ListIterator<String>) {
                    while (arguments.hasNext()) {
                        val message = arguments.next()

                        if (this@registerRepeatingAction.getFlagAction(message) != null) {
                            arguments.previous()
                            break
                        }
                        invoke(message)
                    }

                }

                override fun invoke() {
                    error("should be never called")
                }
            }

        )

    }

    val cli = CommandLineInterface("dokka")
    val globalArguments = GlobalArguments()

    init {
        cli.flagAction(
            listOf("-pass"),
            "Single dokka pass"
        ) {
            globalArguments.passesConfigurations += Arguments()
        }

        cli.registerRepeatingAction(
            listOf("-src"),
            "Source file or directory (allows many paths separated by the system path separator)"
        ) {
            globalArguments.passesConfigurations.last().sourceRoots.add(SourceRootImpl.parseSourceRoot(it))
        }

        cli.registerRepeatingAction(
            listOf("-srcLink"),
            "Mapping between a source directory and a Web site for browsing the code"
        ) {
            println(it)
        }

        cli.registerRepeatingAction(
            listOf("-include"),
            "Markdown files to load (allows many paths separated by the system path separator)"
        ) {
            globalArguments.passesConfigurations.last().includes.add(it)
        }

        cli.registerRepeatingAction(
            listOf("-samples"),
            "Source root for samples"
        ) {
            globalArguments.passesConfigurations.last().samples.add(it)
        }

        cli.registerSingleAction(
            listOf("-output"),
            "Output directory path"
        ) {
            globalArguments.outputDir = it
        }

        cli.registerSingleAction(
            listOf("-format"),
            "Output format (text, html, markdown, jekyll, kotlin-website)"
        ) {
            globalArguments.format = it
        }

        cli.registerSingleAction(
            listOf("-module"),
            "Name of the documentation module"
        ) {
            globalArguments.passesConfigurations.last().moduleName = it
        }

        cli.registerRepeatingAction(
            listOf("-classpath"),
            "Classpath for symbol resolution"
        ) {
            globalArguments.passesConfigurations.last().classpath.add(it)
        }

        cli.flagAction(
            listOf("-nodeprecacted"),
            "Exclude deprecated members from documentation"
        ) {
            globalArguments.passesConfigurations.last().skipDeprecated = true
        }

        cli.registerSingleAction(
            listOf("jdkVersion"),
            "Version of JDK to use for linking to JDK JavaDoc"
        ) {
            globalArguments.passesConfigurations.last().jdkVersion = Integer.parseInt(it)
        }

        cli.registerRepeatingAction(
            listOf("-impliedPlatforms"),
            "List of implied platforms (comma-separated)"
        ) {
            globalArguments.impliedPlatforms.add(it)
        }

        cli.registerSingleAction(
            listOf("-pckageOptions"),
            "List of package passConfiguration in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" "
        ) {
            globalArguments.passesConfigurations.last().perPackageOptions = parsePerPackageOptions(it)
        }

        cli.registerSingleAction(
            listOf("links"),
            "External documentation links in format url^packageListUrl^^url2..."
        ) {
            globalArguments.passesConfigurations.last().externalDocumentationLinks = MainKt.parseLinks(it)
        }

        cli.flagAction(
            listOf("-noStdlibLink"),
            "Disable documentation link to stdlib"
        ) {
            globalArguments.passesConfigurations.last().noStdlibLink = true
        }

        cli.flagAction(
            listOf("-noJdkLink"),
            "Disable documentation link to jdk"
        ) {
            globalArguments.passesConfigurations.last().noJdkLink = true
        }

        cli.registerSingleAction(
            listOf("-cacheRoot"),
            "Path to cache folder, or 'default' to use ~/.cache/dokka, if not provided caching is disabled"
        ) {
            globalArguments.cacheRoot = it
        }

        cli.registerSingleAction(
            listOf("-languageVersion"),
            "Language Version to pass to Kotlin Analysis"
        ) {
            globalArguments.passesConfigurations.last().languageVersion = it
        }

        cli.registerSingleAction(
            listOf("-apiVesion"),
            "Kotlin Api Version to pass to Kotlin Analysis"
        ) {
            globalArguments.passesConfigurations.last().apiVersion = it
        }

        cli.flagAction(
            listOf("-collectInheritedExtensionsFromLibraries"),
            "Search for applicable extensions in libraries"
        ) {
            globalArguments.passesConfigurations.last().collectInheritedExtensionsFromLibraries = true
        }

    }

    fun parse(args: Array<String>): DokkaConfiguration {
        cli.parseArgs(*args)

        return globalArguments
    }
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
    fun entry(configuration: DokkaConfiguration) {
        val generator = DokkaGenerator(configuration, DokkaConsoleLogger)
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

    fun startWithToolsJar(configuration: DokkaConfiguration) {
        try {
            javaClass.classLoader.loadClass("com.sun.tools.doclets.formats.html.HtmlDoclet")
            entry(configuration)
        } catch (e: ClassNotFoundException) {
            val classLoader = createClassLoaderWithTools()
            classLoader.loadClass("org.jetbrains.dokka.MainKt")
                    .methods.find { it.name == "entry" }!!
                    .invoke(null, configuration)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {


        val dokkaArgumentsParser = DokkaArgumentsParser()
        val configuration = dokkaArgumentsParser.parse(args)

        if (configuration.format == "javadoc")
            startWithToolsJar(configuration)
        else
            entry(configuration)
    }
}



