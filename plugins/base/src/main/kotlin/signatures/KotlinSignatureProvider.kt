package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.dri
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtParameter
import kotlin.text.Typography.nbsp

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger)
    : SignatureProvider, JvmSignatureUtils by KotlinSignatureUtils {
    constructor(context: DokkaContext) : this(
        context.plugin<DokkaBase>().querySingle { commentsToContentConverter },
        context.logger
    )
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Public, KotlinVisibility.Public)
    private val ignoredModifiers = setOf(JavaModifier.Final, KotlinModifier.Final)
    private val ignoredExtraModifiers = setOf(
        ExtraModifiers.KotlinOnlyModifiers.TailRec,
        ExtraModifiers.KotlinOnlyModifiers.External
    )
    private val platformSpecificModifiers: Map<ExtraModifiers, Set<Platform>> = mapOf(
        ExtraModifiers.KotlinOnlyModifiers.External to setOf(Platform.js)
    )

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is DFunction -> functionSignature(documentable)
        is DProperty -> propertySignature(documentable)
        is DClasslike -> classlikeSignature(documentable)
        is DTypeParameter -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeAlias -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun <T> PageContentBuilder.DocumentableContentBuilder.processExtraModifiers(t: T)
            where T : Documentable, T : WithExtraProperties<T> {
        sourceSetDependentText(
            t.modifiers()
                .mapValues { entry ->
                    entry.value.filter {
                        it !in ignoredExtraModifiers || entry.key.analysisPlatform in (platformSpecificModifiers[it]
                            ?: emptySet())
                    }
                }, styles = mainStyles + TokenStyle.Keyword
        ) {
            it.toSignatureString()
        }
    }

    private fun signature(e: DEnumEntry): List<ContentNode> =
        e.sourceSets.map {
            contentBuilder.contentFor(
                e,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace),
                sourceSets = setOf(it)
            ) {
                group(styles = setOf(TextStyle.Block)) {
                    annotationsBlock(e)
                    link(e.name, e.dri, styles = mainStyles + e.stylesIfDeprecated(it))
                }
            }
        }

    private fun actualTypealiasedSignature(c: DClasslike, sourceSet: DokkaSourceSet, aliasedType: Bound): ContentGroup {
        @Suppress("UNCHECKED_CAST")
        val deprecationStyles = (c as? WithExtraProperties<out Documentable>)
            ?.stylesIfDeprecated(sourceSet)
            ?: emptySet()

        return contentBuilder.contentFor(
            c,
            ContentKind.Symbol,
            setOf(TextStyle.Monospace),
            sourceSets = setOf(sourceSet)
        ) {
            keyword("actual ")
            keyword("typealias ")
            link(c.name.orEmpty(), c.dri, styles = mainStyles + deprecationStyles)
            operator(" = ")
            signatureForProjection(aliasedType)
        }
    }

    private fun <T : DClasslike> classlikeSignature(c: T): List<ContentNode> {
        @Suppress("UNCHECKED_CAST")
        val typeAliasUnderlyingType = (c as? WithExtraProperties<out DClasslike>)
            ?.extra
            ?.get(ActualTypealias)
            ?.underlyingType

        return c.sourceSets.map { sourceSetData ->
            val sourceSetType = typeAliasUnderlyingType?.get(sourceSetData)
            if (sourceSetType == null) {
                regularSignature(c, sourceSetData)
            } else {
                actualTypealiasedSignature(c, sourceSetData, sourceSetType)
            }
        }
    }

    private fun <T : Documentable> PageContentBuilder.DocumentableContentBuilder.defaultValueAssign(
        d: WithExtraProperties<T>,
        sourceSet: DokkaSourceSet
    ) {
        // a default value of parameter can be got from expect source set
        // but expect properties cannot have a default value
        d.extra[DefaultValue]?.expression?.let {
            it[sourceSet] ?: if (d is DParameter) it[d.expectPresentInSet] else null
        }?.let { expr ->
            operator(" = ")
            highlightValue(expr)
        }
    }

    private fun regularSignature(c: DClasslike, sourceSet: DokkaSourceSet): ContentGroup {
        @Suppress("UNCHECKED_CAST")
        val deprecationStyles = (c as? WithExtraProperties<out Documentable>)
            ?.stylesIfDeprecated(sourceSet)
            ?: emptySet()

        return contentBuilder.contentFor(
            c,
            ContentKind.Symbol,
            setOf(TextStyle.Monospace),
            sourceSets = setOf(sourceSet)
        ) {
            annotationsBlock(c)
            c.visibility[sourceSet]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
            if (c.isExpectActual) keyword(if (sourceSet == c.expectPresentInSet) "expect " else "actual ")
            if (c is DClass) {
                val modifier =
                    if (c.modifier[sourceSet] !in ignoredModifiers) {
                        when {
                            c.extra[AdditionalModifiers]?.content?.get(sourceSet)?.contains(ExtraModifiers.KotlinOnlyModifiers.Data) == true -> ""
                            c.modifier[sourceSet] is JavaModifier.Empty -> "${KotlinModifier.Open.name} "
                            else -> c.modifier[sourceSet]?.name?.let { "$it " }
                        }
                    } else {
                        null
                    }
                modifier?.takeIf { it.isNotEmpty() }?.let { keyword(it) }
            }
            when (c) {
                is DClass -> {
                    processExtraModifiers(c)
                    keyword("class ")
                }
                is DInterface -> {
                    processExtraModifiers(c)
                    keyword("interface ")
                }
                is DEnum -> {
                    processExtraModifiers(c)
                    keyword("enum ")
                }
                is DObject -> {
                    processExtraModifiers(c)
                    keyword("object ")
                }
                is DAnnotation -> {
                    processExtraModifiers(c)
                    keyword("annotation class ")
                }
            }
            link(c.name!!, c.dri, styles = mainStyles + deprecationStyles)
            if (c is WithGenerics) {
                list(c.generics, prefix = "<", suffix = ">",
                    separatorStyles = mainStyles + TokenStyle.Punctuation,
                    surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                    annotationsInline(it)
                    +buildSignature(it)
                }
            }
            if (c is WithConstructors) {
                val pConstructor = c.constructors.singleOrNull { it.extra[PrimaryConstructorExtra] != null }
                if (pConstructor?.sourceSets?.contains(sourceSet) == true) {
                    if (pConstructor.annotations().values.any { it.isNotEmpty() }) {
                        text(nbsp.toString())
                        annotationsInline(pConstructor)
                        keyword("constructor")
                    }

                    // for primary constructor, opening and closing parentheses
                    // should be present only if it has parameters. If there are
                    // no parameters, it should result in `class Example`
                    if (pConstructor.parameters.isNotEmpty()) {
                        val parameterPropertiesByName = c.properties
                            .filter { it.isAlsoParameter(sourceSet) }
                            .associateBy { it.name }

                        punctuation("(")
                        parametersBlock(pConstructor) { param ->
                            annotationsInline(param)
                            parameterPropertiesByName[param.name]?.let { property ->
                                property.setter?.let { keyword("var ") } ?: keyword("val ")
                            }
                            text(param.name.orEmpty())
                            operator(": ")
                            signatureForProjection(param.type)
                            defaultValueAssign(param, sourceSet)
                        }
                        punctuation(")")
                    }
                }
            }
            if (c is WithSupertypes) {
                c.supertypes.filter { it.key == sourceSet }.map { (s, typeConstructors) ->
                    list(typeConstructors, prefix = " : ", sourceSets = setOf(s)) {
                        link(it.typeConstructor.dri.sureClassNames, it.typeConstructor.dri, sourceSets = setOf(s))
                        list(it.typeConstructor.projections, prefix = "<", suffix = "> ",
                            separatorStyles = mainStyles + TokenStyle.Punctuation,
                            surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                            signatureForProjection(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * An example would be a primary constructor `class A(val s: String)`,
     * where `s` is both a function parameter and a property
     */
    private fun DProperty.isAlsoParameter(sourceSet: DokkaSourceSet) =
        (this.sources[sourceSet] as? DescriptorDocumentableSource)?.descriptor?.findPsi() is KtParameter

    private fun propertySignature(p: DProperty) =
        p.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(
                p,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace),
                sourceSets = setOf(sourceSet)
            ) {
                annotationsBlock(p)
                p.visibility[sourceSet].takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                if (p.isExpectActual) keyword(if (sourceSet == p.expectPresentInSet) "expect " else "actual ")
                p.modifier[sourceSet].takeIf { it !in ignoredModifiers }?.let {
                        if (it is JavaModifier.Empty) KotlinModifier.Open else it
                    }?.name?.let { keyword("$it ") }
                p.modifiers()[sourceSet]?.toSignatureString()?.let { keyword(it) }
                if (p.isMutable()) keyword("var ") else keyword("val ")
                list(p.generics, prefix = "<", suffix = "> ",
                    separatorStyles = mainStyles + TokenStyle.Punctuation,
                    surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                    annotationsInline(it)
                    +buildSignature(it)
                }
                p.receiver?.also {
                    signatureForProjection(it.type)
                    punctuation(".")
                }
                link(p.name, p.dri, styles = mainStyles + p.stylesIfDeprecated(sourceSet))
                operator(": ")
                signatureForProjection(p.type)

                if (p.isNotMutable()) {
                    defaultValueAssign(p, sourceSet)
                }
            }
        }

    private fun DProperty.isNotMutable(): Boolean = !isMutable()

    private fun DProperty.isMutable(): Boolean {
        return this.extra[IsVar] != null || this.setter != null
    }

    private fun PageContentBuilder.DocumentableContentBuilder.highlightValue(expr: Expression) = when (expr) {
        is IntegerConstant -> constant(expr.value.toString())
        is FloatConstant -> constant(expr.value.toString() + "f")
        is DoubleConstant -> constant(expr.value.toString())
        is BooleanConstant -> booleanLiteral(expr.value)
        is StringConstant -> stringLiteral("\"${expr.value}\"")
        is ComplexExpression -> text(expr.value)
        else ->  Unit
    }

    private fun functionSignature(f: DFunction) =
        f.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(
                f,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace),
                sourceSets = setOf(sourceSet)
            ) {
                annotationsBlock(f)
                f.visibility[sourceSet]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                if (f.isExpectActual) keyword(if (sourceSet == f.expectPresentInSet) "expect " else "actual ")
                f.modifier[sourceSet]?.takeIf { it !in ignoredModifiers }?.let {
                    if (it is JavaModifier.Empty) KotlinModifier.Open else it
                }?.name?.let { keyword("$it ") }
                f.modifiers()[sourceSet]?.toSignatureString()?.let { keyword(it) }
                keyword("fun ")
                val usedGenerics = if (f.isConstructor) f.generics.filter { f uses it } else f.generics
                list(usedGenerics, prefix = "<", suffix = "> ",
                    separatorStyles = mainStyles + TokenStyle.Punctuation,
                    surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                    annotationsInline(it)
                    +buildSignature(it)
                }
                f.receiver?.also {
                    signatureForProjection(it.type)
                    punctuation(".")
                }
                link(f.name, f.dri, styles = mainStyles + TokenStyle.Function + f.stylesIfDeprecated(sourceSet))

                // for a function, opening and closing parentheses must be present
                // anyway, even if it has no parameters, resulting in `fun test(): R`
                punctuation("(")
                if (f.parameters.isNotEmpty()) {
                    parametersBlock(f) { param ->
                        annotationsInline(param)
                        processExtraModifiers(param)
                        text(param.name!!)
                        operator(": ")
                        signatureForProjection(param.type)
                        defaultValueAssign(param, sourceSet)
                    }
                }
                punctuation(")")
                if (f.documentReturnType()) {
                    operator(": ")
                    signatureForProjection(f.type)
                }
            }
        }

    private fun DFunction.documentReturnType() = when {
        this.isConstructor -> false
        this.type is TypeConstructor && (this.type as TypeConstructor).dri == DriOfUnit -> false
        this.type is Void -> false
        else -> true
    }

    private fun signature(t: DTypeAlias) =
        t.sourceSets.map {
            contentBuilder.contentFor(t, sourceSets = setOf(it)) {
                t.underlyingType.entries.groupBy({ it.value }, { it.key }).map { (type, platforms) ->
                    +contentBuilder.contentFor(
                        t,
                        ContentKind.Symbol,
                        setOf(TextStyle.Monospace),
                        sourceSets = platforms.toSet()
                    ) {
                        annotationsBlock(t)
                        t.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                        processExtraModifiers(t)
                        keyword("typealias ")
                        group(styles = mainStyles + t.stylesIfDeprecated(it)) {
                            signatureForProjection(t.type)
                        }
                        operator(" = ")
                        signatureForTypealiasTarget(t, type)
                    }
                }
            }
        }

    private fun signature(t: DTypeParameter) =
        t.sourceSets.map {
            contentBuilder.contentFor(t, sourceSets = setOf(it)) {
                group(styles = mainStyles + t.stylesIfDeprecated(it)) {
                    signatureForProjection(t.variantTypeParameter.withDri(t.dri.withTargetToDeclaration()))
                }
                list(
                    elements = t.nontrivialBounds,
                    prefix = " : ",
                    surroundingCharactersStyle = mainStyles + TokenStyle.Operator
                ) { bound ->
                    signatureForProjection(bound)
                }
            }
        }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForTypealiasTarget(
        typeAlias: DTypeAlias, bound: Bound
    ) {
        signatureForProjection(
            p = bound,
            showFullyQualifiedName = bound.driOrNull?.classNames == typeAlias.dri.classNames
        )
    }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(
        p: Projection, showFullyQualifiedName: Boolean = false
    ) {
        return when (p) {
            is TypeParameter -> {
                if (p.presentableName != null) {
                    text(p.presentableName!!)
                    operator(": ")
                }
                annotationsInline(p)
                link(p.name, p.dri)
            }
            is FunctionalTypeConstructor -> +funType(mainDRI.single(), mainSourcesetData, p)
            is GenericTypeConstructor ->
                group(styles = emptySet()) {
                    val linkText = if (showFullyQualifiedName && p.dri.packageName != null) {
                        "${p.dri.packageName}.${p.dri.classNames.orEmpty()}"
                    } else p.dri.classNames.orEmpty()
                    if (p.presentableName != null) {
                        text(p.presentableName!!)
                        operator(": ")
                    }
                    annotationsInline(p)
                    link(linkText, p.dri)
                    list(p.projections, prefix = "<", suffix = ">",
                        separatorStyles = mainStyles + TokenStyle.Punctuation,
                        surroundingCharactersStyle = mainStyles + TokenStyle.Operator) {
                        signatureForProjection(it, showFullyQualifiedName)
                    }
                }

            is Variance<*> -> group(styles = emptySet()) {
                keyword("$p ".takeIf { it.isNotBlank() } ?: "")
                signatureForProjection(p.inner, showFullyQualifiedName)
            }

            is Star -> operator("*")

            is Nullable -> group(styles = emptySet()) {
                signatureForProjection(p.inner, showFullyQualifiedName)
                operator("?")
            }
            is DefinitelyNonNullable -> group(styles = emptySet()) {
                signatureForProjection(p.inner, showFullyQualifiedName)
                operator(" & ")
                link("Any", DriOfAny)
            }

            is TypeAliased -> signatureForProjection(p.typeAlias)
            is JavaObject -> {
                annotationsInline(p)
                link("Any", DriOfAny)
            }
            is Void -> link("Unit", DriOfUnit)
            is PrimitiveJavaType -> signatureForProjection(p.translateToKotlin(), showFullyQualifiedName)
            is Dynamic -> text("dynamic")
            is UnresolvedBound -> text(p.name)
        }
    }

    private fun funType(dri: DRI, sourceSets: Set<DokkaSourceSet>, type: FunctionalTypeConstructor) =
        contentBuilder.contentFor(dri, sourceSets, ContentKind.Main) {

            if (type.presentableName != null) {
                text(type.presentableName!!)
                operator(": ")
            }
            annotationsInline(type)
            if (type.isSuspendable) keyword("suspend ")

            if (type.isExtensionFunction) {
                signatureForProjection(type.projections.first())
                punctuation(".")
            }

            val args = if (type.isExtensionFunction)
                type.projections.drop(1)
            else
                type.projections

            punctuation("(")
            args.subList(0, args.size - 1).forEachIndexed { i, arg ->
                signatureForProjection(arg)
                if (i < args.size - 2) punctuation(", ")
            }
            punctuation(")")
            operator(" -> ")
            signatureForProjection(args.last())
        }
}

private fun PrimitiveJavaType.translateToKotlin() = GenericTypeConstructor(
    dri = dri,
    projections = emptyList(),
    presentableName = null
)

private val DTypeParameter.nontrivialBounds: List<Bound>
    get() = bounds.filterNot { it is Nullable && it.inner.driOrNull == DriOfAny  }
