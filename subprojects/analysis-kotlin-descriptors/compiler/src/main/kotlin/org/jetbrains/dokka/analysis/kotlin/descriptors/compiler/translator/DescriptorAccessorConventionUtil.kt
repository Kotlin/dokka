/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName

internal data class DescriptorFunctionsHolder(
    val regularFunctions: List<FunctionDescriptor>,
    val accessors: Map<PropertyDescriptor, DescriptorAccessorHolder>
)

internal data class DescriptorAccessorHolder(
    val getter: FunctionDescriptor? = null,
    val setter: FunctionDescriptor? = null
)

/**
 * Separate regular Kotlin/Java functions and inherited Java accessors
 * to properly display properties inherited from Java.
 *
 * Take this example:
 * ```
 * // java
 * public class JavaClass {
 *     private int a = 1;
 *     public int getA() { return a; }
 *     public void setA(int a) { this.a = a; }
 * }
 *
 * // kotlin
 * class Bar : JavaClass() {
 *     fun foo() {}
 * }
 * ```
 *
 * It should result in:
 * - 1 regular function `foo`
 * - Map a=[`getA`, `setA`]
 */
internal fun splitFunctionsAndInheritedAccessors(
    properties: List<PropertyDescriptor>,
    functions: List<FunctionDescriptor>
): DescriptorFunctionsHolder {
    val (javaMethods, kotlinFunctions) = functions.partition { it is JavaMethodDescriptor }
    if (javaMethods.isEmpty()) {
        return DescriptorFunctionsHolder(regularFunctions = kotlinFunctions, emptyMap())
    }

    val propertiesByName = properties.associateBy { it.name.asString() }
    val regularFunctions = ArrayList<FunctionDescriptor>(kotlinFunctions)

    val accessors = mutableMapOf<PropertyDescriptor, DescriptorAccessorHolder>()
    javaMethods.forEach { function ->
        val possiblePropertyNamesForFunction = function.toPossiblePropertyNames()
        val property = possiblePropertyNamesForFunction.firstNotNullOfOrNull { propertiesByName[it] }
        if (property != null && function.isAccessorFor(property)) {
            accessors.compute(property) { prop, accessorHolder ->
                if (function.isGetterFor(prop))
                    accessorHolder?.copy(getter = function) ?: DescriptorAccessorHolder(getter = function)
                else
                    accessorHolder?.copy(setter = function) ?: DescriptorAccessorHolder(setter = function)
            }
        } else {
            regularFunctions.add(function)
        }
    }

    val accessorLookalikes = removeNonAccessorsReturning(accessors)
    regularFunctions.addAll(accessorLookalikes)

    return DescriptorFunctionsHolder(regularFunctions, accessors)
}

/**
 * If a field has no getter, it's not accessible as a property from Kotlin's perspective,
 * but it still might have a setter lookalike. In this case, this "setter" should be just a regular function
 *
 * @return removed elements
 */
private fun removeNonAccessorsReturning(
    propertyAccessors: MutableMap<PropertyDescriptor, DescriptorAccessorHolder>
): List<FunctionDescriptor> {
    val nonAccessors = mutableListOf<FunctionDescriptor>()
    propertyAccessors.entries.removeIf { (_, accessors) ->
        if (accessors.getter == null && accessors.setter != null) {
            nonAccessors.add(accessors.setter)
            true
        } else {
            false
        }
    }
    return nonAccessors
}

private fun FunctionDescriptor.toPossiblePropertyNames(): List<String> {
    val stringName = this.name.asString()
    return when {
        JvmAbi.isSetterName(stringName) -> propertyNamesBySetMethodName(this.name).map { it.asString() }
        JvmAbi.isGetterName(stringName) -> propertyNamesByGetMethod(this)
        else -> listOf()
    }
}

private fun propertyNamesByGetMethod(functionDescriptor: FunctionDescriptor): List<String> {
    val stringName = functionDescriptor.name.asString()
    // In java, the convention for boolean property accessors is as follows:
    // - `private boolean active;`
    // - `private boolean isActive();`
    //
    // Whereas in Kotlin, because there are no explicit accessors, the convention is
    // - `val isActive: Boolean`
    //
    // This makes it difficult to guess the name of the accessor property in case of Java
    val javaPropName = if (functionDescriptor is JavaMethodDescriptor && JvmAbi.startsWithIsPrefix(stringName)) {
        val javaPropName = stringName.removePrefix("is").let { newName ->
            newName.replaceFirst(newName[0], newName[0].toLowerCase())
        }
        javaPropName
    } else {
        null
    }
    val kotlinPropName = propertyNameByGetMethodName(functionDescriptor.name)?.asString()
    return listOfNotNull(javaPropName, kotlinPropName)
}

private fun FunctionDescriptor.isAccessorFor(property: PropertyDescriptor): Boolean {
    return (this.isGetterFor(property) || this.isSetterFor(property))
            && !property.visibility.isPublicAPI
            && this.visibility.isPublicAPI
}

private fun FunctionDescriptor.isGetterFor(property: PropertyDescriptor): Boolean {
    return this.returnType == property.returnType
            && this.valueParameters.isEmpty()
}

private fun FunctionDescriptor.isSetterFor(property: PropertyDescriptor): Boolean {
    return this.valueParameters.size == 1
            && this.valueParameters[0].type == property.returnType
}

