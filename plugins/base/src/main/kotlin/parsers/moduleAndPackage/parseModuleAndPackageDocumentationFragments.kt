package org.jetbrains.dokka.base.parsers.moduleAndPackage

import java.io.File


internal fun parseModuleAndPackageDocumentationFragments(source: File): List<ModuleAndPackageDocumentationFragment> {
    return parseModuleAndPackageDocumentationFragments(ModuleAndPackageDocumentationFile(source))
}

internal fun parseModuleAndPackageDocumentationFragments(
    source: ModuleAndPackageDocumentationSource
): List<ModuleAndPackageDocumentationFragment> {
    val fragmentStrings = source.documentation.split(Regex("(|^)#\\s*(?=(Module|Package))"))
    return fragmentStrings
        .filter(String::isNotBlank)
        .map { fragmentString -> parseModuleAndPackageDocFragment(source, fragmentString) }
}

private fun parseModuleAndPackageDocFragment(
    source: ModuleAndPackageDocumentationSource,
    fragment: String
): ModuleAndPackageDocumentationFragment {
    val firstLineAndDocumentation = fragment.split("\r\n", "\n", "\r", limit = 2)
    val firstLine = firstLineAndDocumentation[0]

    val classifierAndName = firstLine.split(Regex("\\s+"), limit = 2)
    if (classifierAndName.size != 2) {
        throw IllegalModuleAndPackageDocumentation(source, "Missing ${classifierAndName.first()} name")
    }

    val classifier = when (classifierAndName[0].trim()) {
        "Module" -> ModuleAndPackageDocumentation.Classifier.Module
        "Package" -> ModuleAndPackageDocumentation.Classifier.Package
        else -> throw IllegalStateException("Unexpected classifier ${classifierAndName[0]}")
    }

    val name = classifierAndName[1].trim()
    if (name.contains(Regex("\\s"))) {
        throw IllegalModuleAndPackageDocumentation(
            source, "Module/Package name cannot contain whitespace in '$firstLine'"
        )
    }

    return ModuleAndPackageDocumentationFragment(
        name = name,
        classifier = classifier,
        documentation = firstLineAndDocumentation.getOrNull(1)?.trim().orEmpty(),
        source = source
    )
}



