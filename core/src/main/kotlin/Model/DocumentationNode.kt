package org.jetbrains.dokka.Model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

class Module(val packages: List<Package>) : DocumentationNode<Nothing>() {
    override val dri: DRI = DRI.topLevel
    override val children: List<Package> = packages
    override val extra: MutableSet<Extra> = mutableSetOf()
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode<Nothing>() {
    val name = dri.packageName.orEmpty()
}

class Class(
    override val dri: DRI,
    val name: String,
    val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val expectDescriptor: Descriptor<ClassDescriptor>?,
    override val actualDescriptors: List<Descriptor<ClassDescriptor>>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode<ClassDescriptor>()

class Function(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val expectDescriptor: Descriptor<FunctionDescriptor>?,
    override val actualDescriptors: List<Descriptor<FunctionDescriptor>>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : CallableNode<FunctionDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    override val expectDescriptor: Descriptor<PropertyDescriptor>?,
    override val actualDescriptors: List<Descriptor<PropertyDescriptor>>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : CallableNode<PropertyDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

// TODO: treat named Parameters and receivers differently
class Parameter(
    override val dri: DRI,
    val name: String?,
    override val actualDescriptors: List<Descriptor<ParameterDescriptor>>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : DocumentationNode<ParameterDescriptor>() {
    override val children: List<DocumentationNode<*>>
        get() = emptyList()
}

class Descriptor<out T : DeclarationDescriptor>(
    val descriptor: T,
    val docTag: KDocTag?,
    val links: Map<String, DRI>,
    val platformData: List<PlatformData>
) : DeclarationDescriptor by descriptor {

    override fun equals(other: Any?): Boolean =
        other is Descriptor<*> && (
                descriptor.toString() == other.descriptor.toString() &&
                        docTag?.text == other.docTag?.text &&
                        links == other.links)

    override fun hashCode(): Int =
        listOf(descriptor.toString(), docTag?.text, links).hashCode()
}

abstract class DocumentationNode<out T : DeclarationDescriptor> {
    open val expectDescriptor: Descriptor<T>? = null
    open val actualDescriptors: List<Descriptor<T>> = emptyList()
    val descriptors by lazy { listOfNotNull(expectDescriptor) + actualDescriptors }
    val platformData by lazy { descriptors.flatMap { it.platformData }.toSet() }

    abstract val dri: DRI

    abstract val children: List<DocumentationNode<*>>

    override fun toString(): String {
        return "${javaClass.simpleName}($dri)" + briefDocstring.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
    }

    override fun equals(other: Any?) = other is DocumentationNode<*> && this.dri == other.dri

    override fun hashCode() = dri.hashCode()

    val commentsData: List<Pair<String, Map<String, DRI>>>
        get() = descriptors.mapNotNull { it.docTag?.let { tag -> Pair(tag.getContent(), it.links) } }

    val briefDocstring: String
        get() = descriptors.firstOrNull()?.docTag?.getContent().orEmpty().shorten(40)

    open val extra: MutableSet<Extra> = mutableSetOf()
}

abstract class ScopeNode<out T : ClassOrPackageFragmentDescriptor> : DocumentationNode<T>() {
    abstract val functions: List<Function>
    abstract val properties: List<Property>
    abstract val classes: List<Class>

    override val children: List<DocumentationNode<MemberDescriptor>>
        get() = functions + properties + classes
}

abstract class CallableNode<out T : CallableDescriptor> : DocumentationNode<T>() {
    abstract val receiver: Parameter?
}

private fun String.shorten(maxLength: Int) = lineSequence().first().let {
    if (it.length != length || it.length > maxLength) it.take(maxLength - 3) + "..." else it
}

fun DocumentationNode<*>.walk(process: DocumentationNode<*>.() -> Unit) {
    this.process()
    this.children.forEach { it.process() }
}

fun DocumentationNode<*>.dfs(predicate: (DocumentationNode<*>) -> Boolean): DocumentationNode<*>? =
    if (predicate(this)) {
        this
    } else {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    }

fun DocumentationNode<*>.findAll(predicate: (DocumentationNode<*>) -> Boolean): Set<DocumentationNode<*>> {
    val found = mutableSetOf<DocumentationNode<*>>()
    if (predicate(this)) {
        found.add(this)
    } else {
        this.children.asSequence().mapNotNull { it.findAll(predicate) }.forEach { found.addAll(it) }
    }
    return found
}

interface Extra