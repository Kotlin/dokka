/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.tags

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.DocTag

/**
 * Provides an ability to render custom doc tags
 *
 * Custom tags can be generated during build, for instance via transformers from converting an annotation
 * (such as in [org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer])
 *
 * Also, custom tags can come from the kdoc itself, where "custom" is defined as unknown to the compiler/spec.
 * `@property` and `@throws` are not custom tags - they are defined by the spec and have special meaning
 * and separate blocks on the documentation page, it's clear how to render it. Whereas `@usesMathJax` is
 * a custom tag - it's application/plugin specific and is not handled by dokka by default.
 *
 * Using this provider, we can map custom tags  (such as `@usesMathJax`) and generate content for it that
 * will be displayed on the pages.
 */
public interface CustomTagContentProvider {

    /**
     * Whether this content provider supports given [CustomTagWrapper].
     *
     * Tags can be filtered out either by name or by nested [DocTag] type
     */
    public fun isApplicable(customTag: CustomTagWrapper): Boolean

    /**
     * Full blown content description, most likely to be on a separate page
     * dedicated to just one element (i.e one class/function), so any
     * amount of detail should be fine.
     */
    public fun DocumentableContentBuilder.contentForDescription(
        sourceSet: DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {}

    /**
     * Brief comment section, usually displayed as a summary/preview.
     *
     * For instance, when listing all functions of a class on one page,
     * it'll be too much to display complete documentation for each function.
     * Instead, a small brief is shown for each one (i.e the first paragraph
     * or some other important information) - the user can go to the dedicated
     * page for more details if they find the brief interesting.
     *
     * Tag-wise, it would make sense to include `Since Kotlin`, since it's
     * important information for the users of stdlib. It would make little
     * sense to include `@usesMathjax` here, as this information seems
     * to be more specific and detailed than is needed for a brief.
     */
    public fun DocumentableContentBuilder.contentForBrief(
        sourceSet: DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {}
}
