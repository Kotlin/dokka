package org.jetbrains.dokka

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.CLIEntity
import java.io.File
import java.nio.file.Paths


object ArgTypeFile : ArgType<File>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): File = Paths.get(value).toRealPath().toFile()
    override val description: kotlin.String
        get() = "{ String that represents a directory / file path }"
}

object ArgTypePlatform : ArgType<Platform>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): Platform = Platform.fromString(value)
    override val description: kotlin.String
        get() = "{ String that represents a Kotlin platform. Possible values: jvm/js/native/common/android }"
}

object ArgTypeVisibility : ArgType<DokkaConfiguration.Visibility>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String) = DokkaConfiguration.Visibility.fromString(value)
    override val description: kotlin.String
        get() = "{ String that represents a visibility modifier. Possible values: ${getPossibleVisibilityValues()}"

    private fun getPossibleVisibilityValues(): kotlin.String =
        DokkaConfiguration.Visibility.values().joinToString(separator = ", ")
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
        get() = "{ String that represents plugin configuration. " +
                "Format is {fullyQualifiedPluginName}={jsonConfiguration}. " +
                "Quotation marks (`\"`) inside json must be escaped. }"
}

object ArgTypeSourceLinkDefinition : ArgType<DokkaConfiguration.SourceLinkDefinition>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): DokkaConfiguration.SourceLinkDefinition {
        return if (value.isNotEmpty() && value.contains("="))
            SourceLinkDefinitionImpl.parseSourceLinkDefinition(value)
        else {
            throw IllegalArgumentException(
                "Warning: Invalid -srcLink syntax. " +
                        "Expected: <path>=<url>[#lineSuffix]. No source links will be generated."
            )
        }
    }

    override val description: kotlin.String
        get() = "{ String that represent source links. Format: {srcPath}={remotePath#lineSuffix} }"
}

data class ArgTypeArgument(val moduleName: CLIEntity<kotlin.String>) :
    ArgType<DokkaConfiguration.DokkaSourceSet>(true) {
    override fun convert(value: kotlin.String, name: kotlin.String): DokkaConfiguration.DokkaSourceSet =
        (if (moduleName.valueOrigin != ArgParser.ValueOrigin.UNSET && moduleName.valueOrigin != ArgParser.ValueOrigin.UNDEFINED) {
            moduleName.value
        } else {
            DokkaDefaults.moduleName
        }).let { moduleNameOrDefault ->
            parseSourceSet(moduleNameOrDefault, value.split(" ").filter { it.isNotBlank() }.toTypedArray())
        }

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
