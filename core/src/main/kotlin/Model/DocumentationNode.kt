package org.jetbrains.dokka.Model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.descriptors.*

class Module(val packages: List<Package>) : DocumentationNode<Nothing>() {
    override val dri: DRI
        get() = DRI.topLevel

    override val children: List<Package>
        get() = packages
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>
) : ScopeNode<Nothing>() {
    val name = dri.packageName.orEmpty()
}

class Class(
    override val dri: DRI,
    val name: String,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val descriptor: ClassDescriptor
) : ScopeNode<ClassDescriptor>()

class Function(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val descriptor: FunctionDescriptor
) : CallableNode<FunctionDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    val name: String,
    override val receiver: Parameter?,
    override val descriptor: PropertyDescriptor
) : CallableNode<PropertyDescriptor>() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

class Parameter(
    override val dri: DRI,
    val name: String?,
    override val descriptor: ParameterDescriptor
) : DocumentationNode<ParameterDescriptor>() {
    override val children: List<DocumentationNode<*>>
        get() = emptyList()
}

abstract class DocumentationNode<out T : DeclarationDescriptor> {
    open val descriptor: T? = null

    abstract val dri: DRI

    abstract val children: List<DocumentationNode<*>>

    override fun toString(): String {
        return "${javaClass.simpleName}($dri)"
    }

    override fun equals(other: Any?) = other is DocumentationNode<*> && this.dri == other.dri

    override fun hashCode() = dri.hashCode()
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