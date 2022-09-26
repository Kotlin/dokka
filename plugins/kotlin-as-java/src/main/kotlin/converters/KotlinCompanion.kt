package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DProperty


internal class KotlinCompanion(private val companion: DObject?) {

    fun staticFunctions(): List<DFunction> {
        if(companion == null) return emptyList()
        return companion.functions.filter { it.isJvmStatic }
    }

    /**
     * @return properties that will be visible as static for java.
     * See [Static fields](https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields)
     */
    fun staticProperties(): List<DProperty> {
        if(companion == null) return emptyList()
        return companion.properties.filter { it.isJvmField || it.isConst || it.isLateInit}
    }

    fun asJava():DObject? {
        if(companion == null) return null
        if(companion.hasNothingToRender()) return null

        return companion.asJava(
            excludedProps = staticProperties(),
            excludedFunctions = staticFunctions()
        )
    }

    /**
     * Hide companion object if there isn't members of parents.
     * Properties and functions that are moved to outer class are not counted as members.
     */
    private fun DObject.hasNothingToRender(): Boolean{
        val nonStaticPropsCount = properties.minus(staticProperties().toSet()).size
        val nonStaticFunctionsCount = functions.minus(staticFunctions().toSet()).size
        val classLikesCount = classlikes.size
        val superTypesCount = supertypes.values.firstOrNull()?.size ?: 0

        return nonStaticFunctionsCount + nonStaticPropsCount +
                classLikesCount + superTypesCount == 0
    }
}

