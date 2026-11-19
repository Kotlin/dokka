/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

/**
 * Marks a [Bound] as an unresolved type and carries the information needed to report it:
 * where the unresolved type was found ([location]) and the human-readable description of
 * what exactly is unresolved ([message]).
 *
 * It is attached by the analysis (e.g. to [UnresolvedBound]) so that reporting can be done
 * later, on the documentable model, by transformers like
 * `org.jetbrains.dokka.base.transformers.documentables.ReportUnresolvedTypesTransformer`.
 */
public data class UnresolvedTypeExtra(
    public val location: String,
    public val message: String,
) : ExtraProperty<Bound> {
    public companion object Key : ExtraProperty.Key<Bound, UnresolvedTypeExtra> {
        override fun mergeStrategyFor(left: UnresolvedTypeExtra, right: UnresolvedTypeExtra): MergeStrategy<Bound> =
            MergeStrategy.Replace(left)
    }

    override val key: Key = Key
}
