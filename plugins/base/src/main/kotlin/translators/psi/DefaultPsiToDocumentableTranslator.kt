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
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.translators.isDirectlyAnException
import org.jetbrains.dokka.base.translators.psi.parsers.JavaDocumentationParser
import org.jetbrains.dokka.base.translators.psi.parsers.JavadocParser
import org.jetbrains.dokka.base.translators.unquotedValue
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.nextTarget
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.parallelMapNotNull
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
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
            val documentation = psiFiles.firstOrNull { it.name == "package-info.java" }?.let {
                javadocParser.parseDocumentation(it).toSourceSetDependent()
            } ?: emptyMap()

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
                setOf(sourceSetData)
            )
        }

        private suspend fun parseClasslike(psi: PsiClass, parent: DRI): DClasslike = coroutineScope {
            with(psi) {
                val dri = parent.withClass(name.toString())
                val ancestryTree = mutableListOf<AncestryLevel>()
                val superMethodsKeys = hashSetOf<Int>()
                val superMethods = mutableListOf<Pair<PsiMethod, DRI>>()
                methods.asIterable().parallelForEach { superMethodsKeys.add(it.hash) }
                fun parseSupertypes(superTypes: Array<PsiClassType>, level: Int = 0) { // TODO: Rewrite it
                    if (superTypes.isEmpty()) return
                    val parsedClasses = superTypes.filter { !it.shouldBeIgnored }.mapNotNull { supertypePsi ->
                        supertypePsi.resolve()?.let { supertypePsiClass ->
                            val (supertypeDri, javaClassKind) = when {
                                supertypePsiClass.isInterface -> DRI.from(supertypePsiClass) to JavaClassKindTypes.INTERFACE
                                else -> DRI.from(supertypePsiClass) to JavaClassKindTypes.CLASS
                            }
                            GenericTypeConstructor(
                                supertypeDri,
                                supertypePsi.parameters.map(::getProjection)
                            ) to javaClassKind
                        }
                    }
                    val (classes, interfaces) = parsedClasses.partition { it.second == JavaClassKindTypes.CLASS }
                    ancestryTree.add(AncestryLevel(level, classes.firstOrNull()?.first, interfaces.map { it.first }))

                    superTypes.forEach { type ->
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
                            parseSupertypes(it.superTypes, level + 1)
                        }
                    }
                }
                parseSupertypes(superTypes)
                val (regularFunctions, accessors) = splitFunctionsAndAccessors()
                val overriden = regularFunctions.flatMap { it.findSuperMethods().toList() }
                val documentation = javadocParser.parseDocumentation(this).toSourceSetDependent()
                val allFunctions = async {
                    regularFunctions.parallelMapNotNull {
                        if (!it.isConstructor) parseFunction(
                            it,
                            parentDRI = dri
                        ) else null
                    } + superMethods.filter { it.first !in overriden }.parallelMap { parseFunction(it.first, inheritedFrom = it.second) }
                }
                val source = PsiDocumentableSource(this).toSourceSetDependent()
                val classlikes = async { innerClasses.asIterable().parallelMap { parseClasslike(it, dri) } }
                val visibility = getVisibility().toSourceSetDependent()
                val ancestors = ancestryTree.filter { it.level == 0 }.flatMap {
                    listOfNotNull(it.superclass?.let {
                        TypeConstructorWithKind(
                            typeConstructor = it,
                            kind = JavaClassKindTypes.CLASS
                        )
                    }) + it.interfaces.map {
                        TypeConstructorWithKind(
                            typeConstructor = it,
                            kind = JavaClassKindTypes.INTERFACE
                        )
                    }
                }.toSourceSetDependent()
                val modifiers = getModifier().toSourceSetDependent()
                val implementedInterfacesExtra =
                    ImplementedInterfaces(ancestryTree.flatMap { it.interfaces }.distinct().toSourceSetDependent())
                when {
                    isAnnotationType ->
                        DAnnotation(
                            name.orEmpty(),
                            dri,
                            documentation,
                            null,
                            source,
                            allFunctions.await(),
                            fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                            classlikes.await(),
                            visibility,
                            null,
                            constructors.map { parseFunction(it, true) },
                            mapTypeParameters(dri),
                            setOf(sourceSetData),
                            false,
                            PropertyContainer.withAll(
                                implementedInterfacesExtra,
                                annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                    .toAnnotations()
                            )
                        )
                    isEnum -> DEnum(
                        dri,
                        name.orEmpty(),
                        fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                            DEnumEntry(
                                dri.withClass(entry.name),
                                entry.name,
                                javadocParser.parseDocumentation(entry).toSourceSetDependent(),
                                null,
                                emptyList(),
                                emptyList(),
                                emptyList(),
                                setOf(sourceSetData),
                                PropertyContainer.withAll(
                                    implementedInterfacesExtra,
                                    annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                        .toAnnotations()
                                )
                            )
                        },
                        documentation,
                        null,
                        source,
                        allFunctions.await(),
                        fields.filter { it !is PsiEnumConstant }.map { parseField(it, accessors[it].orEmpty()) },
                        classlikes.await(),
                        visibility,
                        null,
                        constructors.map { parseFunction(it, true) },
                        ancestors,
                        setOf(sourceSetData),
                        false,
                        PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                    isInterface -> DInterface(
                        dri,
                        name.orEmpty(),
                        documentation,
                        null,
                        source,
                        allFunctions.await(),
                        fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                        classlikes.await(),
                        visibility,
                        null,
                        mapTypeParameters(dri),
                        ancestors,
                        setOf(sourceSetData),
                        false,
                        PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                    else -> DClass(
                        dri,
                        name.orEmpty(),
                        constructors.map { parseFunction(it, true) },
                        allFunctions.await(),
                        fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                        classlikes.await(),
                        source,
                        visibility,
                        null,
                        mapTypeParameters(dri),
                        ancestors,
                        documentation,
                        null,
                        modifiers,
                        setOf(sourceSetData),
                        false,
                        PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations(),
                            isExceptionExtra(ancestryTree),
                        )
                    )
                }
            }
        }

        private fun isExceptionExtra(ancestryTree: List<AncestryLevel>): ExceptionInSupertypes? =
            ancestryTree.mapNotNull { it.superclass }.filter { it.dri.isDirectlyAnException() }
                .takeIf { it.isNotEmpty() }?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }

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
            return DFunction(
                dri,
                psi.name,
                isConstructor,
                psi.parameterList.parameters.map { psiParameter ->
                    DParameter(
                        dri.copy(target = dri.target.nextTarget()),
                        psiParameter.name,
                        DocumentationNode(
                            listOfNotNull(docs.firstChildOfTypeOrNull<Param> {
                                it.name == psiParameter.name
                            })
                        ).toSourceSetDependent(),
                        null,
                        getBound(psiParameter.type),
                        setOf(sourceSetData),
                        PropertyContainer.withAll(
                            psiParameter.annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )
                },
                docs.toSourceSetDependent(),
                null,
                PsiDocumentableSource(psi).toSourceSetDependent(),
                psi.getVisibility().toSourceSetDependent(),
                psi.returnType?.let { getBound(type = it) } ?: Void,
                psi.mapTypeParameters(dri),
                null,
                psi.getModifier().toSourceSetDependent(),
                setOf(sourceSetData),
                false,
                psi.additionalExtras().let {
                    PropertyContainer.withAll(
                        InheritedMember(inheritedFrom.toSourceSetDependent()),
                        it.toSourceSetDependent().toAdditionalModifiers(),
                        (psi.annotations.toList()
                            .toListOfAnnotations() + it.toListOfAnnotations()).toSourceSetDependent()
                            .toAnnotations(),
                        ObviousMember.takeIf { inheritedFrom != null && inheritedFrom.isObvious }
                    )
                }
            )
        }

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

        private fun getBound(type: PsiType): Bound {
            fun annotationsFromType(): List<Annotations.Annotation> = type.annotations.toList().toListOfAnnotations()

            fun <T : AnnotationTarget> annotations(): PropertyContainer<T> =
                annotationsFromType().takeIf { it.isNotEmpty() }?.let { annotations ->
                    PropertyContainer.withAll(annotations.toSourceSetDependent().toAnnotations())
                } ?: PropertyContainer.empty()

            fun bound() = when (type) {
                is PsiClassReferenceType ->
                    type.resolve()?.let { resolved ->
                        when {
                            resolved.qualifiedName == "java.lang.Object" -> JavaObject(annotations())
                            resolved is PsiTypeParameter ->
                                TypeParameter(
                                    dri = DRI.from(resolved),
                                    name = resolved.name.orEmpty(),
                                    extra = annotations()
                                )
                            Regex("kotlin\\.jvm\\.functions\\.Function.*").matches(resolved.qualifiedName ?: "") ||
                                    Regex("java\\.util\\.function\\.Function.*").matches(
                                        resolved.qualifiedName ?: ""
                                    ) -> FunctionalTypeConstructor(
                                DRI.from(resolved),
                                type.parameters.map { getProjection(it) },
                                extra = annotations()
                            )
                            else -> GenericTypeConstructor(
                                DRI.from(resolved),
                                type.parameters.map { getProjection(it) },
                                extra = annotations()
                            )
                        }
                    } ?: UnresolvedBound(type.presentableText)
                is PsiArrayType -> GenericTypeConstructor(
                    DRI("kotlin", "Array"),
                    listOf(getProjection(type.componentType)),
                    extra = annotations()
                )
                is PsiPrimitiveType -> if (type.name == "void") Void else PrimitiveJavaType(type.name)
                is PsiImmediateClassType -> JavaObject(annotations())
                else -> throw IllegalStateException("${type.presentableText} is not supported by PSI parser")
            }

            //We would like to cache most of the bounds since it is not common to annotate them,
            //but if this is the case, we treat them as 'one of'
            return if (annotationsFromType().isEmpty()) {
                cachedBounds.getOrPut(type.canonicalText) {
                    bound()
                }
            } else {
                bound()
            }
        }


        private fun getVariance(type: PsiWildcardType): Projection = when {
            type.extendsBound != PsiType.NULL -> Covariance(getBound(type.extendsBound))
            type.superBound != PsiType.NULL -> Contravariance(getBound(type.superBound))
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
            return typeParameters.map { type ->
                DTypeParameter(
                    dri.copy(target = dri.target.nextTarget()),
                    type.name.orEmpty(),
                    null,
                    javadocParser.parseDocumentation(type).toSourceSetDependent(),
                    null,
                    mapBounds(type.bounds),
                    setOf(sourceSetData),
                    PropertyContainer.withAll(
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

        private fun parseField(psi: PsiField, accessors: List<PsiMethod>): DProperty {
            val dri = DRI.from(psi)
            return DProperty(
                dri,
                psi.name,
                javadocParser.parseDocumentation(psi).toSourceSetDependent(),
                null,
                PsiDocumentableSource(psi).toSourceSetDependent(),
                psi.getVisibility().toSourceSetDependent(),
                getBound(psi.type),
                null,
                accessors.firstOrNull { it.hasParameters() }?.let { parseFunction(it) },
                accessors.firstOrNull { it.returnType == psi.type }?.let { parseFunction(it) },
                psi.getModifier().toSourceSetDependent(),
                setOf(sourceSetData),
                emptyList(),
                false,
                psi.additionalExtras().let {
                    PropertyContainer.withAll<DProperty>(
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

    private data class AncestryLevel(
        val level: Int,
        val superclass: TypeConstructor?,
        val interfaces: List<TypeConstructor>
    )
}
