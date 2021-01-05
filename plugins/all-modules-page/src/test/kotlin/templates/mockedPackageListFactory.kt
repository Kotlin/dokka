package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.base.renderers.PackageListService
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat

internal fun mockedPackageListForPackages(format: RecognizedLinkFormat, vararg packages: String): String =
    """
       ${PackageListService.DOKKA_PARAM_PREFIX}.format:${format.formatName}
       ${PackageListService.DOKKA_PARAM_PREFIX}.linkExtension:${format.linkExtension}
       
       ${packages.sorted().joinToString(separator = "\n", postfix = "\n") { it }}
    """.trimIndent()