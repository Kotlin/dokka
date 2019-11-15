package org.jetbrains.dokka.Model.transformers

import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

internal object DocumentationNodesMerger : DocumentationNodeTransformer {
    override fun invoke(original: Module) = Module(
        original.packages.map { mergePackageContent(it) }
    )
    override fun invoke(modules: Collection<Module>): Module =
        Module(merge(modules.flatMap { it.packages }, Package::mergeWith))
}

private fun mergePackageContent(original: Package) = Package(
    original.dri,
    merge(original.functions, Function::mergeWith),
    merge(original.properties, Property::mergeWith),
    merge(original.classes, Class::mergeWith)
)

private fun <T: DocumentationNode<*>> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right)}
        .values.toList()

fun <T:DeclarationDescriptor> Descriptor<T>.mergeWith(other: Descriptor<T>?) = Descriptor(
    descriptor,
    docTag,
    links,
    (platformData + (other?.platformData ?: emptyList())).distinct()
)

fun <T:DeclarationDescriptor> List<Descriptor<T>>.merge() : List<Descriptor<T>> =
    groupingBy { it.descriptor }.reduce {
        _, left, right -> left.mergeWith(right)
    }.values.toList()

fun Function.mergeWith(other: Function) = Function(
    dri,
    name,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    merge(parameters + other.parameters, Parameter::mergeWith),
    expectDescriptor?.mergeWith(other.expectDescriptor),
    (actualDescriptors + other.actualDescriptors).merge()
)

fun Property.mergeWith(other: Property) = Property(
    dri,
    name,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    expectDescriptor?.mergeWith(other.expectDescriptor),
    (actualDescriptors + other.actualDescriptors).merge()
)

fun Class.mergeWith(other: Class) = Class(
    dri,
    name,
    merge(constructors + other.constructors, Function::mergeWith),
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classes + other.classes, Class::mergeWith),
    expectDescriptor?.mergeWith(other.expectDescriptor),
    (actualDescriptors + other.actualDescriptors).merge()
)

fun Parameter.mergeWith(other: Parameter) = Parameter(
    dri,
    name,
    (actualDescriptors + other.actualDescriptors).merge()
)

fun Package.mergeWith(other: Package) = Package(
    dri,
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classes + other.classes, Class::mergeWith)
)