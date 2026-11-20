/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.documentation.KdClass
import org.jetbrains.kotlin.documentation.KdConstructor
import org.jetbrains.kotlin.documentation.KdElement
import org.jetbrains.kotlin.documentation.KdFragment
import org.jetbrains.kotlin.documentation.KdFunction
import org.jetbrains.kotlin.documentation.KdInheritedMember
import org.jetbrains.kotlin.documentation.KdModule
import org.jetbrains.kotlin.documentation.KdPackage
import org.jetbrains.kotlin.documentation.KdProperty
import org.jetbrains.kotlin.documentation.KdRefinedElement
import org.jetbrains.kotlin.documentation.KdTypealias

/**
 * Presentation model: page content ready to be handed to the React renderer.
 *
 * Built on the Kotlin side from KDM and the page tree ([Page]).
 * Serialized with [PJson] using the `kind` discriminator, like KDM — see KdJson.
 *
 * Rule: a node describes meaning, not layout. The overview shape of the whole
 * vocabulary is `docs/presentation-model.sketch.kt`; this file is the real
 * vocabulary, growing step by step from the `models/{jvm,kmp}/model.json` data
 * and the current reference. When a type appears here, its sketch is obsolete.
 */
@Serializable
sealed interface ContentNode

/**
 * Step 1 finding: the module page (P-02, `/jvm/`) carries a list of packages (E-036).
 *
 * Data: `MODULE:jvm` -> `packages`: 8 ids, each resolving to a `KdPackage`
 * with a `name` field. The row address is the url of the child package page
 * in the [Page] tree.
 *
 * Notes:
 *  - module documentation (F-044, includes) is absent from the jvm model;
 *    if it appears, it is a separate content node, not a field here;
 *  - the empty package (`emptypackage`, 0 declarations, P-04) is included in
 *    the list — filter it or not, open question Q-019;
 *  - platform badges on rows (E-036, fragment set) come at the kmp step.
 */
@Serializable
@SerialName("packageList")
data class PackageList(
    val packages: List<PackageEntry>,
) : ContentNode

/** One row of the package list on the module page. */
@Serializable
data class PackageEntry(
    /** Full package name: "org.jetbrains.dokka.uitest". */
    val name: String,
    /** Ready url of the package page: "/jvm/org.jetbrains.dokka.uitest/". */
    val url: String,
)

/**
 * Step 2 finding: the package page (P-03) carries member groups — Types, Functions,
 * Properties. Only non-empty groups are built; how they are presented (tabs,
 * sections, otherwise) is a UI decision.
 *
 * Data: the package's top-level declarations — child pages of the package page.
 * The group comes from the element kind, the name from the KDM element,
 * the address from the child page url.
 *
 * Notes:
 *  - constructors cannot appear at the package level, so no group for them;
 *  - a typealias goes into Types, as in Dokka v1;
 *  - the row signature (`fun foo(): Bar`) comes later, with signature tokens;
 *  - the enum order of [MemberGroupKind] is the group order on the page (K-07).
 */
@Serializable
@SerialName("memberGroup")
data class MemberGroup(
    val group: MemberGroupKind,
    val members: List<MemberEntry>,
) : ContentNode

/** Group kinds of a package page. The declaration order is the display order. */
enum class MemberGroupKind {
    TYPES,
    FUNCTIONS,
    PROPERTIES,
}

/** One row of a member group on the package page. */
@Serializable
data class MemberEntry(
    /** Simple declaration name: "Foo", "printHello". */
    val name: String,
    /** Ready url of the member page: "/jvm/org.jetbrains.dokka.uitest/Foo/". */
    val url: String,
)

/** Group and display name of a package-level declaration; null when it owes no row. */
private fun memberGroupEntry(element: KdElement): Pair<MemberGroupKind, String>? = when (element) {
    is KdClass -> MemberGroupKind.TYPES to element.name
    is KdTypealias -> MemberGroupKind.TYPES to element.name
    is KdFunction -> MemberGroupKind.FUNCTIONS to element.name
    is KdProperty -> MemberGroupKind.PROPERTIES to element.name
    is KdConstructor,
    is KdModule,
    is KdPackage,
    is KdRefinedElement,
    is KdInheritedMember,
    -> null
}

/**
 * JSON format for the presentation model. The same `kind` discriminator as KDM,
 * so the React side reads both formats with one rule.
 */
val PJson: Json = Json {
    classDiscriminator = "kind"
    ignoreUnknownKeys = true
    prettyPrint = true
}

/**
 * Content of a single page of the [Page] tree.
 *
 * Data comes from KDM through `represents`: id from the ref -> element in [fragment].
 * Addresses come from the tree: a child page already has its url computed.
 * MODULE and PACKAGE are filled for now; other page kinds return an empty list.
 */
fun buildContent(fragment: KdFragment, page: Page): List<ContentNode> = when (page.kind) {
    PageKind.MODULE -> {
        val entries = page.children.mapNotNull { child ->
            val element = child.represents.singleOrNull()?.let { fragment.elements[it.id] }
            (element as? KdPackage)?.let { PackageEntry(name = it.name, url = child.url) }
        }
        listOf(PackageList(entries))
    }
    PageKind.PACKAGE -> {
        val members = page.children.mapNotNull { child ->
            val element = child.represents.singleOrNull()?.let { fragment.elements[it.id] }
            element?.let(::memberGroupEntry)?.let { (kind, name) ->
                kind to MemberEntry(name = name, url = child.url)
            }
        }
        MemberGroupKind.entries.mapNotNull { kind ->
            members.filter { it.first == kind }.map { it.second }
                .takeIf { it.isNotEmpty() }
                ?.let { MemberGroup(group = kind, it) }
        }
    }
    PageKind.CLASSLIKE, PageKind.CALLABLE -> emptyList()
}
