package org.jetbrains.dokka

import org.jetbrains.dokka.links.ClassReference
import org.jetbrains.kotlin.descriptors.*

class DocumentationNodes {

    class Module(name: String, parent: DocumentationNode<*>? = null):
        DocumentationNode<Nothing>(name, parent = parent)

    class Package(name: String, parent: DocumentationNode<*>):
        DocumentationNode<Nothing>(name, parent = parent)

    class Class(name: String, descriptor: ClassDescriptor, parent: DocumentationNode<*>? = null) :
        DocumentationNode<ClassDescriptor>(name, descriptor, parent) {
        override val classLike: Boolean = true
        val supertypes = mutableListOf<ClassReference>()
        val isInterface: Boolean
            get() = descriptor?.kind == ClassKind.CLASS
        val isEnum: Boolean
            get() = descriptor?.kind == ClassKind.ENUM_CLASS
        val isEnumEntry: Boolean
            get() = descriptor?.kind == ClassKind.ENUM_ENTRY
        val isAnnotationClass: Boolean
            get() = descriptor?.kind == ClassKind.ANNOTATION_CLASS
        val isObject: Boolean
            get() = descriptor?.kind == ClassKind.OBJECT
    }

    class Constructor(name: String, descriptor: ConstructorDescriptor) :
        DocumentationNode<ConstructorDescriptor>(name, descriptor) {
        override val memberLike = true
    }

    class Function(name: String, descriptor: FunctionDescriptor) :
        DocumentationNode<FunctionDescriptor>(name, descriptor) {
        override val memberLike = true
    }

    class Property(name: String, descriptor: PropertyDescriptor) :
        DocumentationNode<PropertyDescriptor>(name, descriptor) {
        override val memberLike = true
    }
/*
    class Field(name: String, descriptor: FieldDescriptor) :
        DocumentationNode<FieldDescriptor>(name, descriptor) {
        override val memberLike = true
    }*/

    class Parameter(name: String, descriptor: ParameterDescriptor?) :
        DocumentationNode<ParameterDescriptor>(name, descriptor)
/*
    class Annotation(name: String, descriptor: AnnotationDescriptor?) :
        DocumentationNode<AnnotationDescriptor>(name, descriptor)*/
}

abstract class DocumentationNode<T: DeclarationDescriptor>(
    var name: String,
    val descriptor: T? = null,
    val parent: DocumentationNode<*>? = null
) {

    private val children = mutableListOf<DocumentationNode<*>>()

    open val classLike = false

    open val memberLike = false

    fun addChild(child: DocumentationNode<*>) =
        children.add(child)

    override fun toString(): String {
        return "${javaClass.name}:$name"
    }
}