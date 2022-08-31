package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorFactory

/**
 * Copy-pasted from kotlin stdlib sources, it should be present in Enum.kt file
 *
 * See https://github.com/JetBrains/kotlin/blob/master/core/builtins/native/kotlin/Enum.kt
 */
internal val KOTLIN_ENUM_VALUE_OF_DOCUMENTATION = DocumentationNode(listOf(
    Description(
        P(listOf(
            Text(
                "Returns the enum constant of this type with the specified name. " +
                        "The string must match exactly an identifier used to declare an enum " +
                        "constant in this type. (Extraneous whitespace characters are not permitted.)"
            )
        ))
    ),
    Throws(
        name = "kotlin.IllegalArgumentException",
        exceptionAddress = DRI(
            packageName = "kotlin",
            classNames = "IllegalArgumentException",
            target = PointingToDeclaration
        ),
        root = P(listOf(
            Text("if this enum type has no constant with the specified name")
        ))
    )
))

/**
 * Copy-pasted from kotlin stdlib sources, it should be present in Enum.kt file
 *
 * See https://github.com/JetBrains/kotlin/blob/master/core/builtins/native/kotlin/Enum.kt
 */
internal val KOTLIN_ENUM_VALUES_DOCUMENTATION = DocumentationNode(listOf(
    Description(
        P(listOf(
            Text(
                "Returns an array containing the constants of this enum type, in the order " +
                        "they're declared. This method may be used to iterate over the constants."
            )
        ))
    )
))

internal fun FunctionDescriptor.isDocumentedSyntheticFunction() =
    DescriptorFactory.isEnumValuesMethod(this) || DescriptorFactory.isEnumValueOfMethod(this)

internal fun FunctionDescriptor.getSyntheticFunctionDocumentation(): DocumentationNode? {
    return when {
        DescriptorFactory.isEnumValuesMethod(this) -> KOTLIN_ENUM_VALUES_DOCUMENTATION
        DescriptorFactory.isEnumValueOfMethod(this) -> KOTLIN_ENUM_VALUE_OF_DOCUMENTATION
        else -> null
    }
}
