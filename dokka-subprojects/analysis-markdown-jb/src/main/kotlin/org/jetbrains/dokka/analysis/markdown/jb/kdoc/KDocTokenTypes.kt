/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb.kdoc

/** Tags whose first word after the tag name is a subject (parameter name, exception name, ...), not body text. */
internal object KDocTokenTypes {
    val SUBJECT_TAKING_TAGS: Set<String> = setOf("param", "property", "throws", "exception", "see", "sample")
}
