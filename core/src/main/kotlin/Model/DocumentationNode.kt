package org.jetbrains.dokka.Model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

class Module(val packages: List<Package>) : DocumentationNode<Nothing>() {
    override val docTag: KDocTag? = null

    override val dri: DRI = DRI.topLevel

    override val children: List<Package> = packages
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>
) : ScopeNode<Nothing>() {
    val name = dri.packageName.orEmpty()

    override val docTag: KDocTag? = null
}

class Class(
    override val dri: DRI,
    val name: String,
    val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val docTag: KDocTag?,
    override val descriptor: ClassDescriptor
) : ScopeNode<ClassDescriptor>()

class Function(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val docTag: KDocTag?,
    override val descriptor: FunctionDescriptor
) : CallableNode<FunctionDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    override val docTag: KDocTag?,
    override val descriptor: PropertyDescriptor
) : CallableNode<PropertyDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

class Parameter(
    override val dri: DRI,
    val name: String?,
    override val docTag: KDocTag?,
    override val descriptor: ParameterDescriptor
) : DocumentationNode<ParameterDescriptor>() {
    override val children: List<DocumentationNode<*>>
        get() = emptyList()
}

abstract class DocumentationNode<out T : DeclarationDescriptor> {
    open val descriptor: T? = null

    abstract val docTag: KDocTag? // TODO: replace in the future with more robust doc-comment model

    abstract val dri: DRI

    abstract val children: List<DocumentationNode<*>>

    override fun toString(): String {
        return "${javaClass.simpleName}($dri)" + briefDocstring.takeIf { it.isNotBlank() }?.let { " [$it]"}.orEmpty()
    }

    override fun equals(other: Any?) = other is DocumentationNode<*> && this.dri == other.dri

    override fun hashCode() = dri.hashCode()

    val rawDocstring: String
        get() = docTag?.getContent().orEmpty()

    val briefDocstring: String
        get() = rawDocstring.shorten(40)
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