package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name

/**
 * This transformer is used to merge the backing fields and accessors (getters and setters)
 * obtained from Java sources. This way, we could generate more coherent documentation,
 * since the model is now aware of the relationship between accessors and the fields.
 * This way if we generate Kotlin output we get rid of spare getters and setters,
 * and from Kotlin-as-Java perspective we can collect accessors of each property.
 */
class PropertiesMergerTransformer : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>) =
        modules.map { it.copy(packages = it.packages.map {
            it.mergeAccessorsAndField().copy(
                classlikes = it.classlikes.map { it.mergeAccessorsAndField() }
            )
        }) }

    private fun <T : WithScope> T.mergeAccessorsAndField(): T {
        val (functions, properties) = mergePotentialAccessorsAndField(this.functions, this.properties)
        return when (this) {
            is DClass -> {
                this.copy(functions = functions, properties = properties)
            }
            is DEnum -> {
                this.copy(functions = functions, properties = properties)
            }
            is DInterface -> {
                this.copy(functions = functions, properties = properties)
            }
            is DObject -> {
                this.copy(functions = functions, properties = properties)
            }
            is DAnnotation -> {
                this.copy(functions = functions, properties = properties)
            }
            is DPackage -> {
                this.copy(functions = functions, properties = properties)
            }
            else -> this
        } as T
    }

    /**
     * This is copied from here
     * [org.jetbrains.dokka.base.translators.psi.DefaultPsiToDocumentableTranslator.DokkaPsiParser.getPropertyNameForFunction]
     * we should consider if we could unify that.
     * TODO: Revisit that
     */
    private fun DFunction.getPropertyNameForFunction() =
        when {
            JvmAbi.isGetterName(name) -> propertyNameByGetMethodName(Name.identifier(name))?.asString()
            JvmAbi.isSetterName(name) -> propertyNamesBySetMethodName(Name.identifier(name)).firstOrNull()
                ?.asString()
            else -> null
        }

    /**
     * This is loosely copied from here
     * [org.jetbrains.dokka.base.translators.psi.DefaultPsiToDocumentableTranslator.DokkaPsiParser.splitFunctionsAndAccessors]
     * we should consider if we could unify that.
     * TODO: Revisit that
     */
    private fun mergePotentialAccessorsAndField(
        functions: List<DFunction>,
        fields: List<DProperty>
    ): Pair<List<DFunction>, List<DProperty>> {
        val fieldNames = fields.associateBy { it.name }

        // Regular methods are methods that are not getters or setters
        val regularMethods = mutableListOf<DFunction>()
        // Accessors are methods that are getters or setters
        val accessors = mutableMapOf<DProperty, MutableList<DFunction>>()
        functions.forEach { method ->
            val field = method.getPropertyNameForFunction()?.let { name -> fieldNames[name] }
            if (field != null) {
                accessors.getOrPut(field, ::mutableListOf).add(method)
            } else {
                regularMethods.add(method)
            }
        }

        // Properties are triples of field and its getters and/or setters.
        // They are wrapped up in DProperty class,
        // so we copy accessors into its dedicated DProperty data class properties
        val propertiesWithAccessors = accessors.map { (dProperty, dFunctions) ->
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
        }

        // The above logic is driven by accessors list
        // Therefore, if there was no getter or setter, we missed processing the field itself.
        // To include them, we collect all fields that have no accessors
        val remainingFields = fields.toSet().minus(accessors.keys.toSet())

        val allProperties = propertiesWithAccessors + remainingFields

        return regularMethods.toList() to allProperties
    }
}