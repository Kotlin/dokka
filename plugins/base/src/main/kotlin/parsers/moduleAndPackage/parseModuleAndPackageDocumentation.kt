package org.jetbrains.dokka.base.parsers.moduleAndPackage

fun parseModuleAndPackageDocumentation(
    context: ModuleAndPackageDocumentationParsingContext,
    fragment: ModuleAndPackageDocumentationFragment
): ModuleAndPackageDocumentation {
    return ModuleAndPackageDocumentation(
        name = fragment.name,
        classifier = fragment.classifier,
        documentation = context.parse(fragment)
    )
}
