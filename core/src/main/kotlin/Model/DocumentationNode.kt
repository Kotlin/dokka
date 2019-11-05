package org.jetbrains.dokka.Model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

class Module(val packages: List<Package>) : DocumentationNode<Nothing>() {
    override val docTags: List<KDocTag> = emptyList()

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

    override val docTags: List<KDocTag> = emptyList()
}

class Class(
    override val dri: DRI,
    val name: String,
    val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val docTags: List<KDocTag>,
    override val descriptors: List<ClassDescriptor>
) : ScopeNode<ClassDescriptor>()

class Function(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val docTags: List<KDocTag>,
    override val descriptors: List<FunctionDescriptor>
) : CallableNode<FunctionDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    override val docTags: List<KDocTag>,
    override val descriptors: List<PropertyDescriptor>
) : CallableNode<PropertyDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

// TODO: treat named Parameters and receivers differently
class Parameter(
    override val dri: DRI,
    val name: String?,
    override val docTags: List<KDocTag>,
    override val descriptors: List<ParameterDescriptor>
) : DocumentationNode<ParameterDescriptor>() {
    override val children: List<DocumentationNode<*>>
        get() = emptyList()
}

abstract class DocumentationNode<out T : DeclarationDescriptor> {
    open val descriptors: List<T> = emptyList()

    abstract val docTags: List<KDocTag> // TODO: replace in the future with more robust doc-comment model

    abstract val dri: DRI

    abstract val children: List<DocumentationNode<*>>

    override fun toString(): String {
        return "${javaClass.simpleName}($dri)" + briefDocstring.takeIf { it.isNotBlank() }?.let { " [$it]"}.orEmpty()
    }

    override fun equals(other: Any?) = other is DocumentationNode<*> && this.dri == other.dri

    override fun hashCode() = dri.hashCode()

    val rawDocstrings: List<String>
        get() = docTags.map(KDocTag::getContent)

    val briefDocstring: String
        get() = rawDocstrings.firstOrNull().orEmpty().shorten(40)
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