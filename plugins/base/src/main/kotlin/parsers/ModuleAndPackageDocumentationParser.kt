package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.DokkaException
import java.io.File

internal class IllegalModuleAndPackageDocumentation(
    source: ModuleAndPackageDocumentationSource, message: String
) : DokkaException("[$source] $message")

data class ModuleAndPackageDocFragment(
    val classifier: Classifier,
    val name: String,
    val documentation: String
) {
    enum class Classifier { Module, Package }
}

internal abstract class ModuleAndPackageDocumentationSource {
    abstract val sourceDescription: String
    abstract val documentation: String

    override fun toString(): String {
        return sourceDescription
    }
}

internal class ModuleAndPackageDocumentationFile(private val file: File) : ModuleAndPackageDocumentationSource() {
    override val sourceDescription: String = file.path
    override val documentation: String by lazy(LazyThreadSafetyMode.PUBLICATION) { file.readText() }
}

internal fun parseModuleAndPackageDocFragments(source: File): List<ModuleAndPackageDocFragment> {
    return parseModuleAndPackageDocFragments(ModuleAndPackageDocumentationFile(source))
}

internal fun parseModuleAndPackageDocFragments(source: ModuleAndPackageDocumentationSource): List<ModuleAndPackageDocFragment> {
    val fragmentStrings = source.documentation.split(Regex("(|^)#\\s*(?=(Module|Package))"))
    return fragmentStrings
        .filter(String::isNotBlank)
        .map { fragmentString -> parseModuleAndPackageDocFragment(source, fragmentString) }
}

private fun parseModuleAndPackageDocFragment(
    source: ModuleAndPackageDocumentationSource,
    fragment: String
): ModuleAndPackageDocFragment {
    val firstLineAndDocumentation = fragment.split("\r\n", "\n", "\r", limit = 2)
    val firstLine = firstLineAndDocumentation[0]

    val classifierAndName = firstLine.split(Regex("\\s+"), limit = 2)
    if (classifierAndName.size != 2) {
        throw IllegalModuleAndPackageDocumentation(source, "Missing ${classifierAndName.first()} name")
    }

    val classifier = when (classifierAndName[0].trim()) {
        "Module" -> ModuleAndPackageDocFragment.Classifier.Module
        "Package" -> ModuleAndPackageDocFragment.Classifier.Package
        else -> throw IllegalStateException("Unexpected classifier ${classifierAndName[0]}")
    }

    val name = classifierAndName[1].trim()
    if (name.contains(Regex("\\s"))) {
        throw IllegalModuleAndPackageDocumentation(
            source, "Module/Package name cannot contain whitespace in '$firstLine'"
        )
    }

    return ModuleAndPackageDocFragment(
        classifier = classifier,
        name = name,
        documentation = firstLineAndDocumentation.getOrNull(1)?.trim().orEmpty()
    )
}



