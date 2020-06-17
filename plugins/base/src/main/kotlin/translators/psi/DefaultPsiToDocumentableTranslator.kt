package org.jetbrains.dokka.base.translators.psi

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiImmediateClassType
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.nextTarget
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

object DefaultPsiToDocumentableTranslator : SourceToDocumentableTranslator {

    override fun invoke(sourceSetData: SourceSetData, context: DokkaContext): DModule {

        fun isFileInSourceRoots(file: File) : Boolean {
            return sourceSetData.sourceRoots.any { root -> file.path.startsWith(File(root.path).absolutePath) }
        }

        val (environment, _) = context.platforms.getValue(sourceSetData)

        val sourceRoots = environment.configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.mapNotNull { it.file.takeIf(::isFileInSourceRoots) }
            ?: listOf()
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")

        val psiFiles = sourceRoots.map { sourceRoot ->
            sourceRoot.absoluteFile.walkTopDown().mapNotNull {
                localFileSystem.findFileByPath(it.path)?.let { vFile ->
                    PsiManager.getInstance(environment.project).findFile(vFile) as? PsiJavaFile
                }
            }.toList()
        }.flatten()

        val docParser =
            DokkaPsiParser(
                sourceSetData,
                context.logger
            )
        return DModule(
            sourceSetData.moduleName,
            psiFiles.mapNotNull { it.safeAs<PsiJavaFile>() }.groupBy { it.packageName }.map { (packageName, psiFiles) ->
                val dri = DRI(packageName = packageName)
                DPackage(
                    dri,
                    emptyList(),
                    emptyList(),
                    psiFiles.flatMap { psiFile ->
                        psiFile.classes.map { docParser.parseClasslike(it, dri) }
                    },
                    emptyList(),
                    emptyMap(),
                    null,
                    listOf(sourceSetData)
                )
            },
            emptyMap(),
            null,
            listOf(sourceSetData)
        )
    }

    class DokkaPsiParser(
        private val sourceSetData: SourceSetData,
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
            mapOf(sourceSetData to this)

        fun parseClasslike(psi: PsiClass, parent: DRI): DClasslike = with(psi) {
            val dri = parent.withClass(name.toString())
            val ancestorsSet = hashSetOf<Ancestor>()
            val superMethodsKeys = hashSetOf<Int>()
            val superMethods = mutableListOf<Pair<PsiMethod, DRI>>()
            methods.forEach { superMethodsKeys.add(it.hash) }
            fun parseSupertypes(superTypes: Array<PsiClassType>) {
                superTypes.forEach { type ->
                    (type as? PsiClassType)?.takeUnless { type.shouldBeIgnored }?.resolve()?.let {
                        val definedAt = DRI.from(it)
                        it.methods.forEach { method ->
                            val hash = method.hash
                            if (!method.isConstructor && !superMethodsKeys.contains(hash) &&
                                method.getVisibility() != Visibilities.PRIVATE
                            ) {
                                superMethodsKeys.add(hash)
                                superMethods.add(Pair(method, definedAt))
                            }
                        }
                        ancestorsSet.add(Ancestor(DRI.from(it), it.isInterface))
                        parseSupertypes(it.superTypes)
                    }
                }
            }
            parseSupertypes(superTypes)
            val (regularFunctions, accessors) = splitFunctionsAndAccessors()
            val documentation = javadocParser.parseDocumentation(this).toPlatformDependant()
            val allFunctions = regularFunctions.mapNotNull { if (!it.isConstructor) parseFunction(it) else null } +
                    superMethods.map { parseFunction(it.first, inheritedFrom = it.second) }
            val source = PsiDocumentableSource(this).toPlatformDependant()
            val classlikes = innerClasses.map { parseClasslike(it, dri) }
            val visibility = getVisibility().toPlatformDependant()
            val ancestors = ancestorsSet.toList().map { it.dri }.toPlatformDependant()
            val modifiers = getModifier().toPlatformDependant()
            val implementedInterfacesExtra = ImplementedInterfaces(ancestorsSet.filter { it.isInterface }.map { it.dri }.toList())
            return when {
                isAnnotationType ->
                    DAnnotation(
                        name.orEmpty(),
                        dri,
                        documentation,
                        null,
                        source,
                        allFunctions,
                        fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                        classlikes,
                        visibility,
                        null,
                        constructors.map { parseFunction(it, true) },
                        mapTypeParameters(dri),
                        listOf(sourceSetData),
                        PropertyContainer.withAll(implementedInterfacesExtra, annotations.toList().toListOfAnnotations().let(::Annotations))
                    )
                isEnum -> DEnum(
                    dri,
                    name.orEmpty(),
                    fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                        DEnumEntry(
                            dri.withClass("$name.${entry.name}"),
                            entry.name.orEmpty(),
                            javadocParser.parseDocumentation(entry).toPlatformDependant(),
                            null,
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            listOf(sourceSetData),
                            PropertyContainer.withAll(implementedInterfacesExtra, annotations.toList().toListOfAnnotations().let(::Annotations))
                        )
                    },
                    documentation,
                    null,
                    source,
                    allFunctions,
                    fields.filter { it !is PsiEnumConstant }.map { parseField(it, accessors[it].orEmpty()) },
                    classlikes,
                    visibility,
                    null,
                    constructors.map { parseFunction(it, true) },
                    ancestors,
                    listOf(sourceSetData),
                    PropertyContainer.withAll(implementedInterfacesExtra, annotations.toList().toListOfAnnotations().let(::Annotations))
                )
                isInterface -> DInterface(
                    dri,
                    name.orEmpty(),
                    documentation,
                    null,
                    source,
                    allFunctions,
                    fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                    classlikes,
                    visibility,
                    null,
                    mapTypeParameters(dri),
                    ancestors,
                    listOf(sourceSetData),
                    PropertyContainer.withAll(implementedInterfacesExtra, annotations.toList().toListOfAnnotations().let(::Annotations))
                )
                else -> DClass(
                    dri,
                    name.orEmpty(),
                    constructors.map { parseFunction(it, true) },
                    allFunctions,
                    fields.mapNotNull { parseField(it, accessors[it].orEmpty()) },
                    classlikes,
                    source,
                    visibility,
                    null,
                    mapTypeParameters(dri),
                    ancestors,
                    documentation,
                    null,
                    modifiers,
                    listOf(sourceSetData),
                    PropertyContainer.withAll(implementedInterfacesExtra, annotations.toList().toListOfAnnotations().let(::Annotations))
                )
            }
        }

        private fun parseFunction(
            psi: PsiMethod,
            isConstructor: Boolean = false,
            inheritedFrom: DRI? = null
        ): DFunction {
            val dri = DRI.from(psi)
            val docs = javadocParser.parseDocumentation(psi).toPlatformDependant()
            return DFunction(
                dri,
                if (isConstructor) "<init>" else psi.name,
                isConstructor,
                psi.parameterList.parameters.map { psiParameter ->
                    DParameter(
                        dri.copy(target = dri.target.nextTarget()),
                        psiParameter.name,
                        DocumentationNode(docs.entries.mapNotNull { it.value.children.filterIsInstance<Param>().firstOrNull { it.root.children.firstIsInstanceOrNull<DocumentationLink>()?.children?.firstIsInstanceOrNull<Text>()?.body == psiParameter.name } }).toPlatformDependant(),
                        null,
                        getBound(psiParameter.type),
                        listOf(sourceSetData)
                    )
                },
                docs,
                null,
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                psi.returnType?.let { getBound(type = it) } ?: Void,
                psi.mapTypeParameters(dri),
                null,
                psi.getModifier().toPlatformDependant(),
                listOf(sourceSetData),
                psi.additionalExtras().let {
                    PropertyContainer.withAll(
                        InheritedFunction(inheritedFrom),
                        it,
                        (psi.annotations.toList().toListOfAnnotations() + it.toListOfAnnotations()).let(::Annotations)
                    )
                }
            )
        }

        private fun PsiModifierListOwner.additionalExtras() = AdditionalModifiers(
            listOfNotNull(
                ExtraModifiers.JavaOnlyModifiers.Static.takeIf { hasModifier(JvmModifier.STATIC) },
                ExtraModifiers.JavaOnlyModifiers.Native.takeIf { hasModifier(JvmModifier.NATIVE) },
                ExtraModifiers.JavaOnlyModifiers.Synchronized.takeIf { hasModifier(JvmModifier.SYNCHRONIZED) },
                ExtraModifiers.JavaOnlyModifiers.StrictFP.takeIf { hasModifier(JvmModifier.STRICTFP) },
                ExtraModifiers.JavaOnlyModifiers.Transient.takeIf { hasModifier(JvmModifier.TRANSIENT) },
                ExtraModifiers.JavaOnlyModifiers.Volatile.takeIf { hasModifier(JvmModifier.VOLATILE) },
                ExtraModifiers.JavaOnlyModifiers.Transitive.takeIf { hasModifier(JvmModifier.TRANSITIVE) }
            ).toSet()
        )

        private fun AdditionalModifiers.toListOfAnnotations() = this.content.map {
            if (it !is ExtraModifiers.JavaOnlyModifiers.Static)
                Annotations.Annotation(DRI("kotlin.jvm", it.name.toLowerCase().capitalize()), emptyMap())
            else
                Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), emptyMap())
        }

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
                    is PsiPrimitiveType -> if (type.name == "void") Void else PrimitiveJavaType(type.name)
                    is PsiImmediateClassType -> JavaObject
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
            return typeParameters.map { type ->
                DTypeParameter(
                    dri.copy(target = dri.target.nextTarget()),
                    type.name.orEmpty(),
                    javadocParser.parseDocumentation(type).toPlatformDependant(),
                    null,
                    mapBounds(type.bounds),
                    listOf(sourceSetData)
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
                javadocParser.parseDocumentation(psi).toPlatformDependant(),
                null,
                PsiDocumentableSource(psi).toPlatformDependant(),
                psi.getVisibility().toPlatformDependant(),
                getBound(psi.type),
                null,
                accessors.firstOrNull { it.hasParameters() }?.let { parseFunction(it) },
                accessors.firstOrNull { it.returnType == psi.type }?.let { parseFunction(it) },
                psi.getModifier().toPlatformDependant(),
                listOf(sourceSetData),
                emptyList(),
                psi.additionalExtras().let {
                    PropertyContainer.withAll<DProperty>(
                        it,
                        (psi.annotations.toList().toListOfAnnotations() + it.toListOfAnnotations()).let(::Annotations)
                    )
                }
            )
        }

        private fun Collection<PsiAnnotation>.toListOfAnnotations() = filter { it !is KtLightAbstractAnnotation }.mapNotNull { it.toAnnotation() }

        private fun JvmAnnotationAttribute.toValue(): AnnotationParameterValue = when (this) {
            is PsiNameValuePair -> value?.toValue() ?: StringValue("")
            else -> StringValue(this.attributeName)
        }

        private fun PsiAnnotationMemberValue.toValue(): AnnotationParameterValue = when(this) {
            is PsiAnnotation -> AnnotationValue(toAnnotation())
            is PsiArrayInitializerMemberValue -> ArrayValue(initializers.map { it.toValue() })
            is PsiReferenceExpression -> EnumValue(
                text ?: "",
                driOfReference()
            )
            is PsiClassObjectAccessExpression -> ClassValue(
                text ?: "",
                DRI.from(((type as PsiImmediateClassType).parameters.single() as PsiClassReferenceType).resolve()!!)
            )
            else -> StringValue(text ?: "")
        }

        private fun PsiAnnotation.toAnnotation() = Annotations.Annotation(
            driOfReference(),
            attributes.filter { it !is KtLightAbstractAnnotation }.mapNotNull { it.attributeName to it.toValue() }.toMap()
        )

        private fun PsiElement.driOfReference() = getChildOfType<PsiJavaCodeReferenceElement>()?.resolve()?.let {
            DRI.from(it)
        } ?: DRI("", getChildOfType<PsiJavaCodeReferenceElement>()?.qualifiedName ?: "").also {
            logger.error("$this cannot be resolved to symbol!")
        }
    }

    private data class Ancestor(val dri: DRI, val isInterface: Boolean)
}
