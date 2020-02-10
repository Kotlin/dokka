package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import org.jetbrains.dokka.Utilities.defaultLinks
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

open class GlobalArguments(parser: DokkaArgumentsParser) : DokkaConfiguration {
    override val outputDir: String by parser.stringOption(
        listOf("-output"),
        "Output directory path",
        "")

    override val format: String by parser.stringOption(
        listOf("-format"),
        "Output format (text, html, markdown, jekyll, kotlin-website)",
        "")

    override val generateIndexPages: Boolean by parser.singleFlag(
        listOf("-generateIndexPages"),
        "Generate index page"
    )

    override val cacheRoot: String? by parser.stringOption(
        listOf("-cacheRoot"),
        "Path to cache folder, or 'default' to use ~/.cache/dokka, if not provided caching is disabled",
        null)

    override val impliedPlatforms: List<String> = emptyList()

    override val passesConfigurations: List<Arguments> by parser.repeatableFlag(
        listOf("-pass"),
        "Single dokka pass"
    ) {
        Arguments(parser)
    }
}

class Arguments(val parser: DokkaArgumentsParser) : DokkaConfiguration.PassConfiguration {
    override val moduleName: String by parser.stringOption(
        listOf("-module"),
        "Name of the documentation module",
        "")

    override val classpath: List<String> by parser.repeatableOption(
        listOf("-classpath"),
        "Classpath for symbol resolution"
    )

    override val sourceRoots: List<DokkaConfiguration.SourceRoot> by parser.repeatableOption(
        listOf("-src"),
        "Source file or directory (allows many paths separated by the system path separator)"
    ) { SourceRootImpl(it) }

    override val samples: List<String> by parser.repeatableOption(
        listOf("-sample"),
        "Source root for samples"
    )

    override val includes: List<String> by parser.repeatableOption(
        listOf("-include"),
        "Markdown files to load (allows many paths separated by the system path separator)"
    )

    override val includeNonPublic: Boolean by parser.singleFlag(
        listOf("-includeNonPublic"),
        "Include non public")

    override val includeRootPackage: Boolean by parser.singleFlag(
        listOf("-includeRootPackage"),
        "Include root package")

    override val reportUndocumented: Boolean by parser.singleFlag(
        listOf("-reportUndocumented"),
        "Report undocumented members")

    override val skipEmptyPackages: Boolean by parser.singleFlag(
        listOf("-skipEmptyPackages"),
        "Do not create index pages for empty packages")

    override val skipDeprecated: Boolean by parser.singleFlag(
        listOf("-skipDeprecated"),
        "Do not output deprecated members")

    override val jdkVersion: Int by parser.singleOption(
        listOf("-jdkVersion"),
        "Version of JDK to use for linking to JDK JavaDoc",
        { it.toInt() },
        { 6 }
    )

    override val languageVersion: String? by parser.stringOption(
        listOf("-languageVersion"),
        "Language Version to pass to Kotlin Analysis",
        null)

    override val apiVersion: String? by parser.stringOption(
        listOf("-apiVersion"),
        "Kotlin Api Version to pass to Kotlin Analysis",
        null
    )

    override val noStdlibLink: Boolean by parser.singleFlag(
        listOf("-noStdlibLink"),
        "Disable documentation link to stdlib")

    override val noJdkLink: Boolean by parser.singleFlag(
        listOf("-noJdkLink"),
        "Disable documentation link to JDK")

    override val suppressedFiles: List<String> by parser.repeatableOption(
        listOf("-suppressedFile"),
        ""
    )

    override val sinceKotlin: String? by parser.stringOption(
        listOf("-sinceKotlin"),
        "Kotlin Api version to use as base version, if none specified",
        null
    )

    override val collectInheritedExtensionsFromLibraries: Boolean by parser.singleFlag(
        listOf("-collectInheritedExtensionsFromLibraries"),
        "Search for applicable extensions in libraries")

    override val analysisPlatform: Platform by parser.singleOption(
        listOf("-analysisPlatform"),
        "Platform for analysis",
        { Platform.fromString(it) },
        { Platform.DEFAULT }
    )

    override val targets: List<String> by parser.repeatableOption(
        listOf("-target"),
        "Generation targets"
    )

    override val perPackageOptions: MutableList<DokkaConfiguration.PackageOptions> by parser.singleOption(
        listOf("-packageOptions"),
        "List of package passConfiguration in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" ",
        { parsePerPackageOptions(it).toMutableList() },
        { mutableListOf() }
    )

    override val externalDocumentationLinks: MutableList<DokkaConfiguration.ExternalDocumentationLink> by parser.singleOption(
        listOf("-links"),
        "External documentation links in format url^packageListUrl^^url2...",
        { MainKt.parseLinks(it).toMutableList() },
        { mutableListOf() }
    )

    override val sourceLinks: MutableList<DokkaConfiguration.SourceLinkDefinition> by parser.repeatableOption(
        listOf("-srcLink"),
        "Mapping between a source directory and a Web site for browsing the code"
    ) {
        if (it.isNotEmpty() && it.contains("="))
            SourceLinkDefinitionImpl.parseSourceLinkDefinition(it)
        else {
            throw IllegalArgumentException("Warning: Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
        }
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

    fun createConfiguration(args: Array<String>): GlobalArguments {
        val parseContext = ParseContext()
        val parser = DokkaArgumentsParser(args, parseContext)
        val configuration = GlobalArguments(parser)

        parseContext.cli.singleAction(
            listOf("-globalPackageOptions"),
            "List of package passConfiguration in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" "
        ) { link ->
            configuration.passesConfigurations.all { it.perPackageOptions.addAll(parsePerPackageOptions(link)) }
        }

        parseContext.cli.singleAction(
            listOf("-globalLinks"),
            "External documentation links in format url^packageListUrl^^url2..."
        ) { link ->
            configuration.passesConfigurations.all { it.externalDocumentationLinks.addAll(parseLinks(link)) }
        }

        parseContext.cli.repeatingAction(
            listOf("-globalSrcLink"),
            "Mapping between a source directory and a Web site for browsing the code"
        ) {
            val newSourceLinks = if (it.isNotEmpty() && it.contains("="))
                listOf(SourceLinkDefinitionImpl.parseSourceLinkDefinition(it))
            else {
                if (it.isNotEmpty()) {
                    println("Warning: Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
                }
                listOf()
            }

            configuration.passesConfigurations.all { it.sourceLinks.addAll(newSourceLinks) }
        }

        parser.parseInto(configuration)
        return configuration
    }

    fun GlobalArguments.addDefaultLinks() = passesConfigurations.forEach { it.externalDocumentationLinks += it.defaultLinks() }

    @JvmStatic
    fun main(args: Array<String>) {
        val configuration = createConfiguration(args).apply { addDefaultLinks() }
        if (configuration.format.toLowerCase() == "javadoc")
            startWithToolsJar(configuration)
        else
            entry(configuration)
    }
}



