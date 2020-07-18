@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl


class GradlePackageOptionsBuilder : DokkaConfigurationBuilder<PackageOptionsImpl> {
    @Input
    var prefix: String = ""

    @Input
    var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic

    @Input
    var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    @Input
    var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    @Input
    var suppress: Boolean = DokkaDefaults.suppress

    override fun build(): PackageOptionsImpl {
        return PackageOptionsImpl(
            prefix = prefix,
            includeNonPublic = includeNonPublic,
            reportUndocumented = reportUndocumented,
            skipDeprecated = skipDeprecated,
            suppress = suppress
        )
    }
}
