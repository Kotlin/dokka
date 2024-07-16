package dev.adamko.dokkatoo

import dev.adamko.dokkatoo.formats.DokkatooGfmPlugin
import dev.adamko.dokkatoo.formats.DokkatooHtmlPlugin
import dev.adamko.dokkatoo.formats.DokkatooJavadocPlugin
import dev.adamko.dokkatoo.formats.DokkatooJekyllPlugin
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import javax.inject.Inject
import org.jetbrains.dokka.gradle.DokkaPlugin as ClassicDokkaPlugin

/**
 * Dokkatoo Gradle Plugin.
 *
 * Creates all necessary defaults to generate documentation for HTML, Jekyll, Markdown, and Javadoc formats.
 */
abstract class DokkatooPlugin
@Inject
@DokkatooInternalApi
constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {
        if (isDokkatooEnabled(target)) {
            with(target.pluginManager) {
                apply(type = DokkatooBasePlugin::class)

                // auto-apply the custom format plugins
                apply(type = DokkatooGfmPlugin::class)
                apply(type = DokkatooHtmlPlugin::class)
                apply(type = DokkatooJavadocPlugin::class)
                apply(type = DokkatooJekyllPlugin::class)
            }
        } else {
            target.pluginManager.apply(ClassicDokkaPlugin::class)
        }
    }

    private fun isDokkatooEnabled(project: Project): Boolean {
        return providers.gradleProperty("enableDokkatoo").orNull.toBoolean()
                || (
                project.extraProperties.has("enableDokkatoo")
                        &&
                        project.extraProperties.get("enableDokkatoo")?.toString().toBoolean()
                )
    }
}
