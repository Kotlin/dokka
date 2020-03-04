package org.jetbrains.dokka.base.translators.psi

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.psi.PsiToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

object DefaultPsiToDocumentableTranslator : PsiToDocumentableTranslator {

    override fun invoke(
        moduleName: String,
        psiFiles: List<PsiJavaFile>,
        platformData: PlatformData,
        context: DokkaContext
    ): Module {
        val docParser =
            DokkaPsiParser(
                platformData,
                context.logger
            )
        return Module(
            moduleName,
            psiFiles.map { psiFile ->
                val dri = DRI(packageName = psiFile.packageName)
                Package(
                    dri,
                    emptyList(),
                    emptyList(),
                    psiFile.classes.map { docParser.parseClasslike(it, dri) },
                    emptyList(),
                    PlatformDependent.empty(),
                    listOf(platformData)
                )
            },
            PlatformDependent.empty(),
            listOf(platformData)
        )
    }

    class DokkaPsiParser(
        private val platformData: PlatformData,
        logger: DokkaLogger
    ) {

        private val javadocParser: JavaDocumentationParser = JavadocParser(logger)

        private fun PsiModifierListOwner.getVisibility() = modifierList?.children?.toList()?.let { ml ->
            when {
                ml.any { it.text == PsiKeyword.PUBLIC } -> JavaVisibility.Public
                ml.any { it.text == PsiKeyword.PROTECTED } -> JavaVisibility.Protected
                ml.any { it.text == PsiKeyword.PRIVATE } -> JavaVisibility.Private
                else -> JavaVisibility.Default
            }
        } ?: JavaVisibility.Default

        private val PsiMethod.hash: Int
            get() = "$returnType $name$parameterList".hashCode()

        private val PsiClassType.shouldBeIgnored: Boolean
            get() = isClass("java.lang.Enum") || isClass("java.lang.Object")

        private fun PsiClassType.isClass(qName: String): Boolean {
            val shortName = qName.substringAfterLast('.')
            if (className == shortName) {
                val psiClass = resolve()
                return psiClass?.qualifiedName == qName
            }
            return false
        }

        private fun <T> T.toPlatformDependant() =
            PlatformDependent(mapOf(platformData to this))

        fun parseClasslike(psi: PsiClass, parent: DRI): Classlike = with(psi) {
            val dri = parent.withClass(name.toString())
            val ancestorsSet = hashSetOf<DRI>()
            val superMethodsKeys = hashSetOf<Int>()
            val superMethods = mutableListOf<PsiMethod>()
            methods.forEach { superMethodsKeys.add(it.hash) }
            fun addAncestors(element: PsiClass) {
                ancestorsSet.add(element.toDRI())
                element.interfaces.forEach(::addAncestors)
                element.superClass?.let(::addAncestors)
            }

            fun parseSupertypes(superTypes: Array<PsiClassType>) {
                superTypes.forEach { type ->
                    (type as? PsiClassType)?.takeUnless { type.shouldBeIgnored }?.resolve()?.let {
                        it.methods.forEach { method ->
                            val hash = method.hash
                            if (!method.isConstructor && !superMethodsKeys.contains(hash) &&
                                method.getVisibility() != Visibilities.PRIVATE
                            ) {
                                superMethodsKeys.add(hash)
                                superMethods.add(method)
                            }
                        }
                        addAncestors(it)
                        parseSupertypes(it.superTypes)
                    }
                }
            }
            parseSupertypes(superTypes)
            val (regularFunctions, accessors) = splitFunctionsAndAccessors()
            val documentation = javadocParser.parseDocumentation(this).toPlatformDependant()
            val allFunctions = regularFunctions.mapNotNull { if (!it.isConstructor) parseFunction(it, dri) else null } +
                    superMethods.map { parseFunction(it, dri, isInherited = true) }
            val source = PsiDocumentableSource(this).toPlatformDependant()
            val classlikes = innerClasses.map { parseClasslike(it, dri) }
            val visibility = getVisibility().toPlatformDependant()
            val ancestors = ancestorsSet.toList().toPlatformDependant()
            return when {
                isAnnotationType ->
                    Annotation(
                        name.orEmpty(),
                        dri,
                        documentation,
                        source,
                        allFunctions,
                        fields.mapNotNull { parseField(it, dri, accessors[it].orEmpty()) },
                        classlikes,
                        visibility,
                        null,
                        constructors.map { parseFunction(it, dri, true) },
                        listOf(platformData)
                    )
                isEnum -> Enum(
                    dri,
                    name.orEmpty(),
                    fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                        EnumEntry(
                            dri.withClass("$name.${entry.name}"),
                            entry.name.orEmpty(),
                            javadocParser.parseDocumentation(entry).toPlatformDependant(),
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            listOf(platformData)
                        )
                    },
                    documentation,
                    source,
                    allFunctions,
                    fields.filter { it !is PsiEnumConstant }.map { parseField(it, dri, accessors[it].orEmpty()) },
                    classlikes,
                    visibility,
                    null,
                    constructors.map { parseFunction(it, dri, true) },
                    ancestors,
                    listOf(platformData)
                )
                isInterface -> Interface(
                    dri,
                    name.orEmpty(),
                    documentation,
                    source,
                    allFunctions,
                    fields.mapNotNull { parseField(it, dri, accessors[it].orEmpty()) },
                    classlikes,
                    visibility,
                    null,
                    mapTypeParameters(dri),
                    ancestors,
                    listOf(platformData)
                )
                else -> Class(
                    dri,
                    name.orEmpty(),
                    constructors.map { parseFunction(it, dri, true) },
                    allFunctions,
                    fields.mapNotNull { parseField(it, dri, accessors[it].orEmpty()) },
                    classlikes,
                    source,
                    visibility,
                    null,
                    mapTypeParameters(dri),
                    ancestors,
                    documentation,
                    getModifier(),
                    listOf(platformData)
                )
            }
        }

        private fun parseFunction(
            psi: PsiMethod,
            parent: DRI,
            isConstructor: Boolean = false,
            isInherited: Boolean = false
        ): Function {
            val dri = parent.copy(
                callable = Callable(
                    psi.name,
                    JavaClassReference(psi.containingClass?.name.orEmpty()),
                    psi.parameterList.parameters.map { parameter ->
                        JavaClassReference(parameter.type.canonicalText)
                    })
            )
            return Function(
                dri,
                if (isConstructor) "<init>" else psi.name,
                isConstructor,
                psi.parameterList.parameters.mapIndexed { index, psiParameter ->
                    Parameter(
                        dri.copy(target = index + 1),
                        psiParameter.name,
                        javadocParser.parseDocumentation(psiParameter).toPlatformDependant(),
                        JavaTypeWrapper(psiParameter.type),
                        listOf(platformData)
                    )
                },
                javadocParser.parseDocumentation(psi).toPlatformDependant(),
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                psi.returnType?.let { JavaTypeWrapper(type = it) } ?: JavaTypeWrapper.VOID,
                psi.mapTypeParameters(dri),
                null,
                psi.getModifier(),
                listOf(platformData),
                PropertyContainer.empty<Function>() + InheritedFunction(
                    isInherited
                )

            )
        }

        private fun PsiModifierListOwner.getModifier() = when {
            hasModifier(JvmModifier.ABSTRACT) -> JavaModifier.Abstract
            hasModifier(JvmModifier.FINAL) -> JavaModifier.Final
            else -> JavaModifier.Empty
        }

        private fun PsiTypeParameterListOwner.mapTypeParameters(dri: DRI): List<TypeParameter> {
            fun mapBounds(bounds: Array<JvmReferenceType>): List<Bound> =
                if (bounds.isEmpty()) emptyList() else bounds.mapNotNull {
                    (it as? PsiClassType)?.let { classType ->
                        Nullable(TypeConstructor(classType.resolve()!!.toDRI(), emptyList()))
                    }
                }
            return typeParameters.mapIndexed { index, type ->
                TypeParameter(
                    dri.copy(genericTarget = index),
                    type.name.orEmpty(),
                    javadocParser.parseDocumentation(type).toPlatformDependant(),
                    mapBounds(type.bounds),
                    listOf(platformData)
                )
            }
        }

        private fun PsiQualifiedNamedElement.toDRI() =
            DRI(qualifiedName.orEmpty().substringBeforeLast('.', ""), name)

        private fun PsiMethod.getPropertyNameForFunction() =
            getAnnotation(DescriptorUtils.JVM_NAME.asString())?.findAttributeValue("name")?.text
                ?: when {
                    JvmAbi.isGetterName(name) -> propertyNameByGetMethodName(Name.identifier(name))?.asString()
                    JvmAbi.isSetterName(name) -> propertyNamesBySetMethodName(Name.identifier(name)).firstOrNull()?.asString()
                    else -> null
                }

        private fun PsiClass.splitFunctionsAndAccessors(): Pair<MutableList<PsiMethod>, MutableMap<PsiField, MutableList<PsiMethod>>> {
            val fieldNames = fields.map { it.name to it }.toMap()
            val accessors = mutableMapOf<PsiField, MutableList<PsiMethod>>()
            val regularMethods = mutableListOf<PsiMethod>()
            methods.forEach { method ->
                val field = method.getPropertyNameForFunction()?.let { name -> fieldNames[name] }
                if (field != null) {
                    accessors.getOrPut(field, ::mutableListOf).add(method)
                } else {
                    regularMethods.add(method)
                }
            }
            return regularMethods to accessors
        }

        private fun parseField(psi: PsiField, parent: DRI, accessors: List<PsiMethod>): Property {
            val dri = parent.copy(
                callable = Callable(
                    psi.name!!, // TODO: Investigate if this is indeed nullable
                    JavaClassReference(psi.containingClass?.name.orEmpty()),
                    emptyList()
                )
            )
            return Property(
                dri,
                psi.name!!, // TODO: Investigate if this is indeed nullable
                javadocParser.parseDocumentation(psi).toPlatformDependant(),
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                JavaTypeWrapper(psi.type),
                null,
                accessors.firstOrNull { it.hasParameters() }?.let { parseFunction(it, parent) },
                accessors.firstOrNull { it.returnType == psi.type }?.let { parseFunction(it, parent) },
                psi.getModifier(),
                listOf(platformData)
            )
        }
    }
}
