/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
    TYPE, CONSTRUCTOR, FUNCTION, PROPERTY, ENTRY, EXTENSION_PROPERTY, EXTENSION_FUNCTION
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
