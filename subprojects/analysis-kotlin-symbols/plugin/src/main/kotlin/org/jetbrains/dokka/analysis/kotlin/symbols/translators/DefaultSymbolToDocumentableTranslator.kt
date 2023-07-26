@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.dokka.analysis.kotlin.symbols.translators


import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.*
import com.intellij.psi.util.PsiLiteralUtil
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.AnalysisContext
import org.jetbrains.dokka.analysis.kotlin.symbols.services.KtPsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getJavaDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.getKDocDocumentationFrom
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.typeConstructorsBeingExceptions
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Visibility
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import java.nio.file.Paths

// 792 / 839
//   487 / 707
class DefaultSymbolToDocumentableTranslator(context: DokkaContext) : AsyncSourceToDocumentableTranslator {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val javadocParser = JavadocParser(
        docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers },
        docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
    )

    override suspend fun invokeSuspending(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        context: DokkaContext
    ): DModule {
        val analysisContext = kotlinAnalysis[sourceSet]
        @Suppress("unused")
        return DokkaSymbolVisitor(
            sourceSet = sourceSet,
            moduleName = context.configuration.moduleName,
            analysisContext = analysisContext,
            logger = context.logger,
            javadocParser = javadocParser
        ).visitModule()
    }
}

class TranslatorError(message: String, cause: Throwable?) : IllegalStateException(message, cause)

inline fun <R> KtAnalysisSession.withExceptionCatcher(symbol: KtSymbol, action: KtAnalysisSession.() -> R): R =
    try {
        action()
    } catch (e: TranslatorError) {
        throw e
    } catch (e: Throwable) {
        val file = try {
            symbol.psi?.containingFile?.virtualFile?.path
        } catch (e: Throwable) {
            "[$e]"
        }
        val textRange = try {
            symbol.psi?.textRange.toString()
        } catch (e: Throwable) {
            "[$e]"
        }
        throw TranslatorError(
            "Error in translating of symbol (${(symbol as? KtNamedSymbol)?.name}) $symbol in file: $file, $textRange",
            e
        )
    }
internal fun <T : Bound> T.wrapWithVariance(variance: org.jetbrains.kotlin.types.Variance) =
    when (variance) {
        org.jetbrains.kotlin.types.Variance.INVARIANT -> Invariance(this)
        org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Contravariance(this)
        org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Covariance(this)
    }

internal class DokkaSymbolVisitor(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val moduleName: String,
    private val analysisContext: AnalysisContext,
    private val logger: DokkaLogger,
    private val javadocParser: JavadocParser? = null
) {
    private var annotationTranslator = AnnotationTranslator()
    private var typeTranslator = TypeTranslator(sourceSet, annotationTranslator)

    /**
     * to avoid recursive classes
     * e.g.
     * open class Klass() {
     *   object Default : Klass()
     * }
     */
    private val visitedNamedClassOrObjectSymbol: MutableSet<KtNamedClassOrObjectSymbol> =
        mutableSetOf() // or MutableSet<ClassId>

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    // KT-54846 will replace
    private val KtDeclarationSymbol.isActual
        get() = (psi as? KtModifierListOwner)?.hasActualModifier() == true
    private val KtDeclarationSymbol.isExpect
        get() = (psi as? KtModifierListOwner)?.hasExpectModifier() == true

    private fun <T : KtSymbol> Collection<T>.filterSymbolsInSourceSet() = filter {
        it.psi?.containingFile?.virtualFile?.path?.let { path ->
            path.isNotBlank() && sourceSet.sourceRoots.any { root ->
                Paths.get(path).startsWith(root.toPath())
            }
        } == true
    }

    internal fun visitModule(): DModule {
        val ktFiles: List<KtFile> = getPsiFilesFromPaths(
            analysisContext.project,
            getSourceFilePaths(sourceSet.sourceRoots.map { it.canonicalPath })
        )
        val processedPackages: MutableSet<FqName> = mutableSetOf()
        return analyze(analysisContext.mainModule) {
            val packageSymbols: List<DPackage> = ktFiles
                .mapNotNull {
                    if (processedPackages.contains(it.packageFqName))
                        return@mapNotNull null
                    processedPackages.add(it.packageFqName)
                    getPackageSymbolIfPackageExists(it.packageFqName)?.let { it1 ->
                        visitPackageSymbol(
                            it1
                        )
                    }
                }
            
            DModule(
                name = moduleName,
                packages = packageSymbols,
                documentation = emptyMap(),
                expectPresentInSet = null,
                sourceSets = setOf(sourceSet)
            )
        }
    }

    private fun KtAnalysisSession.visitPackageSymbol(packageSymbol: KtPackageSymbol): DPackage {
        val dri = getDRIFromPackage(packageSymbol)
        val scope = packageSymbol.getPackageScope()
        val callables = scope.getCallableSymbols().toList().filterSymbolsInSourceSet()
        val classifiers = scope.getClassifierSymbols().toList().filterSymbolsInSourceSet()

        val functions = callables.filterIsInstance<KtFunctionSymbol>().map { visitFunctionSymbol(it, dri) }
        val properties = callables.filterIsInstance<KtPropertySymbol>().map { visitPropertySymbol(it, dri) }
        val classlikes =
            classifiers.filterIsInstance<KtNamedClassOrObjectSymbol>()
                .map { visitNamedClassOrObjectSymbol(it, dri) }
        val typealiases = classifiers.filterIsInstance<KtTypeAliasSymbol>().map { visitTypeAliasSymbol(it, dri) }

        return DPackage(
            dri = dri,
            functions = functions,
            properties = properties,
            classlikes = classlikes,
            typealiases = typealiases,
            documentation = emptyMap(),
            sourceSets = setOf(sourceSet)
        )
    }

    private fun KtAnalysisSession.visitTypeAliasSymbol(
        typeAliasSymbol: KtTypeAliasSymbol,
        parent: DRI
    ): DTypeAlias = withExceptionCatcher(typeAliasSymbol) {
        val name = typeAliasSymbol.name.asString()
        val dri = parent.withClass(name)

        val ancestryInfo = with(typeTranslator) { buildAncestryInformationFrom(typeAliasSymbol.expandedType) }

        val generics =
            typeAliasSymbol.typeParameters.mapIndexed { index, symbol -> visitVariantTypeParameter(index, symbol, dri) }

        return DTypeAlias(
            dri = dri,
            name = name,
            type = GenericTypeConstructor(
                dri = dri,
                projections = generics.map { it.variantTypeParameter }), // this property can be removed in DTypeAlias
            expectPresentInSet = null,
            underlyingType = toBoundFrom(typeAliasSymbol.expandedType).toSourceSetDependent(),
            visibility = typeAliasSymbol.getDokkaVisibility().toSourceSetDependent(),
            documentation = getDocumentation(typeAliasSymbol)?.toSourceSetDependent() ?: emptyMap(),
            sourceSets = setOf(sourceSet),
            generics = generics,
            sources = typeAliasSymbol.getSource(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(typeAliasSymbol)?.toSourceSetDependent()?.toAnnotations(),
                ancestryInfo.exceptionInSupertypesOrNull(),
            )
        )

    }

    fun KtAnalysisSession.visitNamedClassOrObjectSymbol(
        namedClassOrObjectSymbol: KtNamedClassOrObjectSymbol,
        parent: DRI
    ): DClasslike = withExceptionCatcher(namedClassOrObjectSymbol) {
        visitedNamedClassOrObjectSymbol.add(namedClassOrObjectSymbol)

        val name = namedClassOrObjectSymbol.name.asString()
        val dri = parent.withClass(name)

        val scope = namedClassOrObjectSymbol.getMemberScope()
        val isExpect = namedClassOrObjectSymbol.isExpect
        val isActual = namedClassOrObjectSymbol.isActual
        val documentation = getDocumentation(namedClassOrObjectSymbol)?.toSourceSetDependent() ?: emptyMap()

        val constructors = scope.getConstructors().map { visitConstructorSymbol(it, dri) }.toList()


        val callables = scope.getCallableSymbols().toList()
        val classifiers = scope.getClassifierSymbols().toList()


        val javaFields = callables.filterIsInstance<KtJavaFieldSymbol>()

        /**
         * TODO: For synthetic Java properties KTIJ-22359 to research:
         *
         *         val syntheticProperties = getSyntheticJavaPropertiesScope()
         *         ?.filterIsInstance<KtSyntheticJavaPropertySymbol>()
         *         .orEmpty()
         */
        val (regularFunctions, accessors) = splitFunctionsAndInheritedAccessors(
            fields = javaFields,
            functions = callables.filterIsInstance<KtFunctionSymbol>()
        )

        val functions = regularFunctions.map { visitFunctionSymbol(it, dri) }
        val properties = callables.filterIsInstance<KtPropertySymbol>().map { visitPropertySymbol(it, dri) } +
                javaFields.map {
                    visitJavaFieldSymbol(
                        javaFieldSymbol = it,
                        parent = dri,
                        getterFunctionSymbol = accessors[it]?.getter,
                        setterFunctionSymbol = accessors[it]?.setter
                    )
                }


       // hack, by default, compiler adds an empty companion object for enum
        fun List<KtNamedClassOrObjectSymbol>.filterOutEnumCompanion() = filterNot {
            if (namedClassOrObjectSymbol.classKind == KtClassKind.ENUM_CLASS)
                it.name.asString() == "Companion"
            else false
        }

        val classlikes = classifiers.filterIsInstance<KtNamedClassOrObjectSymbol>()
            .filterOutEnumCompanion() // hack to filter out companion for enum
            .filterNot { symbol -> visitedNamedClassOrObjectSymbol.contains(symbol).also { if(!it) visitedNamedClassOrObjectSymbol.add(symbol) } }
            .map { visitNamedClassOrObjectSymbol(it, dri) }

        val generics = namedClassOrObjectSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        val ancestryInfo = with(typeTranslator) { buildAncestryInformationFrom(namedClassOrObjectSymbol.buildSelfClassType()) }
        val supertypes =
            //(ancestryInfo.interfaces.map{ it.typeConstructor } + listOfNotNull(ancestryInfo.superclass?.typeConstructor))
            namedClassOrObjectSymbol.superTypes.filterNot { it.isAny }.map {  with(typeTranslator) { toTypeConstructorWithKindFrom(it) } }
                .toSourceSetDependent()

        return@withExceptionCatcher when (namedClassOrObjectSymbol.classKind) {
            KtClassKind.OBJECT, KtClassKind.COMPANION_OBJECT ->
                DObject(
                    dri = dri,
                    name = name,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    sources = namedClassOrObjectSymbol.getSource(),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                        ancestryInfo.exceptionInSupertypesOrNull()
                    )
                )

            KtClassKind.CLASS -> DClass(
                dri = dri,
                name = name,
                constructors = constructors,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                sources = namedClassOrObjectSymbol.getSource(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                modifier = namedClassOrObjectSymbol.getDokkaModality().toSourceSetDependent(),
                companion = namedClassOrObjectSymbol.companionObject?.let {
                    visitNamedClassOrObjectSymbol(
                        it,
                        dri
                    )
                } as? DObject,
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KtClassKind.INTERFACE -> DInterface(
                dri = dri,
                name = name,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                sources = namedClassOrObjectSymbol.getSource(), //
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                supertypes = supertypes,
                generics = generics,
                documentation = documentation,
                companion = namedClassOrObjectSymbol.companionObject?.let {
                    visitNamedClassOrObjectSymbol(
                        it,
                        dri
                    )
                } as? DObject,
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                    ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent()),
                    ancestryInfo.exceptionInSupertypesOrNull()
                )
            )

            KtClassKind.ANNOTATION_CLASS -> DAnnotation(
                dri = dri,
                name = name,
                documentation = documentation,
                functions = functions,
                properties = properties,
                classlikes = classlikes,
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                companion = namedClassOrObjectSymbol.companionObject?.let {
                    visitNamedClassOrObjectSymbol(
                        it,
                        dri
                    )
                } as? DObject,
                visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                generics = generics,
                constructors = constructors,
                sources = namedClassOrObjectSymbol.getSource(),
                extra = PropertyContainer.withAll(
                    namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                    getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                )
            )

            KtClassKind.ANONYMOUS_OBJECT -> throw NotImplementedError("ANONYMOUS_OBJECT does not support")
            KtClassKind.ENUM_CLASS -> {
                val entries = namedClassOrObjectSymbol.getEnumEntries().map { visitEnumEntrySymbol(it) }

                DEnum(
                    dri = dri,
                    name = name,
                    entries = entries,
                    constructors = constructors,
                    functions = functions,
                    properties = properties,
                    classlikes = classlikes,
                    sources = namedClassOrObjectSymbol.getSource(),
                    expectPresentInSet = sourceSet.takeIf { isExpect },
                    visibility = namedClassOrObjectSymbol.getDokkaVisibility().toSourceSetDependent(),
                    supertypes = supertypes,
                    documentation = documentation,
                    companion = namedClassOrObjectSymbol.companionObject?.let {
                        visitNamedClassOrObjectSymbol(
                            it,
                            dri
                        )
                    } as? DObject,
                    sourceSets = setOf(sourceSet),
                    isExpectActual = (isExpect || isActual),
                    extra = PropertyContainer.withAll(
                        namedClassOrObjectSymbol.additionalExtras()?.toSourceSetDependent()
                            ?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(namedClassOrObjectSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        ImplementedInterfaces(ancestryInfo.allImplementedInterfaces().toSourceSetDependent())
                    )
                )
            }
        }
    }

    private fun KtAnalysisSession.visitEnumEntrySymbol(
        enumEntrySymbol: KtEnumEntrySymbol
    ): DEnumEntry = withExceptionCatcher(enumEntrySymbol) {
        val dri = getDRIFromEnumEntry(enumEntrySymbol)
        val isExpect = false

        val scope = enumEntrySymbol.getMemberScope()
        val callables = scope.getCallableSymbols().toList()
        val classifiers = scope.getClassifierSymbols().toList()

        val functions = callables.filterIsInstance<KtFunctionSymbol>().map { visitFunctionSymbol(it, dri) }
        val properties = callables.filterIsInstance<KtPropertySymbol>().map { visitPropertySymbol(it, dri) }
        val classlikes =
            classifiers.filterIsInstance<KtNamedClassOrObjectSymbol>()
                .map { visitNamedClassOrObjectSymbol(it, dri) }

        return DEnumEntry(
            dri = dri,
            name = enumEntrySymbol.name.asString(),
            documentation = getDocumentation(enumEntrySymbol)?.toSourceSetDependent() ?: emptyMap(),
            functions = functions,
            properties = properties,
            classlikes = classlikes,
            sourceSets = setOf(sourceSet),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(enumEntrySymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KtAnalysisSession.visitPropertySymbol(propertySymbol: KtPropertySymbol, parent: DRI): DProperty =
        withExceptionCatcher(propertySymbol) {
            val dri = createDRIWithOverridden(propertySymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = propertySymbol.isExpect
            val isActual = propertySymbol.isActual
            propertySymbol.origin
            val generics =
                propertySymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        dri
                    )
                }

            return DProperty(
                dri = dri,
                name = propertySymbol.name.asString(),
                receiver = propertySymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                sources = propertySymbol.getSource(),
                getter = propertySymbol.getter?.let { visitPropertyAccessor(it, propertySymbol, dri) },
                setter = propertySymbol.setter?.let { visitPropertyAccessor(it, propertySymbol, dri) },
                visibility = propertySymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
                documentation = getDocumentation(propertySymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = propertySymbol.modality.toDokkaModifier().toSourceSetDependent(),
                type = toBoundFrom(propertySymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                        propertySymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(propertySymbol)?.toSourceSetDependent()?.toAnnotations(),
                        propertySymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                        inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                        takeUnless { propertySymbol.isVal }?.let { IsVar },
                        takeIf { propertySymbol.psi is KtParameter }?.let { IsAlsoParameter(listOf(sourceSet)) }
                )
            )
        }

    private fun KtJavaFieldSymbol.getVisibility(getterFunctionSymbol: KtFunctionSymbol? = null): Visibility {
        val isNonPublicJavaProperty = !this.visibility.isPublicAPI
        val visibility =
            if (isNonPublicJavaProperty) {
                // only try to take implicit getter's visibility if it's a java property
                // because it's not guaranteed that implicit accessor will be used
                // for the kotlin property, as it may have an explicit accessor of its own,
                // i.eю in data classes or with get() and set() are overridden
                getterFunctionSymbol?.visibility ?: this.visibility
            } else {
                this.visibility
            }

        return visibility.toDokkaVisibility()
    }

    private fun KtAnalysisSession.visitJavaFieldSymbol(
        javaFieldSymbol: KtJavaFieldSymbol,
        parent: DRI,
        getterFunctionSymbol: KtFunctionSymbol? = null,
        setterFunctionSymbol: KtFunctionSymbol? = null,
    ): DProperty =
        withExceptionCatcher(javaFieldSymbol) {
            val dri = createDRIWithOverridden(javaFieldSymbol).origin
            val inheritedFrom = dri.getInheritedFromDRI(parent)
            val isExpect = false
            val isActual = false
            val generics =
                javaFieldSymbol.typeParameters.mapIndexed { index, symbol ->
                    visitVariantTypeParameter(
                        index,
                        symbol,
                        dri
                    )
                }

            return DProperty(
                dri = dri,
                name = javaFieldSymbol.name.asString(),
                receiver = javaFieldSymbol.receiverParameter?.let {
                    visitReceiverParameter(
                        it,
                        dri
                    )
                },
                sources = javaFieldSymbol.getSource(),
                getter = getterFunctionSymbol?.let { visitFunctionSymbol(it, parent) },
                setter = setterFunctionSymbol?.let { visitFunctionSymbol(it, parent) },
                visibility = javaFieldSymbol.getVisibility(getterFunctionSymbol).toSourceSetDependent(),
                documentation = getDocumentation(javaFieldSymbol)?.toSourceSetDependent() ?: emptyMap(), // TODO
                modifier = javaFieldSymbol.modality.toDokkaModifier().toSourceSetDependent(),
                type = toBoundFrom(javaFieldSymbol.returnType),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                generics = generics,
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                        javaFieldSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                        getDokkaAnnotationsFrom(javaFieldSymbol)?.toSourceSetDependent()?.toAnnotations(),
                        //javaFieldSymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) },
                        inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                        // non-final java property should be var if it has no accessors at all or has a setter
                        IsVar.takeIf { (!javaFieldSymbol.isVal && getterFunctionSymbol == null && setterFunctionSymbol == null) || setterFunctionSymbol != null }
                )
            )
        }

    private fun KtAnalysisSession.visitPropertyAccessor(
        propertyAccessorSymbol: KtPropertyAccessorSymbol,
        propertySymbol: KtPropertySymbol,
        propertyDRI: DRI
    ): DFunction = withExceptionCatcher(propertyAccessorSymbol) {
        val dri = propertyDRI.copy(
            callable = Callable("", null, emptyList())
        )
        val isExpect = propertyAccessorSymbol.isExpect
        val isActual = propertyAccessorSymbol.isActual

        val generics = propertyAccessorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }
        val isGetter = propertyAccessorSymbol is KtPropertyGetterSymbol

        val name = run {
            val modifier = if (isGetter) "get" else "set"
            val rawName = propertySymbol.name.asString()
            "$modifier${rawName.capitalize()}"
            /*
             * Kotlin has special rules for conversion around properties that
             * start with "is" For more info see:
             * https://kotlinlang.org/docs/java-interop.html#getters-and-setters
             * https://kotlinlang.org/docs/java-to-kotlin-interop.html#properties
             *
             * Based on our testing, this rule only applies when the letter after
             * the "is" is *not* lowercase. This means that words like "issue" won't
             * have the rule applied but "is_foobar" and "is1of" will have the rule applied.
             */
            val specialCaseIs = rawName.startsWith("is")
                    && rawName.getOrNull(2)?.isLowerCase() == false

            if (specialCaseIs) {
                if (isGetter) rawName else rawName.replaceFirst("is", "set")
            } else {
                if (isGetter) "get${rawName.capitalize()}" else "set${rawName.capitalize()}"
            }
        }

        return DFunction(
            dri = dri,
            name = name,
            isConstructor = false,
            receiver = propertyAccessorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            parameters = propertyAccessorSymbol.valueParameters.mapIndexed { index, symbol ->
                visitValueParameter(index, symbol, dri)
            },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = propertyAccessorSymbol.getSource(),
            visibility = propertyAccessorSymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = getDocumentation(propertyAccessorSymbol)?.toSourceSetDependent() ?: emptyMap(),
            modifier = propertyAccessorSymbol.modality.toDokkaModifier().toSourceSetDependent(),
            type = toBoundFrom(propertyAccessorSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                propertyAccessorSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                getDokkaAnnotationsFrom(propertyAccessorSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KtAnalysisSession.visitConstructorSymbol(
        constructorSymbol: KtConstructorSymbol,
        parent: DRI
    ): DFunction = withExceptionCatcher(constructorSymbol) {
        val name = constructorSymbol.containingClassIdIfNonLocal?.shortClassName?.asString()
            ?: throw IllegalStateException("Unknown containing class of constructor")
        val dri = createDRIWithOverridden(constructorSymbol).origin
        val isExpect = false // TODO
        val isActual = false // TODO

        val generics = constructorSymbol.typeParameters.mapIndexed { index, symbol ->
            visitVariantTypeParameter(
                index,
                symbol,
                dri
            )
        }

        val documentation = getDocumentation(constructorSymbol)?.let { docNode ->
            if (constructorSymbol.isPrimary) {
                docNode.copy(children = (docNode.children.find { it is Constructor }?.root?.let { constructor ->
                    listOf(Description(constructor))
                } ?: emptyList<TagWrapper>()) + docNode.children.filterIsInstance<Param>())
            } else {
                docNode
            }
        }?.toSourceSetDependent()

        return DFunction(
            dri = dri,
            name = name,
            isConstructor = true,
            receiver = constructorSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            parameters = constructorSymbol.valueParameters.mapIndexed { index, symbol ->
                visitValueParameter(index, symbol, dri)
            },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = constructorSymbol.getSource(),
            visibility = constructorSymbol.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = documentation ?: emptyMap(),
            modifier = KotlinModifier.Empty.toSourceSetDependent(),
            type = toBoundFrom(constructorSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(constructorSymbol)?.toSourceSetDependent()?.toAnnotations(),
                takeIf { constructorSymbol.isPrimary }?.let { PrimaryConstructorExtra }
            )
        )
    }

    private fun KtAnalysisSession.visitFunctionSymbol(functionSymbol: KtFunctionSymbol, parent: DRI): DFunction = withExceptionCatcher(functionSymbol) {
        val dri = createDRIWithOverridden(functionSymbol).origin
        val inheritedFrom = dri.getInheritedFromDRI(parent)
        val isExpect = functionSymbol.isExpect
        val isActual = functionSymbol.isActual

        val generics =
            functionSymbol.typeParameters.mapIndexed { index, symbol -> visitVariantTypeParameter(index, symbol, dri) }

        return DFunction(
            dri = dri,
            name = functionSymbol.name.asString(),
            isConstructor = false,
            receiver = functionSymbol.receiverParameter?.let {
                visitReceiverParameter(
                    it,
                    dri
                )
            },
            parameters = functionSymbol.valueParameters.mapIndexed { index, symbol ->
                visitValueParameter(index, symbol, dri)
            },
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = functionSymbol.getSource(),
            visibility = functionSymbol.getDokkaVisibility().toSourceSetDependent(),
            generics = generics,
            documentation = getDocumentation(functionSymbol)?.toSourceSetDependent() ?: emptyMap(),
            modifier = functionSymbol.getDokkaModality().toSourceSetDependent(),
            type = toBoundFrom(functionSymbol.returnType),
            sourceSets = setOf(sourceSet),
            isExpectActual = (isExpect || isActual),
            extra = PropertyContainer.withAll(
                inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                functionSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
                getDokkaAnnotationsFrom(functionSymbol)
                    ?.toSourceSetDependent()?.toAnnotations(),
                ObviousMember.takeIf { isObvious(functionSymbol) },
            )
        )
    }

    private fun KtAnalysisSession.visitValueParameter(
        index: Int, valueParameterSymbol: KtValueParameterSymbol, dri: DRI
    ) = DParameter(
        dri = dri.copy(target = PointingToCallableParameters(index)),
        name = valueParameterSymbol.name.asString(),
        type = toBoundFrom(valueParameterSymbol.returnType),
        expectPresentInSet = null,
        documentation = getDocumentation(valueParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(
            valueParameterSymbol.additionalExtras()?.toSourceSetDependent()?.toAdditionalModifiers(),
            getDokkaAnnotationsFrom(valueParameterSymbol)?.toSourceSetDependent()?.toAnnotations(),
            valueParameterSymbol.getDefaultValue()?.let { DefaultValue(it.toSourceSetDependent()) }
        )
    )

    private fun KtAnalysisSession.visitReceiverParameter(
        receiverParameterSymbol: KtReceiverParameterSymbol, dri: DRI
    ) = DParameter(
        dri = dri.copy(target = PointingToDeclaration),
        name = null,
        type = toBoundFrom(receiverParameterSymbol.type),
        expectPresentInSet = null,
        documentation = getDocumentation(receiverParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(receiverParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
        )
    )

    private fun KtValueParameterSymbol.getDefaultValue(): Expression? =
        if (origin == KtSymbolOrigin.SOURCE) (psi as? KtParameter)?.defaultValue?.toDefaultValueExpression()
        else null

    private fun KtPropertySymbol.getDefaultValue(): Expression? =
        if (origin == KtSymbolOrigin.SOURCE) psi?.children?.filterIsInstance<KtConstantExpression>()?.firstOrNull()
            ?.toDefaultValueExpression() else null


    private fun KtExpression.toDefaultValueExpression(): Expression? = when (node?.elementType) {
        KtNodeTypes.INTEGER_CONSTANT -> PsiLiteralUtil.parseLong(node?.text)?.let { IntegerConstant(it) }
        KtNodeTypes.FLOAT_CONSTANT -> if (node?.text?.toLowerCase()?.endsWith('f') == true)
            PsiLiteralUtil.parseFloat(node?.text)?.let { FloatConstant(it) }
        else PsiLiteralUtil.parseDouble(node?.text)?.let { DoubleConstant(it) }

        KtNodeTypes.BOOLEAN_CONSTANT -> BooleanConstant(node?.text == "true")
        KtNodeTypes.STRING_TEMPLATE -> StringConstant(node.findChildByType(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)?.text.orEmpty())
        else -> node?.text?.let { ComplexExpression(it) }
    }

    private fun KtAnalysisSession.visitVariantTypeParameter(
        index: Int,
        typeParameterSymbol: KtTypeParameterSymbol,
        dri: DRI
    ): DTypeParameter {
        val upperBoundsOrNullableAny = typeParameterSymbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.NULLABLE_ANY)
        return DTypeParameter(
            variantTypeParameter = TypeParameter(
                dri = dri.copy(target = PointingToGenericParameters(index)),
                name = typeParameterSymbol.name.asString(),
                presentableName = typeParameterSymbol.getPresentableName()
            ).wrapWithVariance(typeParameterSymbol.variance),
            documentation = getDocumentation(typeParameterSymbol)?.toSourceSetDependent() ?: emptyMap(),
            expectPresentInSet = null,
            bounds = upperBoundsOrNullableAny.map { toBoundFrom(it) },
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(typeParameterSymbol)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }
    // ----------- Utils ----------------------------------------------------------------------------

    private fun KtAnalysisSession.getDokkaAnnotationsFrom(annotated: KtAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KtAnalysisSession.toBoundFrom(type: KtType) =
        with(typeTranslator) { toBoundFrom(type) }

    /**
     * `createDRI` returns the DRI of the exact element and potential DRI of an element that is overriding it
     * (It can be also FAKE_OVERRIDE which is in fact just inheritance of the symbol)
     *
     * Looking at what PSIs do, they give the DRI of the element within the classnames where it is actually
     * declared and inheritedFrom as the same DRI but truncated callable part.
     * Therefore, we set callable to null and take the DRI only if it is indeed coming from different class.
     */
    private fun DRI.getInheritedFromDRI(dri: DRI): DRI? {
        return this.copy(callable = null)
            .takeIf { dri.classNames != this.classNames || dri.packageName != this.packageName }
    }

    data class DRIWithOverridden(val origin: DRI, val overridden: DRI? = null)

    private fun KtAnalysisSession.createDRIWithOverridden(
        callableSymbol: KtCallableSymbol,
        wasOverriddenBy: DRI? = null
    ): DRIWithOverridden {
        if (callableSymbol is KtPropertySymbol && callableSymbol.isOverride
            || callableSymbol is KtFunctionSymbol && callableSymbol.isOverride
        ) {
            return DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)
        }

        val overriddenSymbols = callableSymbol.getAllOverriddenSymbols()
        // For already
        return if (overriddenSymbols.isEmpty()) {
            DRIWithOverridden(getDRIFromSymbol(callableSymbol), wasOverriddenBy)
        } else {
            createDRIWithOverridden(overriddenSymbols.first())
        }
    }

    private fun KtAnalysisSession.getDocumentation(symbol: KtSymbol) =
        getKDocDocumentationFrom(symbol) ?: javadocParser?.let { getJavaDocDocumentationFrom(symbol, it) }

    private fun isObvious(functionSymbol: KtFunctionSymbol): Boolean {
        return functionSymbol.origin == KtSymbolOrigin.SOURCE_MEMBER_GENERATED ||
                !functionSymbol.isOverride && functionSymbol.callableIdIfNonLocal?.classId?.isObvious() == true
    }
    private fun ClassId.isObvious(): Boolean = with(asString()) {
        return this == "kotlin/Any" || this == "kotlin/Enum"
                || this == "java.lang/Object" || this == "java.lang/Enum"
    }

    private fun KtSymbol.getSource() = KtPsiDocumentableSource(psi).toSourceSetDependent()

    private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
        typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }


    // ----------- Translators of modifiers ----------------------------------------------------------------------------
    private fun KtSymbolWithModality.getDokkaModality() = modality.toDokkaModifier()
    private fun KtSymbolWithVisibility.getDokkaVisibility() = visibility.toDokkaVisibility()
    private fun KtValueParameterSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.NoInline.takeIf { isNoinline },
        ExtraModifiers.KotlinOnlyModifiers.CrossInline.takeIf { isCrossinline },
//ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { isConst },
//ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { isLateInit },
        ExtraModifiers.KotlinOnlyModifiers.VarArg.takeIf { isVararg }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtPropertyAccessorSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtPropertySymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { (this as? KtKotlinPropertySymbol)?.isConst == true },
        ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { (this as? KtKotlinPropertySymbol)?.isLateInit == true },
        //ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        //ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        //ExtraModifiers.KotlinOnlyModifiers.Static.takeIf { isStatic },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtJavaFieldSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isStatic }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtFunctionSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Infix.takeIf { isInfix },
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
        ExtraModifiers.KotlinOnlyModifiers.Suspend.takeIf { isSuspend },
        ExtraModifiers.KotlinOnlyModifiers.Operator.takeIf { isOperator },
//ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.TailRec.takeIf { (psi as? KtNamedFunction)?.hasModifier(KtTokens.TAILREC_KEYWORD) == true },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { isOverride }
    ).toSet().takeUnless { it.isEmpty() }

    private fun KtNamedClassOrObjectSymbol.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { (this.psi as? KtClass)?.isInline() == true },
        ExtraModifiers.KotlinOnlyModifiers.Value.takeIf { (this.psi as? KtClass)?.isValue() == true },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Inner.takeIf { isInner },
        ExtraModifiers.KotlinOnlyModifiers.Data.takeIf { isData },
        ExtraModifiers.KotlinOnlyModifiers.Fun.takeIf { isFun },
    ).toSet().takeUnless { it.isEmpty() }

    private fun Modality.toDokkaModifier() = when (this) {
        Modality.FINAL -> KotlinModifier.Final //  OR else -> KotlinModifier.Empty
        Modality.SEALED -> KotlinModifier.Sealed
        Modality.OPEN -> KotlinModifier.Open
        Modality.ABSTRACT -> KotlinModifier.Abstract
    }


    private fun org.jetbrains.kotlin.descriptors.Visibility.toDokkaVisibility(): Visibility = when (this) {
        Visibilities.Public -> KotlinVisibility.Public
        Visibilities.Protected -> KotlinVisibility.Protected
        Visibilities.Internal -> KotlinVisibility.Internal
        Visibilities.Private, Visibilities.PrivateToThis -> KotlinVisibility.Private
        JavaVisibilities.ProtectedAndPackage -> KotlinVisibility.Protected
        JavaVisibilities.ProtectedStaticVisibility -> KotlinVisibility.Protected
        JavaVisibilities.PackageVisibility -> JavaVisibility.Default
        else -> KotlinVisibility.Public
    }
}






