/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.jetbrains.dokka.gradle.internal.Attribute
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi

/**
 * Gradle Configuration Attributes for sharing Dokkatoo files across subprojects.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
@DokkaInternalApi
interface DokkaAttribute {

    /** HTML, Markdown, etc. */
    @DokkaInternalApi
    class Format(private val named: String) : Named {
        override fun getName(): String = named
        override fun toString(): String = "Format($named)"
    }

    /** Generated output, or subproject classpath, or included files, etc. */
    @DokkaInternalApi
    class ModuleComponent(private val named: String) : Named {
        override fun getName(): String = named
        override fun toString(): String = "ModuleComponent($named)"
    }

    /** A classpath, e.g. for Dokka Plugins or the Dokka Generator. */
    @DokkaInternalApi
    class Classpath(private val named: String) : Named {
        override fun getName(): String = named
        override fun toString(): String = "Classpath($named)"
    }

    @DokkaInternalApi
    companion object {
        val DokkaFormatAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.format")

        val DokkaModuleComponentAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.module-component")

        val DokkaClasspathAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.classpath")
    }
}
