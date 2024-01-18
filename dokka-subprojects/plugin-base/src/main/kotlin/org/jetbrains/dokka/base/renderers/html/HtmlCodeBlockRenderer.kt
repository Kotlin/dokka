/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import kotlinx.html.FlowContent

/**
 * Provides an ability to override code blocks rendering differently dependent on the code language.
 *
 * Multiple renderers can be installed to support different languages in an independent way.
 */
public interface HtmlCodeBlockRenderer {

    /**
     * Whether this renderer supports rendering Markdown code blocks
     * for the given [language] explicitly specified in the fenced code block definition,
     */
    public fun isApplicableForDefinedLanguage(language: String): Boolean

    /**
     * Whether this renderer supports rendering Markdown code blocks
     * for the given [code] when language is not specified in fenced code blocks
     * or indented code blocks are used.
     */
    public fun isApplicableForUndefinedLanguage(code: String): Boolean

    /**
     * Defines how to render [code] for specified [language] via HTML tags.
     *
     * The value of the [language] will be the same as in the input Markdown fenced code block definition.
     * In the following example [language] = `kotlin` and [code] = `val a`:
     * ~~~markdown
     * ```kotlin
     * val a
     * ```
     * ~~~
     * The value of the [language] will be `null` if language is not specified in the fenced code block definition
     * or indented code blocks are used.
     * In the following example [language] = `null` and [code] = `val a`:
     * ~~~markdown
     * ```
     * val a
     * ```
     * ~~~
     */
    public fun FlowContent.buildCodeBlock(language: String?, code: String)
}
