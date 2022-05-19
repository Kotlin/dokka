package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.base.translators.firstNotNullOfOrNull
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName

internal data class DescriptorFunctionsHolder(
    val regularFunctions: List<FunctionDescriptor>,
    val accessors: Map<PropertyDescriptor, List<FunctionDescriptor>>
)

internal fun splitFunctionsAndAccessors(
    properties: List<PropertyDescriptor>,
    functions: List<FunctionDescriptor>
): DescriptorFunctionsHolder {
    val fieldsByName = properties.associateBy { it.name.asString() }
    val regularFunctions = mutableListOf<FunctionDescriptor>()
    val accessors = mutableMapOf<PropertyDescriptor, MutableList<FunctionDescriptor>>()
    functions.forEach { function ->
        val possiblePropertyNamesForFunction = function.toPossiblePropertyNames()
        val field = possiblePropertyNamesForFunction.firstNotNullOfOrNull { fieldsByName[it] }
        if (field != null) {
            accessors.getOrPut(field, ::mutableListOf).add(function)
        } else {
            regularFunctions.add(function)
        }
    }
    return DescriptorFunctionsHolder(regularFunctions, accessors)
}

internal fun FunctionDescriptor.toPossiblePropertyNames(): List<String> {
    val stringName = this.name.asString()
    return when {
        JvmAbi.isSetterName(stringName) -> propertyNamesBySetMethodName(this.name).map { it.asString() }
        JvmAbi.isGetterName(stringName) -> propertyNamesByGetMethod(this)
        else -> listOf()
    }
}

internal fun propertyNamesByGetMethod(functionDescriptor: FunctionDescriptor): List<String> {
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

internal fun FunctionDescriptor.isGetterFor(property: PropertyDescriptor): Boolean {
    return this.returnType == property.returnType
            && this.valueParameters.isEmpty()
            && !property.visibility.isPublicAPI
            && this.visibility.isPublicAPI
}

internal fun FunctionDescriptor.isSetterFor(property: PropertyDescriptor): Boolean {
    return this.valueParameters.size == 1
            && this.valueParameters[0].type == property.returnType
            && !property.visibility.isPublicAPI
            && this.visibility.isPublicAPI
}

