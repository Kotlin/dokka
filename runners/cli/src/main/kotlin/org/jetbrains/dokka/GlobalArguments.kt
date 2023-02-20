package org.jetbrains.dokka

import kotlinx.cli.*
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.jetbrains.dokka.utilities.cast
import java.io.File

class GlobalArguments(args: Array<String>) : DokkaConfiguration {

    val parser = ArgParser("dokka-cli", prefixStyle = ArgParser.OptionPrefixStyle.JVM)

    val json: String? by parser.argument(ArgType.String, description = "JSON configuration file path").optional()

    private val _moduleName = parser.option(
        ArgType.String,
        description = "Name of the project/module",
        fullName = "moduleName"
    ).default(DokkaDefaults.moduleName)

    override val moduleName: String by _moduleName

    override val moduleVersion by parser.option(
        ArgType.String,
        description = "Documented version",
        fullName = "moduleVersion"
    )

    override val outputDir by parser.option(ArgTypeFile, description = "Output directory path, ./dokka by default")
        .default(DokkaDefaults.outputDir)

    override val cacheRoot = null

    override val sourceSets by parser.option(
        ArgTypeArgument(_moduleName),
        description = "Configuration for a Dokka source set. Contains nested configuration.",
        fullName = "sourceSet"
    ).multiple()

    override val pluginsConfiguration by parser.option(
        ArgTypePlugin,
        description = "Configuration for Dokka plugins. Accepts multiple values separated by `^^`."
    ).delimiter("^^")

    override val pluginsClasspath by parser.option(
        ArgTypeFile,
        fullName = "pluginsClasspath",
        description = "List of jars with Dokka plugins and their dependencies. Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    override val offlineMode by parser.option(
        ArgType.Boolean,
        description = "Whether to resolve remote files/links over network"
    ).default(DokkaDefaults.offlineMode)

    override val failOnWarning by parser.option(
        ArgType.Boolean,
        description = "Whether to fail documentation generation if Dokka has emitted a warning or an error"
    ).default(DokkaDefaults.failOnWarning)

    override val delayTemplateSubstitution by parser.option(
        ArgType.Boolean,
        description = "Delay substitution of some elements. Used in incremental builds of multimodule projects"
    ).default(DokkaDefaults.delayTemplateSubstitution)

    val noSuppressObviousFunctions: Boolean by parser.option(
        ArgType.Boolean,
        description = "Whether to suppress obvious functions such as inherited from `kotlin.Any` and `java.lang.Object`"
    ).default(!DokkaDefaults.suppressObviousFunctions)

    override val suppressObviousFunctions: Boolean by lazy { !noSuppressObviousFunctions }

    private val _includes by parser.option(
        ArgTypeFile,
        fullName = "includes",
        description = "Markdown files that contain module and package documentation. " +
                "Accepts multiple values separated by semicolons"
    ).delimiter(";")

    override val includes: Set<File> by lazy { _includes.toSet() }

    override val suppressInheritedMembers: Boolean by parser.option(
        ArgType.Boolean,
        description = "Whether to suppress inherited members that aren't explicitly overridden in a given class"
    ).default(DokkaDefaults.suppressInheritedMembers)

    override val finalizeCoroutines: Boolean = true

    val globalPackageOptions by parser.option(
        ArgType.String,
        description = "Global list of package configurations in format " +
                "\"matchingRegexp,-deprecated,-privateApi,+warnUndocumented,+suppress;...\". " +
                "Accepts multiple values separated by semicolons. "
    ).delimiter(";")

    val globalLinks by parser.option(
        ArgType.String,
        description = "Global external documentation links in format {url}^{packageListUrl}. " +
                "Accepts multiple values separated by `^^`"
    ).delimiter("^^")

    val globalSrcLink by parser.option(
        ArgType.String,
        description = "Global mapping between a source directory and a Web service for browsing the code. " +
                "Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    val helpSourceSet by parser.option(
        ArgTypeHelpSourceSet(_moduleName),
        description = "Prints help for nested -sourceSet configuration"
    )

    val loggingLevel by parser.option(
        ArgType.Choice(toVariant = {
            when (it.toUpperCase().trim()) {
                "DEBUG", "" -> LoggingLevel.DEBUG
                "PROGRESS" -> LoggingLevel.PROGRESS
                "INFO" -> LoggingLevel.INFO
                "WARN" -> LoggingLevel.WARN
                "ERROR" -> LoggingLevel.ERROR
                else -> {
                    println("""Failed to deserialize logging level, got $it expected one of
                        |"DEBUG", "PROGRESS", "INFO", "WARN", "ERROR", falling back to PROGRESS""".trimMargin())
                    LoggingLevel.PROGRESS
                }
            }
        }, toString = { it.toString() }
        )).default(LoggingLevel.PROGRESS)

    override val modules: List<DokkaConfiguration.DokkaModuleDescription> = emptyList()

    val logger: DokkaLogger by lazy {
        DokkaConsoleLogger(loggingLevel)
    }

    init {
        parser.parse(args)

        sourceSets.forEach {
            it.perPackageOptions.cast<MutableList<DokkaConfiguration.PackageOptions>>()
                .addAll(parsePerPackageOptions(globalPackageOptions))
        }

        sourceSets.forEach {
            it.externalDocumentationLinks.cast<MutableSet<DokkaConfiguration.ExternalDocumentationLink>>().addAll(parseLinks(globalLinks))
        }

        globalSrcLink.forEach {
            if (it.isNotEmpty() && it.contains("="))
                sourceSets.all { sourceSet ->
                    sourceSet.sourceLinks.cast<MutableSet<SourceLinkDefinitionImpl>>()
                        .add(SourceLinkDefinitionImpl.parseSourceLinkDefinition(it))
                }
            else {
                logger.warn("Invalid -srcLink syntax. Expected: <path>=<url>[#lineSuffix]. No source links will be generated.")
            }
        }

        sourceSets.forEach {
            it.externalDocumentationLinks.cast<MutableSet<DokkaConfiguration.ExternalDocumentationLink>>().addAll(defaultLinks(it))
        }
    }
}
