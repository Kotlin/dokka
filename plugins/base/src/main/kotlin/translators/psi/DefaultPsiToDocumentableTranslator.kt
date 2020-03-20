package org.jetbrains.dokka.base.translators.psi

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
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
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils

object DefaultPsiToDocumentableTranslator : PsiToDocumentableTranslator {

    override fun invoke(
        moduleName: String,
        psiFiles: List<PsiJavaFile>,
        platformData: PlatformData,
        context: DokkaContext
    ): DModule {
        val docParser =
            DokkaPsiParser(
                platformData,
                context.logger
            )
        return DModule(
            moduleName,
            psiFiles.groupBy { it.packageName }.map { (packageName, psiFiles) ->
                val dri = DRI(packageName = packageName)
                DPackage(
                    dri,
                    emptyList(),
                    emptyList(),
                    psiFiles.flatMap { psFile ->
                        psFile.classes.map { docParser.parseClasslike(it, dri) }
                    },
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
        private val logger: DokkaLogger
    ) {

        private val javadocParser: JavaDocumentationParser = JavadocParser(logger)

        private val cachedBounds = hashMapOf<String, Bound>()

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

        fun parseClasslike(psi: PsiClass, parent: DRI): DClasslike = with(psi) {
            val dri = parent.withClass(name.toString())
            val ancestorsSet = hashSetOf<DRI>()
            val superMethodsKeys = hashSetOf<Int>()
            val superMethods = mutableListOf<PsiMethod>()
            methods.forEach { superMethodsKeys.add(it.hash) }
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
                        ancestorsSet.add(DRI.from(it))
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
            val modifiers = getModifier().toPlatformDependant()
            return when {
                isAnnotationType ->
                    DAnnotation(
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
                        listOf(platformData),
                        PropertyContainer.empty<DAnnotation>() + annotations.toList().toExtra()
                    )
                isEnum -> DEnum(
                    dri,
                    name.orEmpty(),
                    fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                        DEnumEntry(
                            dri.withClass("$name.${entry.name}"),
                            entry.name.orEmpty(),
                            javadocParser.parseDocumentation(entry).toPlatformDependant(),
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            listOf(platformData),
                            PropertyContainer.empty<DEnumEntry>() + entry.annotations.toList().toExtra()
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
                    listOf(platformData),
                    PropertyContainer.empty<DEnum>() + annotations.toList().toExtra()
                )
                isInterface -> DInterface(
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
                    listOf(platformData),
                    PropertyContainer.empty<DInterface>() + annotations.toList().toExtra()
                )
                else -> DClass(
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
                    modifiers,
                    listOf(platformData),
                    PropertyContainer.empty<DClass>() + annotations.toList().toExtra()
                )
            }
        }

        private fun parseFunction(
            psi: PsiMethod,
            parent: DRI,
            isConstructor: Boolean = false,
            isInherited: Boolean = false
        ): DFunction {
            val dri = DRI.from(psi).copy(classNames = parent.classNames)
            return DFunction(
                dri,
                if (isConstructor) "<init>" else psi.name,
                isConstructor,
                psi.parameterList.parameters.mapIndexed { index, psiParameter ->
                    DParameter(
                        dri.copy(target = index + 1),
                        psiParameter.name,
                        javadocParser.parseDocumentation(psiParameter).toPlatformDependant(),
                        getBound(psiParameter.type),
                        listOf(platformData)
                    )
                },
                javadocParser.parseDocumentation(psi).toPlatformDependant(),
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                psi.returnType?.let { getBound(type = it) } ?: Void,
                psi.mapTypeParameters(dri),
                null,
                psi.getModifier().toPlatformDependant(),
                listOf(platformData),
                PropertyContainer.withAll(
                    InheritedFunction(isInherited),
                    psi.annotations.toList().toExtra(),
                    psi.additionalExtras()
                )
            )
        }

        private fun PsiMethod.additionalExtras() = AdditionalModifiers(
            listOfNotNull(
                ExtraModifiers.STATIC.takeIf { hasModifier(JvmModifier.STATIC) },
                ExtraModifiers.NATIVE.takeIf { hasModifier(JvmModifier.NATIVE) },
                ExtraModifiers.SYNCHRONIZED.takeIf { hasModifier(JvmModifier.SYNCHRONIZED) },
                ExtraModifiers.STRICTFP.takeIf { hasModifier(JvmModifier.STRICTFP) },
                ExtraModifiers.TRANSIENT.takeIf { hasModifier(JvmModifier.TRANSIENT) },
                ExtraModifiers.VOLATILE.takeIf { hasModifier(JvmModifier.VOLATILE) },
                ExtraModifiers.TRANSITIVE.takeIf { hasModifier(JvmModifier.TRANSITIVE) }
            ).toSet()
        )

        private fun getBound(type: PsiType): Bound =
            cachedBounds.getOrPut(type.canonicalText) {
                when (type) {
                    is PsiClassReferenceType -> {
                        val resolved: PsiClass = type.resolve()
                            ?: throw IllegalStateException("${type.presentableText} cannot be resolved")
                        if (resolved.qualifiedName == "java.lang.Object") {
                            JavaObject
                        } else {
                            TypeConstructor(DRI.from(resolved), type.parameters.map { getProjection(it) })
                        }
                    }
                    is PsiArrayType -> TypeConstructor(
                        DRI("kotlin", "Array"),
                        listOf(getProjection(type.componentType))
                    )
                    is PsiPrimitiveType -> if(type.name == "void") Void else PrimitiveJavaType(type.name)
                    else -> throw IllegalStateException("${type.presentableText} is not supported by PSI parser")
                }
            }

        private fun getVariance(type: PsiWildcardType): Projection = when {
            type.extendsBound != PsiType.NULL -> Variance(Variance.Kind.Out, getBound(type.extendsBound))
            type.superBound != PsiType.NULL -> Variance(Variance.Kind.In, getBound(type.superBound))
            else -> throw IllegalStateException("${type.presentableText} has incorrect bounds")
        }

        private fun getProjection(type: PsiType): Projection = when (type) {
            is PsiEllipsisType -> Star
            is PsiWildcardType -> getVariance(type)
            else -> getBound(type)
        }

        private fun PsiModifierListOwner.getModifier() = when {
            hasModifier(JvmModifier.ABSTRACT) -> JavaModifier.Abstract
            hasModifier(JvmModifier.FINAL) -> JavaModifier.Final
            else -> JavaModifier.Empty
        }

        private fun PsiTypeParameterListOwner.mapTypeParameters(dri: DRI): List<DTypeParameter> {
            fun mapBounds(bounds: Array<JvmReferenceType>): List<Bound> =
                if (bounds.isEmpty()) emptyList() else bounds.mapNotNull {
                    (it as? PsiClassType)?.let { classType -> Nullable(getBound(classType)) }
                }
            return typeParameters.mapIndexed { index, type ->
                DTypeParameter(
                    dri.copy(genericTarget = index),
                    type.name.orEmpty(),
                    javadocParser.parseDocumentation(type).toPlatformDependant(),
                    mapBounds(type.bounds),
                    listOf(platformData)
                )
            }
        }

        private fun PsiMethod.getPropertyNameForFunction() =
            getAnnotation(DescriptorUtils.JVM_NAME.asString())?.findAttributeValue("name")?.text
                ?: when {
                    JvmAbi.isGetterName(name) -> propertyNameByGetMethodName(Name.identifier(name))?.asString()
                    JvmAbi.isSetterName(name) -> propertyNamesBySetMethodName(Name.identifier(name)).firstOrNull()
                        ?.asString()
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

        private fun parseField(psi: PsiField, parent: DRI, accessors: List<PsiMethod>): DProperty {
            val dri = DRI.from(psi)
            return DProperty(
                dri,
                psi.name!!, // TODO: Investigate if this is indeed nullable
                javadocParser.parseDocumentation(psi).toPlatformDependant(),
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                getBound(psi.type),
                null,
                accessors.firstOrNull { it.hasParameters() }?.let { parseFunction(it, parent) },
                accessors.firstOrNull { it.returnType == psi.type }?.let { parseFunction(it, parent) },
                psi.getModifier().toPlatformDependant(),
                listOf(platformData),
                PropertyContainer.empty<DProperty>() + psi.annotations.toList().toExtra()
            )
        }

        private fun Collection<PsiAnnotation>.toExtra() = mapNotNull { annotation ->
            val resolved = annotation.getChildOfType<PsiJavaCodeReferenceElement>()?.resolve() ?: run {
                logger.error("$annotation cannot be resolved to symbol!")
                return@mapNotNull null
            }

            Annotations.Annotation(
                DRI.from(resolved),
                annotation.attributes.mapNotNull { attr ->
                    if (attr is PsiNameValuePair) {
                        attr.value?.text?.let { attr.attributeName to it }
                    } else {
                        attr.attributeName to ""
                    }
                }.toMap()
            )
        }.let(::Annotations)
    }
}
