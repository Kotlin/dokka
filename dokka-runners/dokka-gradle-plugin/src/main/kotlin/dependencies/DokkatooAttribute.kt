package dev.adamko.dokkatoo.dependencies

import dev.adamko.dokkatoo.internal.Attribute
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute

/**
 * Gradle Configuration Attributes for sharing Dokkatoo files across subprojects.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
@DokkatooInternalApi
interface DokkatooAttribute {

    /** HTML, Markdown, etc. */
    @DokkatooInternalApi
    @JvmInline
    value class Format(private val named: String) : Named {
        override fun getName(): String = named
    }

    /** Generated output, or subproject classpath, or included files, etc */
    @DokkatooInternalApi
    @JvmInline
    value class ModuleComponent(private val named: String) : Named {
        override fun getName(): String = named
    }

    /** A classpath, e.g. for Dokka Plugins or the Dokka Generator. */
    @DokkatooInternalApi
    @JvmInline
    value class Classpath(private val named: String) : Named {
        override fun getName(): String = named
    }

    @DokkatooInternalApi
    companion object {
        val DokkatooFormatAttribute: Attribute<String> =
            Attribute("dev.adamko.dokkatoo.format")

        val DokkatooModuleComponentAttribute: Attribute<String> =
            Attribute("dev.adamko.dokkatoo.module-component")

        val DokkatooClasspathAttribute: Attribute<String> =
            Attribute("dev.adamko.dokkatoo.classpath")
    }
}
