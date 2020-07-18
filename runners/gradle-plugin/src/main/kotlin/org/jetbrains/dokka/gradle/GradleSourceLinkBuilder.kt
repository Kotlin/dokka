package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.SourceLinkDefinitionImpl

class GradleSourceLinkBuilder : DokkaConfigurationBuilder<SourceLinkDefinitionImpl> {
    // TODO NOW: CHECK UP TO DATE
    @Input
    var path: String = ""

    @Input
    var url: String = ""

    @Input
    var lineSuffix: String? = null

    override fun build(): SourceLinkDefinitionImpl {
        return SourceLinkDefinitionImpl(
            path = path,
            url = url,
            lineSuffix = lineSuffix
        )
    }
}
