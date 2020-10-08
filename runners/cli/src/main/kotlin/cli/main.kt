package org.jetbrains.dokka

import kotlinx.cli.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.cast
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Paths

class GlobalArguments(args: Array<String>) : DokkaConfiguration {

    val parser = ArgParser("globalArguments", prefixStyle = ArgParser.OptionPrefixStyle.JVM)

    val json: String? by parser.argument(ArgType.String, description = "Json file name").optional()

    private val _moduleName = parser.option(
        ArgType.String,
        description = "Name of the documentation module",
        fullName = "moduleName"
    ).default(DokkaDefaults.moduleName)

    override val moduleName: String by _moduleName

    override val moduleVersion by parser.option(
        ArgType.String,
        description = "Documentation version",
        fullName = "moduleVersion"
    )

    override val outputDir by parser.option(ArgTypeFile, description = "Output directory path")
        .default(DokkaDefaults.outputDir)

    override val cacheRoot by parser.option(
        ArgTypeFile,
        description = "Path to cache folder, or 'default' to use ~/.cache/dokka, if not provided caching is disabled"
    )

    override val sourceSets by parser.option(
        ArgTypeArgument(_moduleName),
        description = "Single dokka source set",
        fullName = "sourceSet"
    ).multiple()

    override val pluginsConfiguration by parser.option(
        ArgTypePlugin,
        description = "Configuration for plugins in format fqPluginName=json^^fqPluginName=json..."
    ).delimiter("^^")

    override val pluginsClasspath by parser.option(
        ArgTypeFile,
        fullName = "pluginsClasspath",
        description = "List of jars with dokka plugins (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    override val offlineMode by parser.option(
        ArgType.Boolean,
        "Offline mode (do not download package lists from the Internet)"
    ).default(DokkaDefaults.offlineMode)

    override val failOnWarning by parser.option(
        ArgType.Boolean,
        "Throw an exception if the generation exited with warnings"
    ).default(DokkaDefaults.failOnWarning)

    override val delayTemplateSubstitution by parser.option(
        ArgType.Boolean,
        description = "Delay substitution of some elements (usefull for incremental builds of multimoule projects)"
    ).default(DokkaDefaults.delayTemplateSubstitution)

    val globalPackageOptions by parser.option(
        ArgType.String,
        description = "List of package source sets in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" "
    ).delimiter(";")

    val globalLinks by parser.option(
        ArgType.String,
        description = "External documentation links in format url^packageListUrl^^url2..."
    ).delimiter("^^")

    val globalSrcLink by parser.option(
        ArgType.String,
        description = "Mapping between a source directory and a Web site for browsing the code (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val helpSourceSet by parser.option(
        ArgTypeHelpSourceSet(_moduleName),
        description = "Prints help for single -sourceSet"
    )

    override val modules: List<DokkaConfiguration.DokkaModuleDescription> = emptyList()

    init {
        parser.parse(args)

        sourceSets.forEach {
            it.perPackageOptions.cast<MutableList<DokkaConfiguration.PackageOptions>>()
                .addAll(parsePerPackageOptions(globalPackageOptions))
        }

        sourceSets.forEach {
            it.externalDocumentationLinks.cast<MutableSet<ExternalDocumentationLink>>().addAll(parseLinks(globalLinks))
        }

        globalSrcLink.forEach {
            if (it.isNotEmpty() && it.contains("="))
                sourceSets.all { sourceSet ->
                    sourceSet.sourceLinks.cast<MutableSet<SourceLinkDefinitionImpl>>()
                        .add(SourceLinkDefinitionImpl.parseSourceLinkDefinition(it))
                }
            else {
                DokkaConsoleLogger.warn("Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
            }
        }

        sourceSets.forEach {
            it.externalDocumentationLinks.cast<MutableSet<ExternalDocumentationLink>>().addAll(defaultLinks(it))
        }
    }
}

private fun parseSourceSet(moduleName: String, args: Array<String>): DokkaConfiguration.DokkaSourceSet {

    val parser = ArgParser("sourceSet", prefixStyle = ArgParser.OptionPrefixStyle.JVM)

    val sourceSetName by parser.option(
        ArgType.String,
        description = "Name of the source set"
    ).default("main")

    val displayName by parser.option(
        ArgType.String,
        description = "Displayed name of the source set"
    ).default(DokkaDefaults.sourceSetDisplayName)

    val classpath by parser.option(
        ArgTypeFile,
        description = "Classpath for symbol resolution (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val sourceRoots by parser.option(
        ArgTypeFile,
        description = "Source file or directory (allows many paths separated by the semicolon `;`)",
        fullName = "src"
    ).delimiter(";")

    val dependentSourceSets by parser.option(
        ArgType.String,
        description = "Names of dependent source sets in format \"moduleName/sourceSetName\" (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val samples by parser.option(
        ArgTypeFile,
        description = "Source root for samples (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val includes by parser.option(
        ArgTypeFile,
        description = "Markdown files to load (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val includeNonPublic: Boolean by parser.option(ArgType.Boolean, description = "Include non public")
        .default(DokkaDefaults.includeNonPublic)

    val reportUndocumented by parser.option(ArgType.Boolean, description = "Report undocumented members")
        .default(DokkaDefaults.reportUndocumented)

    val skipEmptyPackages by parser.option(
        ArgType.Boolean,
        description = "Do not create index pages for empty packages"
    ).default(DokkaDefaults.skipEmptyPackages)

    val skipDeprecated by parser.option(ArgType.Boolean, description = "Do not output deprecated members")
        .default(DokkaDefaults.skipDeprecated)

    val jdkVersion by parser.option(
        ArgType.Int,
        description = "Version of JDK to use for linking to JDK JavaDoc"
    ).default(DokkaDefaults.jdkVersion)

    val languageVersion by parser.option(
        ArgType.String,
        description = "Language Version to pass to Kotlin analysis"
    )

    val apiVersion by parser.option(
        ArgType.String,
        description = "Kotlin Api Version to pass to Kotlin analysis"
    )

    val noStdlibLink by parser.option(ArgType.Boolean, description = "Disable documentation link to stdlib")
        .default(DokkaDefaults.noStdlibLink)

    val noJdkLink by parser.option(ArgType.Boolean, description = "Disable documentation link to JDK")
        .default(DokkaDefaults.noJdkLink)

    val suppressedFiles by parser.option(
        ArgTypeFile,
        description = "Paths to files to be suppressed (allows many paths separated by the semicolon `;`)"
    ).delimiter(";")

    val analysisPlatform: Platform by parser.option(
        ArgTypePlatform,
        description = "Platform for analysis"
    ).default(DokkaDefaults.analysisPlatform)

    val perPackageOptions by parser.option(
        ArgType.String,
        description = "List of package source set configuration in format \"prefix,-deprecated,-privateApi,+warnUndocumented,+suppress;...\" "
    ).delimiter(";")

    val externalDocumentationLinks by parser.option(
        ArgType.String,
        description = "External documentation links in format url^packageListUrl^^url2..."
    ).delimiter("^^")

    val sourceLinks by parser.option(
        ArgTypeSourceLinkDefinition,
        description = "Mapping between a source directory and a Web site for browsing the code (allows many paths separated by the semicolon `;`)",
        fullName = "srcLink"
    ).delimiter(";")

    parser.parse(args)

    return object : DokkaConfiguration.DokkaSourceSet {
        override val displayName = displayName
        override val sourceSetID = DokkaSourceSetID(moduleName, sourceSetName)
        override val classpath = classpath.toMutableList()
        override val sourceRoots = sourceRoots.toMutableSet()
        override val dependentSourceSets = dependentSourceSets
            .map { dependentSourceSetName -> dependentSourceSetName.split('/').let { DokkaSourceSetID(it[0], it[1]) } }
            .toMutableSet()
        override val samples = samples.toMutableSet()
        override val includes = includes.toMutableSet()
        override val includeNonPublic = includeNonPublic
        override val reportUndocumented = reportUndocumented
        override val skipEmptyPackages = skipEmptyPackages
        override val skipDeprecated = skipDeprecated
        override val jdkVersion = jdkVersion
        override val sourceLinks = sourceLinks.toMutableSet()
        override val analysisPlatform = analysisPlatform
        override val perPackageOptions = parsePerPackageOptions(perPackageOptions)
        override val externalDocumentationLinks = parseLinks(externalDocumentationLinks).toMutableSet()
        override val languageVersion = languageVersion
        override val apiVersion = apiVersion
        override val noStdlibLink = noStdlibLink
        override val noJdkLink = noJdkLink
        override val suppressedFiles = suppressedFiles.toMutableSet()
    }
}

object ArgTypeFile : ArgType<File>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): File = Paths.get(value).toRealPath().toFile()
    override val description: kotlin.String
        get() = "{ String that points to file path }"
}

object ArgTypePlatform : ArgType<Platform>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): Platform = Platform.fromString(value)
    override val description: kotlin.String
        get() = "{ String that represents platform }"
}

object ArgTypePlugin : ArgType<DokkaConfiguration.PluginConfiguration>(true) {
    override fun convert(
        value: kotlin.String,
        name: kotlin.String
    ): DokkaConfiguration.PluginConfiguration {
        return value.split("=").let {
            PluginConfigurationImpl(
                fqPluginName = it[0],
                serializationFormat = DokkaConfiguration.SerializationFormat.JSON,
                values = it[1]
            )
        }
    }

    override val description: kotlin.String
        get() = "{ String fqName=json, remember to escape `\"` inside json }"
}

object ArgTypeSourceLinkDefinition : ArgType<DokkaConfiguration.SourceLinkDefinition>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): DokkaConfiguration.SourceLinkDefinition =
        if (value.isNotEmpty() && value.contains("="))
            SourceLinkDefinitionImpl.parseSourceLinkDefinition(value)
        else {
            throw IllegalArgumentException("Warning: Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
        }

    override val description: kotlin.String
        get() = "{ String that represent source links }"
}

data class ArgTypeArgument(val moduleName: CLIEntity<kotlin.String>) :
    ArgType<DokkaConfiguration.DokkaSourceSet>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): DokkaConfiguration.DokkaSourceSet =
        parseSourceSet(moduleName.value, value.split(" ").filter { it.isNotBlank() }.toTypedArray())

    override val description: kotlin.String
        get() = ""
}

// Workaround for printing nested parsers help
data class ArgTypeHelpSourceSet(val moduleName: CLIEntity<kotlin.String>) : ArgType<Any>(false) {
    override fun convert(value: kotlin.String, name: kotlin.String): Any = Any().also {
        parseSourceSet(moduleName.value, arrayOf("-h"))
    }

    override val description: kotlin.String
        get() = ""
}

@OptIn(ExperimentalStdlibApi::class)
fun defaultLinks(config: DokkaConfiguration.DokkaSourceSet): MutableList<ExternalDocumentationLink> =
    buildList<ExternalDocumentationLink> {
        if (!config.noJdkLink) {
            add(ExternalDocumentationLink.jdk(config.jdkVersion))
        }

        if (!config.noStdlibLink) {
            add(ExternalDocumentationLink.kotlinStdlib())
        }
    }.toMutableList()


fun parseLinks(links: List<String>): List<ExternalDocumentationLink> {
    val (parsedLinks, parsedOfflineLinks) = links
        .map { it.split("^").map { it.trim() }.filter { it.isNotBlank() } }
        .filter { it.isNotEmpty() }
        .partition { it.size == 1 }

    return parsedLinks.map { (root) -> ExternalDocumentationLink(root) } +
            parsedOfflineLinks.map { (root, packageList) ->
                val rootUrl = URL(root)
                val packageListUrl =
                    try {
                        URL(packageList)
                    } catch (ex: MalformedURLException) {
                        File(packageList).toURI().toURL()
                    }
                ExternalDocumentationLink(rootUrl, packageListUrl)
            }
}

fun main(args: Array<String>) {
    val globalArguments = GlobalArguments(args)
    val configuration = if (globalArguments.json != null)
        DokkaConfigurationImpl(
            Paths.get(checkNotNull(globalArguments.json)).toFile().readText()
        )
    else
        globalArguments
    DokkaGenerator(configuration, DokkaConsoleLogger).generate()
}

