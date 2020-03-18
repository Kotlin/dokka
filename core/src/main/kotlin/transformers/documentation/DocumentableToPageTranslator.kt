package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DProject
import org.jetbrains.dokka.pages.ProjectPageNode

interface DocumentableToPageTranslator {
    operator fun invoke(project: DProject): ProjectPageNode
}