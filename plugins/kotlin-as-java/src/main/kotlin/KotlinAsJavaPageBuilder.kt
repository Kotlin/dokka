package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.conversions.asJava
import org.jetbrains.dokka.kotlinAsJava.conversions.asStatic
import org.jetbrains.dokka.kotlinAsJava.conversions.withClass
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*
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

        val classes = (p.classlikes + zipped.map { (key, funs, props) ->
            val dri = p.dri.withClass(key)
            Class(
                dri = dri,
                name = key,
                kind = KotlinClassKindTypes.CLASS,
                constructors = emptyList(),
                functions = funs.map { it.withClass(key, dri).asStatic() },
                properties = props.map { it.withClass(key, dri) },
                classlikes = emptyList(),
                actual = emptyList(),
                expected = null,
                visibility = p.platformData.map { it to Visibilities.PUBLIC }.toMap()
            )
        }).map { it.asJava() }

        return PackagePageNode(
            p.name, contentForPackage(p, classes), setOf(p.dri), p,
            classes.map(::pageForClasslike)
        )
    }

    private fun contentForPackage(p: Package, nClasses: List<Classlike>) = group(p) {
        header(1) { text("Package ${p.name}") }
        block("Types", 2, ContentKind.Properties, nClasses, p.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
    }

    private fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()
}