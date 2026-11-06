/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation.pages

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.documentation.KdBundle
import org.jetbrains.kotlin.documentation.KdElement
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.KdJson
import org.jetbrains.kotlin.documentation.KdModule
import org.jetbrains.kotlin.documentation.KdPackage
import java.io.File

private const val DEFAULT_MODEL =
    "kdm-renderer/kdm-pages/models/jvm/model.json"

fun main(args: Array<String>) {
    val file = args.firstOrNull()?.let(::File) ?: File(repoRoot(), DEFAULT_MODEL)
    println(file.absolutePath)
    if (!file.isFile) {
        println("no such file")
        return
    }

    val bundle = KdJson.decodeFromString<KdBundle>(file.readText())

    println("typed model: schema v${bundle.schemaVersion}, ${bundle.fragments.size} fragments")
    bundle.fragments.first().let { fragment ->
        val packages = fragment.elements.values.count { it is KdPackage }
        println("  ${fragment.name}: ${fragment.elements.size} elements, $packages packages")
    }

    bundle.fragments.first().let { fragment ->
        val pageSet = PageSetBuilder().buildPageSet(fragment)

        println("  page set urls:")
        pageSet.urls().forEach { println("    $it") }
    }

}

private fun JsonObject.name(): String =
    (this["name"] ?: this["fragmentName"])?.jsonPrimitive?.contentOrNull ?: "?"

/**
 * Packages of a fragment. Three shapes are on disk, all read here:
 *  - branch `saveModule` output — `elements` is a list, packages are elements of kind "package";
 *  - new spec shape (`../models`) — `elements` is a map keyed by id, name in `fragmentName`;
 *  - old `Kd*` serialization (`../samples/kdp`) — packages sit in `packages` directly.
 */
private fun packagesOf(fragment: JsonObject): List<String> = when (val elements = fragment["elements"]) {
    is JsonArray -> elements.map { it.jsonObject }
    is JsonObject -> elements.values.map { it.jsonObject }
    else -> (fragment["packages"] as? JsonArray)?.map { it.jsonObject } ?: emptyList()
}
    .filter { it.stringOrNull("kind") == null || it.stringOrNull("kind") == "package" }
    .mapNotNull { it.stringOrNull("name") }

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun repoRoot(): File = generateSequence(File("").absoluteFile) { it.parentFile }
    .firstOrNull { File(it, "settings.gradle.kts").isFile }
    ?: File("").absoluteFile
