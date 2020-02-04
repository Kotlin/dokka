package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.conversions.asJava
import org.jetbrains.dokka.kotlinAsJava.conversions.asStatic
import org.jetbrains.dokka.kotlinAsJava.conversions.withClass
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.DefaultPageBuilder
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.dokka.pages.RootContentBuilder
import org.jetbrains.dokka.transformers.descriptors.KotlinClassKindTypes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi

fun DeclarationDescriptor.sourceLocation(): String? = this.findPsi()?.containingFile?.virtualFile?.path
fun <T : Documentable> List<T>.groupedByLocation(): Map<String, List<T>> =
    this.map { DescriptorCache[it.dri]?.sourceLocation() to it }
        .filter { it.first != null }.groupBy({ (location, _) ->
            location!!.let { it.split("/").last().split(".").first() + "Kt" }
        }) { it.second }

class KotlinAsJavaPageBuilder(rootContentGroup: RootContentBuilder) : DefaultPageBuilder(rootContentGroup) {

    data class FunsAndProps(val key: String, val funs: List<Function>, val props: List<Property>)

    override fun pageForPackage(p: Package): PackagePageNode {

        val funs = p.functions.groupedByLocation()

        val props = p.properties.groupedByLocation()

        val zipped = (funs.keys + props.keys)
            .map { k -> FunsAndProps(k, funs[k].orEmpty(), props[k].orEmpty()) }

        val classes = (p.classes + zipped.map { (key, funs, props) ->
            val dri = p.dri.withClass(key)
            Class(
                dri = dri,
                name = key,
                kind = KotlinClassKindTypes.CLASS,
                constructors = emptyList(),
                functions = funs.map { it.withClass(key, dri).asStatic() },
                properties = props.map { it.withClass(key, dri) },
                classes = emptyList(),
                actual = emptyList(),
                expected = null,
                visibility = p.platformData.map { it to Visibilities.PUBLIC }.toMap()
            )
        }).map { it.asJava() }

        return PackagePageNode(
            p.name, contentForPackage(p, classes), setOf(p.dri), p,
            classes.map(::pageForClass)
        )
    }

    private fun contentForPackage(p: Package, nClasses: List<Class>) = group(p) {
        header(1) { text("Package ${p.name}") }
        block("Types", 2, ContentKind.Properties, nClasses, p.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
    }

    override fun contentForClass(c: Class) = group(c) {
        header(1) { text(c.name) }
        c.inherited.takeIf { it.isNotEmpty() }?.let {
            header(2) { text("SuperInterfaces") }
            linkTable(it)
        }
        c.commentsData.forEach {
            it.children.forEach {
                header(3) { text(it.toHeaderString()) }
                comment(it.root)
                text("\n")
            }
        }
        block("Constructors", 2, ContentKind.Functions, c.constructors, c.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
        }

        block("Functions", 2, ContentKind.Functions, c.functions, c.platformData) {
            link(it.name, it.dri)
            signature(it)
            text(it.briefDocTagString)
        }
        block("Properties", 2, ContentKind.Properties, c.properties, c.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
    }

    private fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}