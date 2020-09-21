package org.jetbrains.dokka.base.parsers.moduleAndPackage

import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.*
import java.io.File


fun parseModuleAndPackageDocumentationFragments(source: File): List<ModuleAndPackageDocumentationFragment> {
    return parseModuleAndPackageDocumentationFragments(ModuleAndPackageDocumentationFile(source))
}

fun parseModuleAndPackageDocumentationFragments(
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

    val classifier = when (classifierAndName[0].trim()) {
        "Module" -> Module
        "Package" -> Package
        else -> throw IllegalStateException("Unexpected classifier ${classifierAndName[0]}")
    }

    if (classifierAndName.size != 2 && classifier == Module) {
        throw IllegalModuleAndPackageDocumentation(source, "Missing Module name")
    }

    val name = classifierAndName.getOrNull(1)?.trim().orEmpty()
    if (classifier == Package && name.contains(Regex("\\s"))) {
        throw IllegalModuleAndPackageDocumentation(
            source, "Package name cannot contain whitespace in '$firstLine'"
        )
    }

    return ModuleAndPackageDocumentationFragment(
        name = name,
        classifier = classifier,
        documentation = firstLineAndDocumentation.getOrNull(1)?.trim().orEmpty(),
        source = source
    )
}
