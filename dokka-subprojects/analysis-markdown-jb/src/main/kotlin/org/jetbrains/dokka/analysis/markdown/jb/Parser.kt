/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.*

@InternalDokkaApi
public abstract class Parser {

    public abstract fun parseStringToDocNode(extractedString: String): DocTag

    protected abstract fun preparse(text: String): String

    public open fun parse(text: String): DocumentationNode = parseKDocComment(preparse(text))

    /**
     * Parses a full KDoc comment body (already stripped of comment decoration, e.g. of `/**`/`*/`/leading `*`)
     * into a [Description] plus one [TagWrapper] per `@tag` section.
     */
    protected abstract fun parseKDocComment(text: String): DocumentationNode

    protected open fun createTagWrapper(tagName: String, subject: String?, body: DocTag): TagWrapper =
        when (tagName) {
            "description" -> Description(body)
            "author" -> Author(body)
            "version" -> Version(body)
            "since" -> Since(body)
            "see" -> See(body, subject.orEmpty(), null)
            "param" -> Param(body, subject.orEmpty())
            "property" -> Property(body, subject.orEmpty())
            "return" -> Return(body)
            "constructor" -> Constructor(body)
            "receiver" -> Receiver(body)
            "throws", "exception" -> Throws(body, subject.orEmpty(), null)
            "deprecated" -> Deprecated(body)
            "sample" -> Sample(body, subject.orEmpty())
            "suppress" -> Suppress(body)
            else -> CustomTagWrapper(body, tagName)
        }
}
