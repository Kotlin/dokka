@file:Suppress("FunctionName")

package org.jetbrains.dokka.base.parsers.moduleAndPackage

internal fun parseModuleAndPackageDocumentation(
    context: ModuleAndPackageDocumentationParsingContext,
    fragment: ModuleAndPackageDocumentationFragment
): ModuleAndPackageDocumentation {
    return ModuleAndPackageDocumentation(
        name = fragment.name,
        classifier = fragment.classifier,
        documentation = context.parse(fragment)
    )
}

