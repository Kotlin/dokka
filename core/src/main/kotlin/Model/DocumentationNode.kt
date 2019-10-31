package org.jetbrains.dokka

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.kotlin.descriptors.*

class Module(val packages: List<Package>) : DocumentationNode<Nothing>(DRI.topLevel, DRI.topLevel) {
    override val children: List<Package>
        get() = packages
}

class Package(
    val name: String,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>
) : ScopeNode<Nothing>(DRI(packageName = name), DRI.topLevel)

class Class(
    val name: String,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val descriptor: ClassDescriptor,
    parent: DRI
) : ScopeNode<ClassDescriptor>(parent.withClass(name), parent)

class Function(
    val name: String,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val descriptor: FunctionDescriptor,
    parent: DRI
) : CallableNode<FunctionDescriptor>(parent, descriptor) {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    val name: String,
    override val receiver: Parameter?,
    override val descriptor: PropertyDescriptor,
    parent: DRI
) : CallableNode<PropertyDescriptor>(parent, descriptor) {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

class Parameter(
    val name: String,
    override val descriptor: ParameterDescriptor,
    parent: DRI,
    index: Int
) : DocumentationNode<ParameterDescriptor>(parent, parent.copy(target = index)) {
    override val children: List<DocumentationNode<*>>
        get() = emptyList()
}

abstract class DocumentationNode<out T : DeclarationDescriptor>(
    val dri: DRI,
    val parent: DRI
) {
    open val descriptor: T? = null

    abstract val children: List<DocumentationNode<*>>

    override fun toString(): String {
        return "${javaClass.name}($dri)"
    }

    override fun equals(other: Any?) = other is DocumentationNode<*> && this.dri == other.dri

    override fun hashCode() = dri.hashCode()
}

abstract class ScopeNode<out T : ClassOrPackageFragmentDescriptor>(
    dri: DRI,
    parent: DRI
) : DocumentationNode<T>(dri, parent) {
    abstract val functions: List<Function>
    abstract val properties: List<Property>
    abstract val classes: List<Class>

    override val children: List<DocumentationNode<MemberDescriptor>>
        get() = functions + properties + classes
}

abstract class CallableNode<out T: CallableDescriptor>(
    parent: DRI,
    descriptor: CallableDescriptor
) : DocumentationNode<T>(parent.copy(callable = Callable.from(descriptor)), parent) {
    abstract val receiver: Parameter?
}