/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode

/**
 * This page is internal because it's an stdlib-specific feature,
 * which is not intended for public use or customization.
 * 
 * For more details, see https://github.com/Kotlin/dokka/issues/2887
 */
internal class AllTypesPageNode(
    override val content: ContentNode,
    override val embeddedResources: List<String> = listOf()
) : ContentPage {
    override val dri: Set<DRI> = setOf(DRI)
    override val name: String = "All Types"
    override val children: List<PageNode> get() = emptyList()

    override fun modified(name: String, children: List<PageNode>): AllTypesPageNode =
        modified(name = name, content = this.content, dri = dri, children = children)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): AllTypesPageNode =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else AllTypesPageNode(content, embeddedResources)

    companion object {
        val DRI: DRI = DRI(packageName = ".alltypes")
    }
}

// copy-pasted from dokka-core, not sure why it was needed in the first place
private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })
