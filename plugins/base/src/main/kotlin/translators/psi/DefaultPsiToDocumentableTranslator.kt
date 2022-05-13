package org.jetbrains.dokka.base.translators.psi

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiImmediateClassType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.*
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.translators.typeConstructorsBeingExceptions
import org.jetbrains.dokka.base.translators.psi.parsers.JavaDocumentationParser
import org.jetbrains.dokka.base.translators.psi.parsers.JavadocParser
import org.jetbrains.dokka.base.translators.unquotedValue
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.parallelMapNotNull
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

class DefaultPsiToDocumentableTranslator(
    context: DokkaContext
) : AsyncSourceToDocumentableTranslator {

    private val kotlinAnalysis: KotlinAnalysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }

    override suspend fun invokeSuspending(sourceSet: DokkaSourceSet, context: DokkaContext): DModule {
        return coroutineScope {
            fun isFileInSourceRoots(file: File): Boolean =
                sourceSet.sourceRoots.any { root -> file.startsWith(root) }


            val (environment, facade) = kotlinAnalysis[sourceSet]

            val sourceRoots = environment.configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
                ?.filterIsInstance<JavaSourceRoot>()
                ?.mapNotNull { it.file.takeIf(::isFileInSourceRoots) }
                ?: listOf()
            val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")

            val psiFiles = sourceRoots.parallelMap { sourceRoot ->
                sourceRoot.absoluteFile.walkTopDown().mapNotNull {
                    localFileSystem.findFileByPath(it.path)?.let { vFile ->
                        PsiManager.getInstance(environment.project).findFile(vFile) as? PsiJavaFile
                    }
                }.toList()
            }.flatten()

            val docParser =
                DokkaPsiParser(
                    sourceSet,
                    facade,
                    context.logger
                )

            DModule(
                context.configuration.moduleName,
                psiFiles.parallelMapNotNull { it.safeAs<PsiJavaFile>() }.groupBy { it.packageName }.toList()
                    .parallelMap { (packageName: String, psiFiles: List<PsiJavaFile>) ->
                        docParser.parsePackage(packageName, psiFiles)
                    },
                emptyMap(),
                null,
                setOf(sourceSet)
            )
        }
    }

    class DokkaPsiParser(
        private val sourceSetData: DokkaSourceSet,
        facade: DokkaResolutionFacade,
        private val logger: DokkaLogger
    ) {

        private val javadocParser: JavaDocumentationParser = JavadocParser(logger, facade)

        private val cachedBounds = hashMapOf<String, Bound>()

        private fun PsiModifierListOwner.getVisibility() = modifierList?.let {
            val ml = it.children.toList()
            when {
                ml.any { it.text == PsiKeyword.PUBLIC } || it.hasModifierProperty("public") -> JavaVisibility.Public
                ml.any { it.text == PsiKeyword.PROTECTED } || it.hasModifierProperty("protected") -> JavaVisibility.Protected
                ml.any { it.text == PsiKeyword.PRIVATE } || it.hasModifierProperty("private") -> JavaVisibility.Private
                else -> JavaVisibility.Default
            }
        } ?: JavaVisibility.Default

        private val PsiMethod.hash: Int
            get() = "$returnType $name$parameterList".hashCode()

        private val PsiClassType.shouldBeIgnored: Boolean
            get() = isClass("java.lang.Enum") || isClass("java.lang.Object")

        private val DRI.isObvious: Boolean
            get() = packageName == "java.lang" && (classNames == "Object" || classNames == "Enum")

        private fun PsiClassType.isClass(qName: String): Boolean {
            val shortName = qName.substringAfterLast('.')
            if (className == shortName) {
                val psiClass = resolve()
                return psiClass?.qualifiedName == qName
            }
            return false
        }

        private fun <T> T.toSourceSetDependent() = mapOf(sourceSetData to this)

        suspend fun parsePackage(packageName: String, psiFiles: List<PsiJavaFile>): DPackage = coroutineScope {
            val dri = DRI(packageName = packageName)
            val packageInfo = psiFiles.singleOrNull { it.name == "package-info.java" }
            val documentation = packageInfo?.let {
                javadocParser.parseDocumentation(it).toSourceSetDependent()
            }.orEmpty()
            val annotations = packageInfo?.packageStatement?.annotationList?.annotations

            DPackage(
                dri,
                emptyList(),
                emptyList(),
                psiFiles.parallelMap { psiFile ->
                    coroutineScope {
                        psiFile.classes.asIterable().parallelMap { parseClasslike(it, dri) }
                    }
                }.flatten(),
                emptyList(),
                documentation,
                null,
                setOf(sourceSetData),
                PropertyContainer.withAll(
                    annotations?.toList().orEmpty().toListOfAnnotations().toSourceSetDependent().toAnnotations()
                )
            )
        }

        private suspend fun parseClasslike(psi: PsiClass, parent: DRI): DClasslike = coroutineScope {
            with(psi) {
                val dri = parent.withClass(name.toString())
                val sources = PsiDocumentableSource(this).toSourceSetDependent()
                val superMethodsKeys = hashSetOf<Int>()
                val superMethods = mutableListOf<Pair<PsiMethod, DRI>>()
                methods.asIterable().parallelForEach { superMethodsKeys.add(it.hash) }

                /**
                 * Caution! This method mutates superMethodsKeys and superMethods
                 */
                fun Array<PsiClassType>.getSuperTypesPsiClasses(): List<Pair<PsiClass, JavaClassKindTypes>> {
                    forEach { type ->
                        (type as? PsiClassType)?.resolve()?.let {
                            val definedAt = DRI.from(it)
                            it.methods.forEach { method ->
                                val hash = method.hash
                                if (!method.isConstructor && !superMethodsKeys.contains(hash) &&
                                    method.getVisibility() != JavaVisibility.Private
                                ) {
                                    superMethodsKeys.add(hash)
                                    superMethods.add(Pair(method, definedAt))
                                }
                            }
                        }
                    }
                    return filter { !it.shouldBeIgnored }.mapNotNull { supertypePsi ->
                        supertypePsi.resolve()?.let { supertypePsiClass ->
                            val javaClassKind = when {
                                supertypePsiClass.isInterface -> JavaClassKindTypes.INTERFACE
                                else -> JavaClassKindTypes.CLASS
                            }
                            supertypePsiClass to javaClassKind
                        }
                    }
                }

                fun traversePsiClassForAncestorsAndInheritedMembers(psiClass: PsiClass): AncestryNode {
                    val (classes, interfaces) = psiClass.superTypes.getSuperTypesPsiClasses()
                        .partition { it.second == JavaClassKindTypes.CLASS }

                    return AncestryNode(
                        typeConstructor = GenericTypeConstructor(
                            dri = DRI.from(psiClass),
                            projections = psiClass.typeParameters.map { typeParameter ->
                                TypeParameter(
                                    dri = DRI.from(typeParameter),
                                    name = typeParameter.name.orEmpty(),
                                    sources = sources,
                                    extra = PropertyContainer.withAll(
                                        typeParameter.annotations(),
                                    )
                                )
                            },
                            sources = sources
                        ),
                        superclass = classes.singleOrNull()?.first?.let(::traversePsiClassForAncestorsAndInheritedMembers),
                        interfaces = interfaces.map { traversePsiClassForAncestorsAndInheritedMembers(it.first) }
                    )
                }

                val ancestry: AncestryNode = traversePsiClassForAncestorsAndInheritedMembers(this)
                val (regularFunctions, accessors) = splitFunctionsAndAccessors()
                val overridden = regularFunctions.flatMap { it.findSuperMethods().toList() }
                val documentation = javadocParser.parseDocumentation(this).toSourceSetDependent()
                val allFunctions = async {
                    regularFunctions.parallelMapNotNull {
                        if (!it.isConstructor) parseFunction(
                            it,
                            parentDRI = dri
                        ) else null
                    } + superMethods.filter { it.first !in overridden }.parallelMap { parseFunction(it.first, inheritedFrom = it.second) }
                }
                val classlikes = async { innerClasses.asIterable().parallelMap { parseClasslike(it, dri) } }
                val visibility = getVisibility().toSourceSetDependent()
                val ancestors = (listOfNotNull(ancestry.superclass?.let {
                    it.typeConstructor.let {
                        TypeConstructorWithKind(
                            it,
                            JavaClassKindTypes.CLASS
                        )
                    }
                }) + ancestry.interfaces.map { TypeConstructorWithKind(it.typeConstructor, JavaClassKindTypes.INTERFACE) }).toSourceSetDependent()
                val modifiers = getModifier().toSourceSetDependent()
                val implementedInterfacesExtra =
                    ImplementedInterfaces(ancestry.allImplementedInterfaces().toSourceSetDependent())
                when {
                    isAnnotationType ->
                        DAnnotation(
                            name = name.orEmpty(),
                            dri = dri,
                            documentation = documentation,
                            expectPresentInSet = null,
                            sources = sources,
                            functions = allFunctions.await(),
                            properties = fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                            classlikes = classlikes.await(),
                            visibility = visibility,
                            companion = null,
                            constructors = constructors.map { parseFunction(it, true) },
                            generics = mapTypeParameters(dri, sources),
                            sourceSets = setOf(sourceSetData),
                            isExpectActual = false,
                            extra = PropertyContainer.withAll(
                                implementedInterfacesExtra,
                                annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                    .toAnnotations()
                            )
                        )
                    isEnum -> DEnum(
                        dri = dri,
                        name = name.orEmpty(),
                        entries = fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                            DEnumEntry(
                                dri = dri.withClass(entry.name).withEnumEntryExtra(),
                                name = entry.name,
                                documentation = javadocParser.parseDocumentation(entry).toSourceSetDependent(),
                                expectPresentInSet = null,
                                functions = emptyList(),
                                properties = emptyList(),
                                classlikes = emptyList(),
                                sourceSets = setOf(sourceSetData),
                                sources = sources,
                                extra = PropertyContainer.withAll(
                                    implementedInterfacesExtra,
                                    annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                        .toAnnotations()
                                )
                            )
                        },
                        documentation = documentation,
                        expectPresentInSet = null,
                        sources = sources,
                        functions = allFunctions.await(),
                        properties = fields.filter { it !is PsiEnumConstant }.map { parseField(it, accessors[it].orEmpty()) },
                        classlikes = classlikes.await(),
                        visibility = visibility,
                        companion = null,
                        constructors = constructors.map { parseFunction(it, true) },
                        supertypes = ancestors,
                        sourceSets = setOf(sourceSetData),
                        isExpectActual = false,
                        extra = PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                    isInterface -> DInterface(
                        dri = dri,
                        name = name.orEmpty(),
                        documentation = documentation,
                        expectPresentInSet = null,
                        sources = sources,
                        functions = allFunctions.await(),
                        properties = fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                        classlikes = classlikes.await(),
                        visibility = visibility,
                        companion = null,
                        generics = mapTypeParameters(dri, sources),
                        supertypes = ancestors,
                        sourceSets = setOf(sourceSetData),
                        isExpectActual = false,
                        extra = PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                    else -> DClass(
                        dri = dri,
                        name = name.orEmpty(),
                        constructors = constructors.map { parseFunction(it, true) },
                        functions = allFunctions.await(),
                        properties = fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                        classlikes = classlikes.await(),
                        sources = sources,
                        visibility = visibility,
                        companion = null,
                        generics = mapTypeParameters(dri, sources),
                        supertypes = ancestors,
                        documentation = documentation,
                        expectPresentInSet = null,
                        modifier = modifiers,
                        sourceSets = setOf(sourceSetData),
                        isExpectActual = false,
                        extra = PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations(),
                            ancestry.exceptionInSupertypesOrNull()
                        )
                    )
                }
            }
        }

        private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
            typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }

        private fun parseFunction(
            psi: PsiMethod,
            isConstructor: Boolean = false,
            inheritedFrom: DRI? = null,
            parentDRI: DRI? = null
        ): DFunction {
            val dri = parentDRI?.let { dri ->
                DRI.from(psi).copy(packageName = dri.packageName, classNames = dri.classNames)
            } ?: DRI.from(psi)
            val docs = javadocParser.parseDocumentation(psi)
            val sources = PsiDocumentableSource(psi).toSourceSetDependent()
            return DFunction(
                dri = dri,
                name = psi.name,
                isConstructor = isConstructor,
                parameters = psi.parameterList.parameters.map { psiParameter ->
                    DParameter(
                        dri = dri.copy(target = dri.target.nextTarget()),
                        name = psiParameter.name,
                        documentation = DocumentationNode(
                            listOfNotNull(docs.firstChildOfTypeOrNull<Param> {
                                it.name == psiParameter.name
                            })
                        ).toSourceSetDependent(),
                        expectPresentInSet = null,
                        type = getBound(psiParameter.type, sources),
                        sources = sources,
                        sourceSets = setOf(sourceSetData),
                        extra = PropertyContainer.withAll(
                            psiParameter.annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                },
                documentation = docs.toSourceSetDependent(),
                expectPresentInSet = null,
                sources = PsiDocumentableSource(psi).toSourceSetDependent(),
                visibility = psi.getVisibility().toSourceSetDependent(),
                type = psi.returnType?.let { getBound(type = it, sources) } ?: Void(sources),
                generics = psi.mapTypeParameters(dri, sources),
                receiver = null,
                modifier = psi.getModifier().toSourceSetDependent(),
                sourceSets = setOf(sourceSetData),
                isExpectActual = false,
                extra = psi.additionalExtras().let {
                    PropertyContainer.withAll(
                        InheritedMember(inheritedFrom.toSourceSetDependent()),
                        it.toSourceSetDependent().toAdditionalModifiers(),
                        (psi.annotations.toList()
                            .toListOfAnnotations() + it.toListOfAnnotations()).toSourceSetDependent()
                            .toAnnotations(),
                        ObviousMember.takeIf { inheritedFrom != null && inheritedFrom.isObvious },
                        psi.throwsList.toDriList().takeIf { it.isNotEmpty() }
                            ?.let { CheckedExceptions(it.toSourceSetDependent()) }
                    )
                }
            )
        }

        private fun PsiReferenceList.toDriList() = referenceElements.mapNotNull { it?.resolve()?.let { DRI.from(it) } }

        private fun PsiModifierListOwner.additionalExtras() = listOfNotNull(
            ExtraModifiers.JavaOnlyModifiers.Static.takeIf { hasModifier(JvmModifier.STATIC) },
            ExtraModifiers.JavaOnlyModifiers.Native.takeIf { hasModifier(JvmModifier.NATIVE) },
            ExtraModifiers.JavaOnlyModifiers.Synchronized.takeIf { hasModifier(JvmModifier.SYNCHRONIZED) },
            ExtraModifiers.JavaOnlyModifiers.StrictFP.takeIf { hasModifier(JvmModifier.STRICTFP) },
            ExtraModifiers.JavaOnlyModifiers.Transient.takeIf { hasModifier(JvmModifier.TRANSIENT) },
            ExtraModifiers.JavaOnlyModifiers.Volatile.takeIf { hasModifier(JvmModifier.VOLATILE) },
            ExtraModifiers.JavaOnlyModifiers.Transitive.takeIf { hasModifier(JvmModifier.TRANSITIVE) }
        ).toSet()

        private fun Set<ExtraModifiers>.toListOfAnnotations() = map {
            if (it !is ExtraModifiers.JavaOnlyModifiers.Static)
                Annotations.Annotation(DRI("kotlin.jvm", it.name.toLowerCase().capitalize()), emptyMap())
            else
                Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), emptyMap())
        }

        private fun PsiTypeParameter.annotations() =
                this.annotations.toList().toListOfAnnotations().toSourceSetDependent().toAnnotations()
        private fun PsiType.annotations() =
                this.annotations.toList().toListOfAnnotations().toSourceSetDependent().toAnnotations()

        private fun getBound(type: PsiType, sources: SourceSetDependent<DocumentableSource>): Bound {
            fun bound(sources: SourceSetDependent<DocumentableSource>): Bound = when (type) {
                is PsiClassReferenceType ->
                    type.resolve()?.let { resolved ->
                        when {
                            resolved.qualifiedName == "java.lang.Object" -> JavaObject(
                                sources = sources,
                                extra = PropertyContainer.withAll(type.annotations())
                            )
                            resolved is PsiTypeParameter -> {
                                TypeParameter(
                                    dri = DRI.from(resolved),
                                    name = resolved.name.orEmpty(),
                                    sources = sources,
                                    extra = PropertyContainer.withAll(type.annotations())
                                )
                            }
                            Regex("kotlin\\.jvm\\.functions\\.Function.*").matches(resolved.qualifiedName ?: "") ||
                                    Regex("java\\.util\\.function\\.Function.*").matches(
                                        resolved.qualifiedName ?: ""
                                    ) -> FunctionalTypeConstructor(
                                dri = DRI.from(resolved),
                                projections = type.parameters.map { getProjection(it, sources) },
                                sources = sources,
                                extra = PropertyContainer.withAll(type.annotations())
                            )
                            else -> GenericTypeConstructor(
                                dri = DRI.from(resolved),
                                projections = type.parameters.map { getProjection(it, sources) },
                                sources = sources,
                                extra = PropertyContainer.withAll(type.annotations())
                            )
                        }
                    } ?: UnresolvedBound(
                        name = type.presentableText,
                        sources = sources,
                        extra = PropertyContainer.withAll(type.annotations())
                    )
                is PsiArrayType -> GenericTypeConstructor(
                    dri = DRI("kotlin", "Array"),
                    projections = listOf(getProjection(type.componentType, sources)),
                    sources = sources,
                    extra = PropertyContainer.withAll(type.annotations())
                )
                is PsiPrimitiveType -> if (type.name == "void") Void(sources)
                    else PrimitiveJavaType(
                    name = type.name,
                    sources = sources,
                    extra = PropertyContainer.withAll(type.annotations())
                )
                is PsiImmediateClassType -> JavaObject(sources, PropertyContainer.withAll(type.annotations()))
                else -> throw IllegalStateException("${type.presentableText} is not supported by PSI parser")
            }

            //We would like to cache most of the bounds since it is not common to annotate them,
            //but if this is the case, we treat them as 'one of'
            return if (type.annotations.toList().toListOfAnnotations().isEmpty()) {
                cachedBounds.getOrPut(type.canonicalText) {
                    bound(sources)
                }
            } else {
                bound(sources)
            }
        }


        private fun getVariance(type: PsiWildcardType, sources: SourceSetDependent<DocumentableSource>) = when {
            type.extendsBound != PsiType.NULL -> Covariance(getBound(type.extendsBound, sources))
            type.superBound != PsiType.NULL -> Contravariance(getBound(type.superBound, sources))
            else -> throw IllegalStateException("${type.presentableText} has incorrect bounds")
        }

        private fun getProjection(type: PsiType, sources: SourceSetDependent<DocumentableSource>): Projection = when (type) {
            is PsiEllipsisType -> Star
            is PsiWildcardType -> getVariance(type, sources)
            else -> getBound(type, sources)
        }

        private fun PsiModifierListOwner.getModifier() = when {
            hasModifier(JvmModifier.ABSTRACT) -> JavaModifier.Abstract
            hasModifier(JvmModifier.FINAL) -> JavaModifier.Final
            else -> JavaModifier.Empty
        }

        private fun PsiTypeParameterListOwner.mapTypeParameters(
            dri: DRI,
            sources: SourceSetDependent<DocumentableSource>
        ): List<DTypeParameter> {
            fun mapBounds(bounds: Array<JvmReferenceType>): List<Bound> =
                if (bounds.isEmpty()) emptyList() else bounds.mapNotNull {
                    (it as? PsiClassType)?.let { classType -> Nullable(getBound(classType, sources)) }
                }
            return typeParameters.map { type ->
                DTypeParameter(
                    dri = dri.copy(target = dri.target.nextTarget()),
                    name = type.name.orEmpty(),
                    presentableName = null,
                    documentation = javadocParser.parseDocumentation(type).toSourceSetDependent(),
                    expectPresentInSet = null,
                    bounds = mapBounds(type.bounds),
                    sources = sources,
                    sourceSets = setOf(sourceSetData),
                    extra = PropertyContainer.withAll(
                        type.annotations.toList().toListOfAnnotations().toSourceSetDependent()
                            .toAnnotations()
                    )
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
            val fieldNames = fields.associateBy { it.name }
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

        private fun parseField(psi: PsiField, accessors: List<PsiMethod>): DProperty {
            val dri = DRI.from(psi)
            val sources = PsiDocumentableSource(psi).toSourceSetDependent()
            return DProperty(
                dri = dri,
                name = psi.name,
                documentation = javadocParser.parseDocumentation(psi).toSourceSetDependent(),
                expectPresentInSet = null,
                sources = sources,
                visibility = psi.getVisibility().toSourceSetDependent(),
                type = getBound(psi.type, sources),
                receiver = null,
                setter = accessors.firstOrNull { it.hasParameters() }?.let { parseFunction(it) },
                getter = accessors.firstOrNull { it.returnType == psi.type }?.let { parseFunction(it) },
                modifier = psi.getModifier().toSourceSetDependent(),
                sourceSets = setOf(sourceSetData),
                generics = emptyList(),
                isExpectActual = false,
                extra = psi.additionalExtras().let {
                    PropertyContainer.withAll(
                        it.toSourceSetDependent().toAdditionalModifiers(),
                        (psi.annotations.toList()
                            .toListOfAnnotations() + it.toListOfAnnotations()).toSourceSetDependent()
                            .toAnnotations()
                    )
                }
            )
        }

        private fun Collection<PsiAnnotation>.toListOfAnnotations() =
            filter { it !is KtLightAbstractAnnotation }.mapNotNull { it.toAnnotation() }

        private fun JvmAnnotationAttribute.toValue(): AnnotationParameterValue = when (this) {
            is PsiNameValuePair -> value?.toValue() ?: attributeValue?.toValue() ?: StringValue("")
            else -> StringValue(this.attributeName)
        }.let { annotationValue ->
            if (annotationValue is StringValue) annotationValue.copy(unquotedValue(annotationValue.value))
            else annotationValue
        }

        /**
         * This is a workaround for static imports from JDK like RetentionPolicy
         * For some reason they are not represented in the same way than using normal import
         */
        private fun JvmAnnotationAttributeValue.toValue(): AnnotationParameterValue? = when (this) {
            is JvmAnnotationEnumFieldValue -> (field as? PsiElement)?.let { EnumValue(fieldName ?: "", DRI.from(it)) }
            else -> null
        }

        private fun PsiAnnotationMemberValue.toValue(): AnnotationParameterValue? = when (this) {
            is PsiAnnotation -> toAnnotation()?.let { AnnotationValue(it) }
            is PsiArrayInitializerMemberValue -> ArrayValue(initializers.mapNotNull { it.toValue() })
            is PsiReferenceExpression -> psiReference?.let { EnumValue(text ?: "", DRI.from(it)) }
            is PsiClassObjectAccessExpression -> {
                val psiClass = ((type as PsiImmediateClassType).parameters.single() as PsiClassReferenceType).resolve()
                psiClass?.let { ClassValue(text ?: "", DRI.from(psiClass)) }
            }
            is PsiLiteralExpression -> toValue()
            else -> StringValue(text ?: "")
        }

        private fun PsiLiteralExpression.toValue(): AnnotationParameterValue? = when (type) {
            PsiType.INT -> (value as? Int)?.let { IntValue(it) }
            PsiType.LONG -> (value as? Long)?.let { LongValue(it) }
            PsiType.FLOAT -> (value as? Float)?.let { FloatValue(it) }
            PsiType.DOUBLE -> (value as? Double)?.let { DoubleValue(it) }
            PsiType.BOOLEAN -> (value as? Boolean)?.let { BooleanValue(it) }
            PsiType.NULL -> NullValue
            else -> StringValue(text ?: "")
        }

        private fun PsiAnnotation.toAnnotation(): Annotations.Annotation? {
            // TODO Mitigating workaround for issue https://github.com/Kotlin/dokka/issues/1341
            //  Tracking https://youtrack.jetbrains.com/issue/KT-41234
            //  Needs to be removed once this issue is fixed in light classes
            fun PsiElement.getAnnotationsOrNull(): Array<PsiAnnotation>? {
                this as PsiClass
                return try {
                    this.annotations
                } catch (e: KotlinExceptionWithAttachments) {
                    logger.warn("Failed to get annotations from ${this.getKotlinFqName()}")
                    null
                }
            }

            return psiReference?.let { psiElement ->
                Annotations.Annotation(
                    dri = DRI.from(psiElement),
                    params = attributes
                        .filter { it !is KtLightAbstractAnnotation }
                        .mapNotNull { it.attributeName to it.toValue() }
                        .toMap(),
                    mustBeDocumented = psiElement.getAnnotationsOrNull().orEmpty().any { annotation ->
                        annotation.hasQualifiedName("java.lang.annotation.Documented")
                    }
                )
            }
        }

        private val PsiElement.psiReference
            get() = getChildOfType<PsiJavaCodeReferenceElement>()?.resolve()
    }
}
