/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.pages

import org.jetbrains.dokka.model.properties.ExtraProperty

public class SimpleAttr(
    public val extraKey: String,
    public val extraValue: String
) : ExtraProperty<ContentNode> {
    public data class SimpleAttrKey(val key: String) : ExtraProperty.Key<ContentNode, SimpleAttr>
    override val key: ExtraProperty.Key<ContentNode, SimpleAttr> = SimpleAttrKey(extraKey)

}

public enum class BasicTabbedContentType : TabbedContentType {
    TYPE, CONSTRUCTOR,

    // property/function here means a different things depending on parent:
    // - if parent=package - describes just `top-level` property/function without receiver
    // - if parent=classlike - describes `member` property/function,
    //   it could have receiver (becoming member extension property/function) or not (ordinary member property/function)
    // for examples look at docs for `EXTENSION_PROPERTY`, `EXTENSION_FUNCTION`
    FUNCTION, PROPERTY,

    ENTRY,

    // property/function here means a different things depending on parent,
    // and not just `an extension property/function`:
    // example 1: `fun Foo.bar()` - top-level extension function
    // - on a page describing `Foo` class `bar` will have type=`EXTENSION_FUNCTION`
    // - on a page describing package declarations `bar` will have type=`EXTENSION_FUNCTION`
    // example 2: `object Namespace { fun Foo.bar() }` - member extension function
    // - on a page describing `Foo` class `bar` will have type=`EXTENSION_FUNCTION`
    // - on a page describing `Namespace` object `bar` will have type=`FUNCTION`
    //
    // These types are needed to separate member functions and extension function on classlike pages.
    // The same split rules are also used
    // when grouping functions/properties with the same name on pages for classlike and package
    EXTENSION_PROPERTY, EXTENSION_FUNCTION
}

/**
 * It is used only to mark content for tabs in HTML format
 */
public interface TabbedContentType

/**
 * @see TabbedContentType
 */
public class TabbedContentTypeExtra(public val value: TabbedContentType) : ExtraProperty<ContentNode> {
    public companion object : ExtraProperty.Key<ContentNode, TabbedContentTypeExtra>
    override val key: ExtraProperty.Key<ContentNode, TabbedContentTypeExtra> = TabbedContentTypeExtra
}

public object HtmlContent : ExtraProperty<ContentNode>, ExtraProperty.Key<ContentNode, HtmlContent> {
    override val key: ExtraProperty.Key<ContentNode, *> = this
}
