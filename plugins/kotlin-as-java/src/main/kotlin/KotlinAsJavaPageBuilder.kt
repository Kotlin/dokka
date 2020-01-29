package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.kotlinAsJava.conversions.asJava
import org.jetbrains.dokka.kotlinAsJava.conversions.asStatic
import org.jetbrains.dokka.kotlinAsJava.conversions.withClass
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.descriptors.KotlinClassKindTypes

class KotlinAsJavaPageBuilder(val rootContentGroup: RootContentBuilder) {
    fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    data class FunsAndProps(val key: String, val funs: List<Function>, val props: List<Property>)

    private fun pageForPackage(p: Package): PackagePageNode {

        val funs = p.functions.filter { it.sourceLocation != null }.groupBy { function ->
            function.sourceLocation!!.let { it.split("/").last().split(".").first() + "Kt" }
        }

        val props = p.properties.filter { it.sourceLocation != null }.groupBy { property ->
            property.sourceLocation!!.let { it.split("/").last().split(".").first() + "Kt" }
        }

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
                expected = null
            )
        }).map { it.asJava() }

        return PackagePageNode(
            p.name, contentForPackage(p, classes), p.dri, p,
            classes.map(::pageForClass)
        )
    }

    private fun pageForMember(m: CallableNode): MemberPageNode =
        when (m) {
            is Function ->
                MemberPageNode(m.name, contentForFunction(m), m.dri, m)
            else -> throw IllegalStateException("$m should not be present here")
        }

    private fun pageForClass(c: Class): ClassPageNode =
        ClassPageNode(c.name, contentForClass(c), c.dri, c,
            c.constructors.map { pageForMember(it) } +
                    c.classes.map { pageForClass(it) } +
                    c.functions.map { pageForMember(it) })

    private fun contentForModule(m: Module) = group(m) {
        header(1) { text("root") }
        block("Packages", 2, ContentKind.Packages, m.packages, m.platformData) {
            link(it.name, it.dri)
        }
        text("Index\n")
        text("Link to allpage here")
    }

    private fun contentForPackage(p: Package, nClasses: List<Class>) = group(p) {
        header(1) { text("Package ${p.name}") }
        block("Types", 2, ContentKind.Properties, nClasses, p.platformData) {
            link(it.name, it.dri)
            text(it.briefDocTagString)
        }
    }

    private fun contentForClass(c: Class) = group(c) {
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

    private fun contentForFunction(f: Function) = group(f) {
        header(1) { text(f.name) }
        signature(f)
        text(" ")
        f.commentsData.forEach { it.children.forEach { comment(it.root) } }
        block("Parameters", 2, ContentKind.Parameters, f.children, f.platformData) { param ->
            text(param.name ?: "<receiver>")
            param.commentsData.forEach { node -> node.children.forEach { comment(it.root) } }
        }
    }

    private fun TagWrapper.toHeaderString() = this.javaClass.toGenericString().split('.').last()

    private fun group(node: Documentable, content: PageContentBuilderFunction) =
        rootContentGroup(node, ContentKind.Main, content)
}

typealias RootContentBuilder = (Documentable, Kind, PageContentBuilderFunction) -> ContentGroup

class JavaTypeWrapper(
    override val constructorFqName: String?,
    override val arguments: List<TypeWrapper>,
    override val dri: DRI?,
    val isPrimitive: Boolean = false
) : TypeWrapper {
    override val constructorNamePathSegments: List<String>
        get() = constructorFqName?.split(".")?.flatMap { it.split("/") } ?: emptyList()
}