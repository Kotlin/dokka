/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.pages

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode

public fun pageScanner(block: PageNode.() -> Unit): PageTransformer {
    return PageTransformer { input -> input.invokeOnAll(block) as RootPageNode }
}

public fun pageMapper(block: PageNode.() -> PageNode): PageTransformer {
    return PageTransformer { input -> input.alterChildren(block) as RootPageNode }
}

public fun pageStructureTransformer(block: RootPageNode.() -> RootPageNode): PageTransformer {
    return PageTransformer { input -> block(input) }
}

public fun PageNode.invokeOnAll(block: PageNode.() -> Unit): PageNode =
    this.also(block).also { it.children.forEach { it.invokeOnAll(block) } }

public fun PageNode.alterChildren(block: PageNode.() -> PageNode): PageNode =
    block(this).modified(children = this.children.map { it.alterChildren(block) })

