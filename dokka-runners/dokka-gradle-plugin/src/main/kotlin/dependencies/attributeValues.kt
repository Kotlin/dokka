package org.jetbrains.dokka.gradle.dependencies

import org.jetbrains.dokka.gradle.internal.Attribute
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named


/** Common [Attribute] values for Dokkatoo [Configuration]s. */
@DokkatooInternalApi
class BaseAttributes(
    objects: ObjectFactory,
) {
    val dokkatooUsage: Usage = objects.named("org.jetbrains.dokka")

    val dokkaPlugins: DokkatooAttribute.Classpath =
        DokkatooAttribute.Classpath("dokka-plugins")

    val dokkaPublicationPlugins: DokkatooAttribute.Classpath =
        DokkatooAttribute.Classpath("dokka-publication-plugins")

    val dokkaGenerator: DokkatooAttribute.Classpath =
        DokkatooAttribute.Classpath("dokka-generator")
}


/** [Attribute] values for a specific Dokka format. */
@DokkatooInternalApi
class FormatAttributes(
    formatName: String,
) {
    val format: DokkatooAttribute.Format =
        DokkatooAttribute.Format(formatName)

    val moduleOutputDirectories: DokkatooAttribute.ModuleComponent =
        DokkatooAttribute.ModuleComponent("ModuleOutputDirectories")
}
