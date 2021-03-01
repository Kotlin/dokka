package org.jetbrains.dokka.android.transformers

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.plugability.DokkaContext

class HideTagDocumentableFilter(val dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        d.documentation.any { (_, docs) -> docs.dfs { it is CustomTagWrapper && it.name.trim() == "hide" } != null }
}