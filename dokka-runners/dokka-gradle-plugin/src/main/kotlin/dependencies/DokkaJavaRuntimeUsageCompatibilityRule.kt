/*
 * Copyright 2014 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.Usage

/**
 * # Dokka Java Runtime Usage Compatibility Rule
 *
 * If the consumer requests [DokkaAttribute.DokkaJavaRuntimeUsage],
 * then [Usage.JAVA_RUNTIME] is compatible.
 *
 * If the consumer requests [Usage.JAVA_RUNTIME], then do not apply the rule.
 *
 * ### Context
 *
 * When a project has both a JVM (e.g. Java or Kotlin/JVM) plugin and Dokka plugin,
 * both plugins add consumable Configurations:
 * - The traditional 'project JAR' one, containing the compiled JAR of the project
 *   (e.g. `runtimeElements`).
 * - The Dokka one, containing Dokka plugins required when aggregating the consumable Dokka module
 *   (e.g. [org.jetbrains.dokka.gradle.dependencies.DependencyContainerNames.publicationPluginClasspathApiOnlyConsumable]).
 *
 * Since both Configurations contain JARs, and have transitive dependencies on JARs,
 * it is logical for both to use attributes that describe them as 'contains Java classpath JARs'
 * (e.g. [Usage.JAVA_RUNTIME]).
 * However, this creates a tension when a consuming project, also with Dokka and JVM plugins applied,
 * depends on this project:
 * - The Dokka plugin wants the 'Dokka plugins' Configuration.
 * - The Java plugin wants the 'project JAR' Configuration.
 * - Both must resolve the dependencies with [Usage.JAVA_RUNTIME] to include transitive dependencies.
 * - Since both have 'contains Java classpath JARs' attributes, when they are resolved
 *   Gradle can't differentiate between them.
 * - What happens is the Java plugin ends up resolving the 'Dokka plugins' Configuration,
 *   which leads to extremely confusing errors and behaviours that are very hard to diagnose.
 *   See https://github.com/adamko-dev/dokkatoo/issues/165
 *
 * Basically, Gradle's attribute-matching algorithm is too lenient,
 * and does not provide a mechanism of making matching strictly require on a specific attribute.
 *
 * ### Fix
 *
 * So, how can Dokka fix this?
 *
 * First, here's what won't work:
 * Adding _more_ attributes to the 'Dokka plugins' Configuration,
 * like [org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.DokkaClasspathAttribute],
 * - The Dokka plugin will be able to correctly resolve the 'Dokka plugins' Configuration.
 * - But the JVM plugin will still get confused, because the consumer will _ignore_ unknown attributes.
 * - It doesn't make sense for the JVM plugin to modify its attributes, since they are
 *   already established and used in the general ecosystem.
 *   (For similar reasons, Dokka shouldn't modify the Configurations of other plugins.)
 *
 * Instead, Dokka can use a custom [Usage] attribute value:
 * [org.jetbrains.dokka.gradle.dependencies.DokkaAttribute],
 * and a compatibility rule
 * [org.jetbrains.dokka.gradle.dependencies.DokkaJavaRuntimeUsageCompatibilityRule].
 *
 * - Dokka consumers are able to resolve the transitive dependencies of plugins
 *   thanks to the compatibility rule.
 * - Traditional consumers disambiguate the Dokka plugins variants because the
 *   compatibility rule is one way.
 *   If the consumer asks for [Usage.JAVA_RUNTIME],
 *   then the rule expresses no opinion.
 */
internal abstract class DokkaJavaRuntimeUsageCompatibilityRule : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) {
        details.run {
            if (
                consumerValue?.name == DokkaAttribute.DokkaJavaRuntimeUsage
                &&
                producerValue?.name == Usage.JAVA_RUNTIME
            ) {
                compatible()
            }
        }
    }
}
