package org.jetbrains.dokka

import com.google.gson.Gson
import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger

import java.util.function.BiConsumer


fun parsePerPackageOptions(arg: String): List<PackageOptions> {
    if (arg.isBlank()) return emptyList()

    return arg.split(";").map { it.split(",") }.map {
        val prefix = it.first()
        if (prefix == "")
            throw IllegalArgumentException("Please do not register packageOptions with all match pattern, use global settings instead")
        val args = it.subList(1, it.size)
        val deprecated = args.find { it.endsWith("deprecated") }?.startsWith("+") ?: true
        val reportUndocumented = args.find { it.endsWith("reportUndocumented") }?.startsWith("+") ?: true
        val privateApi = args.find { it.endsWith("privateApi") }?.startsWith("+") ?: false
        val suppress = args.find { it.endsWith("suppress") }?.startsWith("+") ?: false
        PackageOptionsImpl(
            prefix,
            includeNonPublic = privateApi,
            reportUndocumented = reportUndocumented,
            skipDeprecated = !deprecated,
            suppress = suppress
        )
    }
}

class DokkaBootstrapImpl : DokkaBootstrap {

    class DokkaProxyLogger(val consumer: BiConsumer<String, String>) : DokkaLogger {
        override var warningsCount: Int = 0
        override var errorsCount: Int = 0

        override fun debug(message: String) {
            consumer.accept("debug", message)
        }

        override fun info(message: String) {
            consumer.accept("info", message)
        }

        override fun progress(message: String) {
            consumer.accept("progress", message)
        }

        override fun warn(message: String) {
            consumer.accept("warn", message).also { warningsCount++ }
        }

        override fun error(message: String) {
            consumer.accept("error", message).also { errorsCount++ }
        }

        override fun report() {
            if (warningsCount > 0 || errorsCount > 0) {
                println(
                    "Generation completed with $warningsCount warning" +
                            (if (DokkaConsoleLogger.warningsCount == 1) "" else "s") +
                            " and $errorsCount error" +
                            if (DokkaConsoleLogger.errorsCount == 1) "" else "s"
                )
            } else {
                println("generation completed successfully")
            }
        }
    }

    private lateinit var generator: DokkaGenerator
    val gson = Gson()

    fun configure(logger: DokkaLogger, configuration: DokkaConfigurationImpl) = with(configuration) {

        fun defaultLinks(config: PassConfigurationImpl): List<ExternalDocumentationLinkImpl> {
            val links = mutableListOf<ExternalDocumentationLinkImpl>()
            if (!config.noJdkLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://docs.oracle.com/javase/${config.jdkVersion}/docs/api/")
                    .build() as ExternalDocumentationLinkImpl

            if (!config.noStdlibLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://kotlinlang.org/api/latest/jvm/stdlib/")
                    .build() as ExternalDocumentationLinkImpl
            return links
        }

        val configurationWithLinks =
            configuration.copy(
                passesConfigurations =
                passesConfigurations.map {
                    val links: List<ExternalDocumentationLinkImpl> =
                        it.externalDocumentationLinks + defaultLinks(it)
                    it.copy(externalDocumentationLinks = links)
                }
            )

        generator = DokkaGenerator(configurationWithLinks, logger)
    }

    override fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String) = configure(
        DokkaProxyLogger(logger),
        gson.fromJson(serializedConfigurationJSON, DokkaConfigurationImpl::class.java)
    )

    override fun generate() = generator.generate()
}
