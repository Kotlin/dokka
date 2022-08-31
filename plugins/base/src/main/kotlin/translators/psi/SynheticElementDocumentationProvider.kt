package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.doc.*

/**
 * Adaptation of text taken from [java.lang.Enum.valueOf]
 */
internal val JAVA_ENUM_VALUE_OF_DOCUMENTATION = DocumentationNode(listOf(
    Description(
        P(listOf(
            Text(
                "Returns the enum constant of the this enum type with the " +
                        "specified name. The name must match exactly an identifier used " +
                        "to declare an enum constant in this type.  (Extraneous whitespace " +
                        "characters are not permitted.)"
            )
        ))
    ),
    Param(
        name = "name",
        root = P(listOf(
            Text("the name of the constant to return")
        ))
    ),
    Return(root = P(listOf(
        Text("the enum constant of this enum type with the specified name")
    ))),
    Throws(
        name = "java.lang.IllegalArgumentException",
        exceptionAddress = DRI(
            packageName = "java.lang",
            classNames = "IllegalArgumentException",
            target = PointingToDeclaration
        ),
        root = P(listOf(
            Text("if this enum type has no constant with the specified name")
        ))
    ),
    Throws(
        name = "java.lang.NullPointerException",
        exceptionAddress = DRI(
            packageName = "java.lang",
            classNames = "NullPointerException",
            target = PointingToDeclaration
        ),
        root = P(listOf(
            Text("if name is null")
        ))
    ),
    Since(root = P(listOf(
        Text("1.5")
    )))
))

/**
 * Adaptation of text from the Java SE specification.
 *
 * See https://docs.oracle.com/javase/specs/jls/se18/html/jls-8.html#jls-8.9.3
 */
internal val JAVA_ENUM_VALUES_DOCUMENTATION = DocumentationNode(listOf(
    Description(
        P(listOf(
            Text(
                "Returns an array containing the enum constants of this type, " +
                        "in the same order as they appear in the body of the declaration"
            )
        ))
    )
))


internal fun PsiMethod.isDocumentedSyntheticMethod() = this.isSyntheticEnumValuesMethod() || this.isSyntheticEnumValueOfMethod()

internal fun PsiMethod.getSyntheticMethodDocumentation(): DocumentationNode? {
    return when {
        this.isSyntheticEnumValuesMethod() -> JAVA_ENUM_VALUES_DOCUMENTATION
        this.isSyntheticEnumValueOfMethod() -> JAVA_ENUM_VALUE_OF_DOCUMENTATION
        else -> null
    }
}

private fun PsiMethod.isSyntheticEnumValuesMethod() = this.isSyntheticEnumFunction() && this.name == "values"
private fun PsiMethod.isSyntheticEnumValueOfMethod() = this.isSyntheticEnumFunction() && this.name == "valueOf"
private fun PsiMethod.isSyntheticEnumFunction() = this is SyntheticElement && this.containingClass?.isEnum == true

