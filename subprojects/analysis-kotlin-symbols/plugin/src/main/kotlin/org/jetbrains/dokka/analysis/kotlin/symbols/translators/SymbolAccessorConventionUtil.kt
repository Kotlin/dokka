package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.analysis.kotlin.symbols.utils.firstNotNullOfOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName

// Copy-pasted from descriptors analysis
internal data class DescriptorFunctionsHolder(
    val regularFunctions: List<KtFunctionSymbol>,
    val accessors: Map<KtJavaFieldSymbol, DescriptorAccessorHolder>
)

internal data class DescriptorAccessorHolder(
    val getter: KtFunctionSymbol? = null,
    val setter: KtFunctionSymbol? = null
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
    fields: List<KtJavaFieldSymbol>,
    functions: List<KtFunctionSymbol>
): DescriptorFunctionsHolder {
    val (javaMethods, kotlinFunctions) = functions.partition { it.origin == KtSymbolOrigin.JAVA }
    if (javaMethods.isEmpty()) {
        return DescriptorFunctionsHolder(regularFunctions = kotlinFunctions, emptyMap())
    }

    val fieldsByName = fields.associateBy { it.name.asString() }
    val regularFunctions = mutableListOf<KtFunctionSymbol>()
    regularFunctions.addAll(kotlinFunctions)

    val accessors = mutableMapOf<KtJavaFieldSymbol, DescriptorAccessorHolder>()
    javaMethods.forEach { function ->
        val possiblePropertyNamesForFunction = function.toPossiblePropertyNames()
        val field = possiblePropertyNamesForFunction.firstNotNullOfOrNull { fieldsByName[it] }
        if (field != null && function.isAccessorFor(field)) {
            accessors.compute(field) { prop, accessorHolder ->
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
    propertyAccessors: MutableMap<KtJavaFieldSymbol, DescriptorAccessorHolder>
): List<KtFunctionSymbol> {
    val nonAccessors = mutableListOf<KtFunctionSymbol>()
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

private fun KtFunctionSymbol.toPossiblePropertyNames(): List<String> {
    val stringName = this.name.asString()
    return when {
        JvmAbi.isSetterName(stringName) -> propertyNamesBySetMethodName(this.name).map { it.asString() }
        JvmAbi.isGetterName(stringName) -> propertyNamesByGetMethod(this)
        else -> listOf()
    }
}

private fun propertyNamesByGetMethod(functionSymbol: KtFunctionSymbol): List<String> {
    val stringName = functionSymbol.name.asString()
    // In java, the convention for boolean property accessors is as follows:
    // - `private boolean active;`
    // - `private boolean isActive();`
    //
    // Whereas in Kotlin, because there are no explicit accessors, the convention is
    // - `val isActive: Boolean`
    //
    // This makes it difficult to guess the name of the accessor property in case of Java
    val javaPropName = if (functionSymbol.origin == KtSymbolOrigin.JAVA && JvmAbi.startsWithIsPrefix(stringName)) {
        val javaPropName = stringName.removePrefix("is").let { newName ->
            newName.replaceFirst(newName[0], newName[0].toLowerCase())
        }
        javaPropName
    } else {
        null
    }
    val kotlinPropName = propertyNameByGetMethodName(functionSymbol.name)?.asString()
    return listOfNotNull(javaPropName, kotlinPropName)
}

private fun KtFunctionSymbol.isAccessorFor(field: KtJavaFieldSymbol): Boolean {
    return (this.isGetterFor(field) || this.isSetterFor(field))
            && !field.visibility.isPublicAPI
            && this.visibility.isPublicAPI
}

private fun KtFunctionSymbol.isGetterFor(field: KtJavaFieldSymbol): Boolean {
    return this.returnType == field.returnType
            && this.valueParameters.isEmpty()
}

private fun KtFunctionSymbol.isSetterFor(field: KtJavaFieldSymbol): Boolean {
    return this.valueParameters.size == 1
            && this.valueParameters[0].returnType == field.returnType
}

