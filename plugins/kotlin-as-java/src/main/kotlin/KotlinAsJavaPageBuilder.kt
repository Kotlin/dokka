package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.TagWrapper
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.descriptors.KotlinClassKindTypes
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

class KotlinAsJavaPageBuilder(val rootContentGroup: RootContentBuilder) {
    fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    private fun pageForPackage(p: Package): PackagePageNode {
        fun Function.withClass(className: String, dri: DRI): Function {
            val nDri = dri.withClass(className).copy(
                callable = getDescriptor()?.let { Callable.from(it) }
            )
            return Function(
                nDri, name, returnType, isConstructor, receiver, parameters, expected, actual, extra, sourceLocation
            )
        }

        fun Function.asStatic() = also { it.extra.add(STATIC) }

        fun Property.withClass(className: String, dri: DRI): Property {
            val nDri = dri.withClass(className).copy(
                callable = getDescriptor()?.let { Callable.from(it) }
            )
            return Property(
                nDri, name, receiver, expected, actual, extra, type, accessors, isVar, sourceLocation
            )
        }

        val funs = p.functions.filter { it.sourceLocation != null }.groupBy { function ->
            function.sourceLocation!!.let { it.split("/").last().split(".").first() + "Kt" }
        }

        val props = p.properties.filter { it.sourceLocation != null }.groupBy { property ->
            property.sourceLocation!!.let { it.split("/").last().split(".").first() + "Kt" }
        }

        val fClasses = funs.map { (key, value) ->
            val nDri = p.dri.withClass(key)
            Class(
                dri = nDri,
                name = key,
                kind = KotlinClassKindTypes.CLASS,
                constructors = emptyList(),
                functions = value.map { it.withClass(key, nDri).asStatic() },
                properties = emptyList(),
                classes = emptyList(),
                actual = emptyList(),
                expected = null
            )
        }

        val pClasses = props.map { (key, value) ->
            val nDri = p.dri.withClass(key)
            Class(
                dri = nDri,
                name = key,
                kind = KotlinClassKindTypes.CLASS,
                constructors = emptyList(),
                functions = emptyList(),
                properties = value.map { it.withClass(key, nDri) },
                classes = emptyList(),
                actual = emptyList(),
                expected = null
            )
        }

        fun Class.merge(other: Class) = Class(
            dri = dri,
            name = name,
            kind = KotlinClassKindTypes.CLASS,
            constructors = emptyList(),
            functions = functions + other.functions,
            properties = properties + other.properties,
            classes = emptyList(),
            actual = emptyList(),
            expected = null
        )

        val classes = (fClasses + pClasses).groupBy { it.name }.map { (_, v) ->
            v.reduce(Class::merge)
        }

        return PackagePageNode(
            p.name, contentForPackage(p, classes), p.dri, p,
            (p.classes + classes).map(::pageForClass)
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
        block("Types", 2, ContentKind.Properties, p.classes + nClasses, p.platformData) {
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

        val functions = c.functions + c.properties.flatMap { it.accessors }
        block("Functions", 2, ContentKind.Functions, functions, c.platformData) {
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

    private fun group(node: Documentable, content: KotlinAsJavaPageContentBuilderFunction) =
        rootContentGroup(node, ContentKind.Main, content)

    private fun Function.getDescriptor(): FunctionDescriptor? = platformInfo.mapNotNull { it.descriptor }.firstOrNull()
        ?.let { it as? FunctionDescriptor }

    private fun Property.getDescriptor(): PropertyDescriptor? = platformInfo.mapNotNull { it.descriptor }.firstOrNull()
        ?.let { it as? PropertyDescriptor }
}

typealias RootContentBuilder = (Documentable, Kind, KotlinAsJavaPageContentBuilderFunction) -> ContentGroup