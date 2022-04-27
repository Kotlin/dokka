package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.model.Language
import org.jetbrains.dokka.model.WithSources

internal fun WithSources.documentableLanguage(sourceSet: DokkaConfiguration.DokkaSourceSet): Language =
    when (sources[sourceSet]) {
        is PsiDocumentableSource -> Language.JAVA
        else -> Language.KOTLIN
    }