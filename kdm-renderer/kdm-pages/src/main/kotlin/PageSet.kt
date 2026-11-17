/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages

import org.jetbrains.kotlin.documentation.KdClass
import org.jetbrains.kotlin.documentation.KdConstructor
import org.jetbrains.kotlin.documentation.KdElement
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.KdFragmentName
import org.jetbrains.kotlin.documentation.KdFunction
import org.jetbrains.kotlin.documentation.KdInheritedMember
import org.jetbrains.kotlin.documentation.KdModule
import org.jetbrains.kotlin.documentation.KdPackage
import org.jetbrains.kotlin.documentation.KdProperty
import org.jetbrains.kotlin.documentation.KdRefinedElement
import org.jetbrains.kotlin.documentation.KdTypealias

/**
 * What a page represents: one declaration in one fragment. The fragment belongs to the
 * reference, not to the page — expect and actual share the same id string across fragments
 * (`expectActualMappings`), and a single page represents them all.
 */
data class ElementRef(
    val fragment: KdFragmentName,
    val id: KdElementId,
)

enum class PageKind {
    MODULE,
    PACKAGE,
    CLASSLIKE,
    CALLABLE,
}

/**
 * One page of the site. [segments] are the URL path segments — the package name as-is,
 * declaration names after sanitize. [represents] holds every element the page stands for:
 * a member page of an overloaded function carries one ref per overload.
 */
data class Page(
    val kind: PageKind,
    val segments: List<String>,
    val represents: List<ElementRef>,
    val children: List<Page> = emptyList(),
    val url: String = segments.joinToString("/", prefix = "/")
        .let { path -> if (kind == PageKind.CALLABLE) path else "$path/" },
)

/** The whole site: one root page per module. The aggregate root (step 7) comes later. */
data class PageSet(
    val modules: List<Page>,
)

/**
 * Flat, sorted URL list, in the shape of `expected.txt`: pages rendered as `index.html`
 * become a trailing slash, member pages lose their extension.
 *
 * The trailing-slash rule lives in [Page.url]; this function only walks the tree.
 * Percent-encoding still comes later, with the `identifierToFilename` port (step 3).
 */
fun PageSet.urls(): List<String> = modules.flatMap { it.urls() }.sorted()

private fun Page.urls(): List<String> = listOf(url) + children.flatMap { it.urls() }

sealed interface PageDecision {
    data class Create(val pageKind: PageKind) : PageDecision
    data class Skip(val reason: String) : PageDecision
}

fun KdElement.pageDecision(): PageDecision = when (this) {
    is KdModule -> PageDecision.Create(PageKind.MODULE)
    is KdPackage -> PageDecision.Create(PageKind.PACKAGE)
    is KdClass -> PageDecision.Create(PageKind.CLASSLIKE)
    is KdTypealias -> PageDecision.Create(PageKind.CLASSLIKE)   // as in Dokka v1
    is KdFunction -> PageDecision.Create(PageKind.CALLABLE)
    is KdProperty -> PageDecision.Create(PageKind.CALLABLE)
    is KdConstructor -> PageDecision.Create(PageKind.CALLABLE)
    is KdRefinedElement -> PageDecision.Skip("page comes from $fragmentName")
    is KdInheritedMember -> PageDecision.Skip("lives on the page of $originalId")
}

fun KdElement.nameForUrl(parent: KdPackage): String = when (this) {
    is KdClass -> name
    is KdTypealias -> name
    is KdFunction -> name
    is KdProperty -> name
    is KdConstructor -> parent.name
    else -> error("unexpected declaration element type: $this")
}

