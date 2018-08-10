package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

open class GlobalArguments(parser: DokkaArgumentsParser) : DokkaConfiguration {
    override val outputDir: String by parser.defaultSingleOption(
        listOf("-output"),
        "Output directory path",
        "")

    override val format: String by parser.defaultSingleOption(
        listOf("-format"),
        "Output format (text, html, markdown, jekyll, kotlin-website)",
        "")

    override val generateIndexPages: Boolean by parser.singleBooleanFlag(
        listOf("-generateIndexPages"),
        "Generate index page"
    )

    override val cacheRoot: String? by parser.defaultSingleOption(
        listOf("-cacheRoot"),
        "Path to cache folder, or 'default' to use ~/.cache/dokka, if not provided caching is disabled",
        null)

    override val impliedPlatforms: List<String> by parser.repeatableOption(
        listOf("-impliedPlatforms"),
        "List of implied platforms (comma-separated)"
    ) { it }

    override val passesConfigurations: List<Arguments> by parser.repeatableFlag(
        listOf("-pass"),
        "Single dokka pass"
    ) {
        Arguments(parser)
    }
}

class Arguments(val parser: DokkaArgumentsParser) : DokkaConfiguration.PassConfiguration {
    override val moduleName: String by parser.defaultSingleOption(
        listOf("-module"),
        "Name of the documentation module",
        "")

    override val classpath: List<String> by parser.repeatableOption(
        listOf("-classpath"),
        "Classpath for symbol resolution"
    ) { it }

    override val sourceRoots: List<DokkaConfiguration.SourceRoot> by parser.repeatableOption(
        listOf("-src"),
        "Source file or directory (allows many paths separated by the system path separator)"
    ) { SourceRootImpl.parseSourceRoot(it) }

    override val samples: List<String> by parser.repeatableOption(
        listOf("-samples"),
        "Source root for samples"
    ) { it }

    override val includes: List<String> by parser.repeatableOption(
        listOf("-include"),
        "Markdown files to load (allows many paths separated by the system path separator)"
    ) { it }

    override val includeNonPublic: Boolean by parser.singleBooleanFlag(
        listOf("-includeNonPublic"),
        "Include non public")

    override val includeRootPackage: Boolean by parser.singleBooleanFlag(
        listOf("-includeRootPackage"),
        "Include non public")

    override val reportUndocumented: Boolean by parser.singleBooleanFlag(
        listOf("-reportUndocumented"),
        "Include non public")

    override val skipEmptyPackages: Boolean by parser.singleBooleanFlag(
        listOf("-skipEmptyPackages"),
        "Include non public")

    override val skipDeprecated: Boolean by parser.singleBooleanFlag(
        listOf("-skipDeprecated"),
        "Include non public")

    override val jdkVersion: Int by parser.singleOption(
        listOf("jdkVersion"),
        "Version of JDK to use for linking to JDK JavaDoc",
        { it.toInt() },
        { 6 }
    )

    override val languageVersion: String? by parser.defaultSingleOption(
        listOf("-languageVersion"),
        "Language Version to pass to Kotlin Analysis",
        null)

    override val apiVersion: String? by parser.defaultSingleOption(
        listOf("-apiVesion"),
        "Kotlin Api Version to pass to Kotlin Analysis",
        null
    )

    override val noStdlibLink: Boolean by parser.singleBooleanFlag(
        listOf("-noStdlibLink"),
        "Disable documentation link to stdlib")

    override val noJdkLink: Boolean by parser.singleBooleanFlag(
        listOf("-noJdkLink"),
        "Disable documentation link to stdlib")

    override val suppressedFiles: List<String> by parser.repeatableOption(
        listOf("-suppresedFiles"),
        "",
        { it }
    )

    override val collectInheritedExtensionsFromLibraries: Boolean by parser.singleBooleanFlag(
        listOf("-collectInheritedExtensionsFromLibraries"),
        "Search for applicable extensions in libraries")

    override val analysisPlatform: Platform by parser.singleOption(
        listOf("-analysisPlatform"),
        "Platform for analysis",
        { Platform.fromString(it) },
        { Platform.DEFAULT }
    )

    override val targets: List<String> by parser.repeatableOption(
        listOf("-targets"),
        "Generation targets",
        { it }
    )

    override val sourceLinks: List<DokkaConfiguration.SourceLinkDefinition>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val perPackageOptions: List<DokkaConfiguration.PackageOptions>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val externalDocumentationLinks: List<DokkaConfiguration.ExternalDocumentationLink>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
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


        val parser = DokkaArgumentsParser(args, ParseContext())
        val parseContext = parser.parseInto(::GlobalArguments)

        val configuration = parseContext

        if (configuration.format == "javadoc")
            startWithToolsJar(configuration)
        else
            entry(configuration)
    }
}



