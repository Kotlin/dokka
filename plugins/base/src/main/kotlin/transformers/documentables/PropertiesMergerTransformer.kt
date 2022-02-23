package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name

class PropertiesMergerTransformer : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>) =
        modules.map { it.copy(packages = it.packages.map {
            it.mergeBeansAndField().copy(
                classlikes = it.classlikes.map { it.mergeBeansAndField() }
            )
        }) }

    private fun <T : Documentable> T.mergeBeansAndField(): T = when (this) {
        is DClass -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        is DEnum -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        is DInterface -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        is DObject -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        is DAnnotation -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        is DPackage -> {
            val (functions, properties) = mergePotentialBeansAndField(this.functions, this.properties)
            this.copy(functions = functions, properties = properties)
        }
        else -> this
    } as T

    private fun DFunction.getPropertyNameForFunction() =
        when {
            JvmAbi.isGetterName(name) -> propertyNameByGetMethodName(Name.identifier(name))?.asString()
            JvmAbi.isSetterName(name) -> propertyNamesBySetMethodName(Name.identifier(name)).firstOrNull()
                ?.asString()
            else -> null
        }

    private fun mergePotentialBeansAndField(
        functions: List<DFunction>,
        fields: List<DProperty>
    ): Pair<List<DFunction>, List<DProperty>> {
        val fieldNames = fields.associateBy { it.name }
        val accessors = mutableMapOf<DProperty, MutableList<DFunction>>()
        val regularMethods = mutableListOf<DFunction>()
        functions.forEach { method ->
            val field = method.getPropertyNameForFunction()?.let { name -> fieldNames[name] }
            if (field != null) {
                accessors.getOrPut(field, ::mutableListOf).add(method)
            } else {
                regularMethods.add(method)
            }
        }
        return regularMethods.toList() to accessors.map { (dProperty, dFunctions) ->
            if (dProperty.visibility.values.all { it is KotlinVisibility.Private }) {
                dFunctions.flatMap { it.visibility.values }.toSet().singleOrNull()?.takeIf {
                    it in listOf(KotlinVisibility.Public, KotlinVisibility.Protected)
                }?.let { visibility ->
                    dProperty.copy(
                        getter = dFunctions.firstOrNull { it.type == dProperty.type },
                        setter = dFunctions.firstOrNull { it.parameters.isNotEmpty() },
                        visibility = dProperty.visibility.mapValues { visibility }
                    )
                } ?: dProperty
            } else {
                dProperty
            }
        } + fields.toSet().minus(accessors.keys.toSet())
    }
}