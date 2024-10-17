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
 * Gradle Configuration Attributes for sharing Dokka files across subprojects.
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
        /**
         * Describes the type of generated output that Dokka generates.
         *
         * For example, [HTML](https://kotl.in/dokka-html) or [Javadoc](https://kotl.in/dokka-javadoc).
         *
         * @see DokkaAttribute.Format
         */
        val DokkaFormatAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.format")

        /**
         * Dokka Modules have several components that must be shared separately.
         *
         * @see DokkaAttribute.ModuleComponent
         */
        val DokkaModuleComponentAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.module-component")

        /**
         * Runtime JVM classpath for executing Dokka Generator.
         *
         * @see DokkaAttribute.Classpath
         */
        val DokkaClasspathAttribute: Attribute<String> =
            Attribute("org.jetbrains.dokka.classpath")
    }
}
