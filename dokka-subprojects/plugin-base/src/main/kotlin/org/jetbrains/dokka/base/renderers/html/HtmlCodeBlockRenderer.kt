/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
     * Whether this renderer supports given [language].
     *
     * [code] can be useful to determine applicability if [language] is not provided (empty string)
     */
    public fun isApplicable(language: String, code: String): Boolean

    /**
     * Defines how to render [code] for specified [language] via HTML tags
     */
    public fun FlowContent.buildCodeBlock(language: String, code: String)
}
