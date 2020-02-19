package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.base.translators.documentables.DefaultPageBuilder
import org.jetbrains.dokka.base.translators.documentables.RootContentBuilder
import org.jetbrains.dokka.kotlinAsJava.conversions.asJava
import org.jetbrains.dokka.kotlinAsJava.conversions.asStatic
import org.jetbrains.dokka.kotlinAsJava.conversions.withClass
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PackagePageNode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi

fun DeclarationDescriptor.sourceLocation(): String? = this.findPsi()?.containingFile?.virtualFile?.path
fun <T : Documentable> List<T>.groupedByLocation(): Map<String, List<T>> =
    this.map { DescriptorCache[it.dri]?.sourceLocation() to it }
        .filter { it.first != null }.groupBy({ (location, _) ->
            location!!.let { it.split("/").last().split(".").first() + "Kt" }
        }) { it.second }

fun PlatformInfo.toClassPlatformInfo(inherited: List<DRI> = emptyList()) =
    ClassPlatformInfo(this, emptyList())

class KotlinAsJavaPageBuilder(rootContentGroup: RootContentBuilder) : DefaultPageBuilder(rootContentGroup) {

    override fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    data class FunsAndProps(val key: String, val funs: List<Function>, val props: List<Property>)

    override fun pageForPackage(p: Package): PackagePageNode {

        val funs = p.functions.groupedByLocation()

        val props = p.properties.groupedByLocation()

        val zipped = (funs.keys + props.keys)
            .map { k -> FunsAndProps(k, funs[k].orEmpty(), props[k].orEmpty()) }

        val classes = (p.classlikes + zipped.map { (key, funs, props) ->
            val dri = p.dri.withClass(key)
            val actual =
                (funs.flatMap { it.actual } + props.flatMap { it.actual }).distinct().map { it.toClassPlatformInfo() }
            Class(
                dri = dri,
                name = key,
                kind = KotlinClassKindTypes.CLASS,
                constructors = emptyList(),
                functions = funs.map { it.withClass(key, dri).asStatic() },
                properties = props.map { it.withClass(key, dri) },
                classlikes = emptyList(),
                actual = actual,
                expected = null,
                visibility = p.platformData.map { it to Visibilities.PUBLIC }.toMap(),
                typeParameters = emptyList()
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

    override fun contentForClasslike(c: Classlike): ContentGroup = when (c) {
        is Class -> contentForClass(c)
        is Enum -> contentForEnum(c)
        else -> throw IllegalStateException("$c should not be present here")
    }
}