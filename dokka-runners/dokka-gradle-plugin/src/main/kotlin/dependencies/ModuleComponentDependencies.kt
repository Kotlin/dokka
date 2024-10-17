/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaFormatAttribute
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaModuleComponentAttribute
import org.jetbrains.dokka.gradle.internal.*
import java.io.File

/**
 * Manage sharing and receiving components used to build a Dokka Module.
 *
 * The type of component is determined by [component].
 *
 * Files are shared using variant-aware Gradle [Configuration]s.
 */
@DokkaInternalApi
class ModuleComponentDependencies(
    project: Project,
    private val component: DokkaAttribute.ModuleComponent,
    private val baseAttributes: BaseAttributes,
    private val formatAttributes: FormatAttributes,
    declaredDependencies: Configuration,
    baseConfigurationName: String,
) {
    private val formatName: String get() = formatAttributes.format.name
    private val componentName: String get() = component.name

    private val resolver: Configuration =
        project.configurations.create("${baseConfigurationName}${componentName}Resolver${INTERNAL_CONF_NAME_TAG}") {
            description = "$INTERNAL_CONF_DESCRIPTION_TAG Resolves Dokka $formatName $componentName files."
            resolvable()
            extendsFrom(declaredDependencies)
            attributes {
                attribute(USAGE_ATTRIBUTE, baseAttributes.dokkaUsage)
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaModuleComponentAttribute, component.name)
            }
        }

    val outgoing: Configuration =
        project.configurations.create("${baseConfigurationName}${componentName}Consumable${INTERNAL_CONF_NAME_TAG}") {
            description =
                "$INTERNAL_CONF_DESCRIPTION_TAG Provides Dokka $formatName $componentName files for consumption by other subprojects."
            consumable()
            extendsFrom(declaredDependencies)
            attributes {
                attribute(USAGE_ATTRIBUTE, baseAttributes.dokkaUsage)
                attribute(DokkaFormatAttribute, formatAttributes.format.name)
                attribute(DokkaModuleComponentAttribute, component.name)
            }
        }

    /**
     * Get all files from declared dependencies.
     *
     * The artifacts will be filtered to ensure:
     *
     * - [DokkaModuleComponentAttribute] equals [component]
     * - [DokkaFormatAttribute] equals [FormatAttributes.format]
     *
     * This filtering should prevent a Gradle bug where it fetches random files.
     * Unfortunately, [org.gradle.api.artifacts.ArtifactView.ViewConfiguration.lenient] must be
     * enabled, which might obscure errors.
     */
    val incomingArtifactFiles: Provider<List<File>>
        get() = resolver.incomingArtifacts().map { it.map(ResolvedArtifactResult::getFile) }

    private fun Configuration.incomingArtifacts(): Provider<List<ResolvedArtifactResult>> {

        // Redefine variables locally, because Configuration Cache is easily confused
        // and produces confusing error messages.
        val baseAttributes = baseAttributes
        val usage = baseAttributes.dokkaUsage
        val formatAttributes = formatAttributes
        val incoming = incoming
        val incomingName = incoming.name
        val component = component

        return incoming
            .artifactView {
                @Suppress("UnstableApiUsage")
                withVariantReselection()
                attributes {
                    attribute(USAGE_ATTRIBUTE, usage)
                    attribute(DokkaFormatAttribute, formatAttributes.format.name)
                    attribute(DokkaModuleComponentAttribute, component.name)
                }
                lenient(true)
            }
            .artifacts
            .resolvedArtifacts
            .map { artifacts ->
                artifacts
                    // Gradle says it will only use the attributes defined in the above
                    // `artifactView {}`, but it doesn't, and the artifacts it finds might be
                    // random ones with arbitrary attributes, so we have to filter again.
                    .filter { artifact ->
                        val variantAttributes = artifact.variant.attributes
                        when {
                            variantAttributes[USAGE_ATTRIBUTE]?.name != baseAttributes.dokkaUsage.name -> {
                                logger.info("[${incomingName}] ignoring artifact $artifact - USAGE_ATTRIBUTE != ${baseAttributes.dokkaUsage} | attributes:${variantAttributes.toDebugString()}")
                                false
                            }

                            variantAttributes[DokkaFormatAttribute] != formatAttributes.format.name -> {
                                logger.info("[${incomingName}] ignoring artifact $artifact - DokkaFormatAttribute != ${formatAttributes.format} | attributes:${variantAttributes.toDebugString()}")
                                false
                            }

                            variantAttributes[DokkaModuleComponentAttribute] != component.name -> {
                                logger.info("[${incomingName}] ignoring artifact $artifact - DokkaModuleComponentAttribute != $component | attributes:${variantAttributes.toDebugString()}")
                                false
                            }

                            else -> {
                                logger.info("[${incomingName}] found valid artifact $artifact | attributes:${variantAttributes.toDebugString()}")
                                true
                            }
                        }
                    }
            }
    }

    @DokkaInternalApi
    companion object {
        private val logger: Logger = Logging.getLogger(DokkaAttribute.ModuleComponent::class.java)
    }
}
