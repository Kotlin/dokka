/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.documentation.KdFragment
import org.jetbrains.kotlin.documentation.KdPackage

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
 * Only MODULE is filled for now; other page kinds return an empty list.
 */
fun buildContent(fragment: KdFragment, page: Page): List<ContentNode> = when (page.kind) {
    PageKind.MODULE -> {
        val entries = page.children.mapNotNull { child ->
            val element = child.represents.singleOrNull()?.let { fragment.elements[it.id] }
            (element as? KdPackage)?.let { PackageEntry(name = it.name, url = child.url) }
        }
        listOf(PackageList(entries))
    }
    PageKind.PACKAGE, PageKind.CLASSLIKE, PageKind.CALLABLE -> emptyList()
}
