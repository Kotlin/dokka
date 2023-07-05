package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs

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
