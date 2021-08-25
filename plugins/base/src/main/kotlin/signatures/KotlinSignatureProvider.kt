package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.Platform
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
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.pages.TokenStyle
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
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
                setOf(TextStyle.Monospace) + e.stylesIfDeprecated(it),
                sourceSets = setOf(it)
            ) {
                group(styles = setOf(TextStyle.Block)) {
                    annotationsBlock(e)
                    link(e.name, e.dri, styles = emptySet())
                    e.extra[ConstructorValues]?.let { constructorValues ->
                        constructorValues.values[it]?.let { values ->
                            punctuation("(")
                            list(
                                elements = values,
                                separator = ", ",
                                separatorStyles = mainStyles + TokenStyle.Punctuation,
                            ) { highlightValue(it) }
                            punctuation(")")
                        }
                    }
                }
            }
        }

    private fun actualTypealiasedSignature(c: DClasslike, sourceSet: DokkaSourceSet, aliasedType: Bound) =
        contentBuilder.contentFor(
            c,
            ContentKind.Symbol,
            setOf(TextStyle.Monospace) + ((c as? WithExtraProperties<out Documentable>)?.stylesIfDeprecated(sourceSet)
                ?: emptySet()),
            sourceSets = setOf(sourceSet)
        ) {
            keyword("typealias ")
            link(c.name.orEmpty(), c.dri)
            operator(" = ")
            signatureForProjection(aliasedType)
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : DClasslike> classlikeSignature(c: T): List<ContentNode> =
        c.sourceSets.map { sourceSetData ->
            (c as? WithExtraProperties<out DClasslike>)?.extra?.get(ActualTypealias)?.underlyingType?.get(sourceSetData)
                ?.let {
                    actualTypealiasedSignature(c, sourceSetData, it)
                } ?: regularSignature(c, sourceSetData)
        }


    private fun regularSignature(c: DClasslike, sourceSet: DokkaSourceSet) =
        contentBuilder.contentFor(
            c,
            ContentKind.Symbol,
            setOf(TextStyle.Monospace) + ((c as? WithExtraProperties<out Documentable>)?.stylesIfDeprecated(sourceSet)
                ?: emptySet()),
            sourceSets = setOf(sourceSet)
        ) {
            annotationsBlock(c)
            c.visibility[sourceSet]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
            if (c is DClass) {
               val modifier = if (c.modifier[sourceSet] !in ignoredModifiers)
                        when {
                            c.extra[AdditionalModifiers]?.content?.get(sourceSet)?.contains(ExtraModifiers.KotlinOnlyModifiers.Data) == true -> ""
                            c.modifier[sourceSet] is JavaModifier.Empty -> "${KotlinModifier.Open.name} "
                            else -> c.modifier[sourceSet]?.name?.let { "$it " } ?: ""
                        }
                    else
                        ""
                modifier.takeIf { it.isNotEmpty() }?.let { keyword(it) }
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
            link(c.name!!, c.dri)
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
                    list(
                        elements = pConstructor.parameters,
                        prefix = "(",
                        suffix = ")",
                        separator = ", ",
                        separatorStyles = mainStyles + TokenStyle.Punctuation,
                        surroundingCharactersStyle = mainStyles + TokenStyle.Punctuation,
                        sourceSets = pConstructor.sourceSets.toSet()
                    ) {
                        annotationsInline(it)
                        text(it.name.orEmpty())
                        operator(": ")
                        signatureForProjection(it.type)
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

    private fun propertySignature(p: DProperty) =
        p.sourceSets.map {
            contentBuilder.contentFor(
                p,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace) + p.stylesIfDeprecated(it),
                sourceSets = setOf(it)
            ) {
                annotationsBlock(p)
                p.visibility[it].takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                p.modifier[it].takeIf { it !in ignoredModifiers }?.let {
                        if (it is JavaModifier.Empty) KotlinModifier.Open else it
                    }?.name?.let { keyword("$it ") }
                p.modifiers()[it]?.toSignatureString()?.let { keyword(it) }
                p.setter?.let { keyword("var ") } ?: keyword("val ")
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
                link(p.name, p.dri)
                operator(": ")
                signatureForProjection(p.type)
                p.extra[DefaultValue]?.run {
                    operator(" = ")
                    highlightValue(value)
                }
            }
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
        f.sourceSets.map {
            contentBuilder.contentFor(
                f,
                ContentKind.Symbol,
                setOf(TextStyle.Monospace) + f.stylesIfDeprecated(it),
                sourceSets = setOf(it)
            ) {
                annotationsBlock(f)
                f.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                f.modifier[it]?.takeIf { it !in ignoredModifiers }?.let {
                    if (it is JavaModifier.Empty) KotlinModifier.Open else it
                }?.name?.let { keyword("$it ") }
                f.modifiers()[it]?.toSignatureString()?.let { keyword(it) }
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
                link(f.name, f.dri, styles = mainStyles + TokenStyle.Function)
                punctuation("(")
                list(f.parameters,
                    separatorStyles = mainStyles + TokenStyle.Punctuation) {
                    annotationsInline(it)
                    processExtraModifiers(it)
                    text(it.name!!)
                    operator(": ")
                    signatureForProjection(it.type)
                    it.extra[DefaultValue]?.run {
                        operator(" = ")
                        highlightValue(value)
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
                        setOf(TextStyle.Monospace) + t.stylesIfDeprecated(it),
                        sourceSets = platforms.toSet()
                    ) {
                        annotationsBlock(t)
                        t.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.let { keyword("$it ") }
                        processExtraModifiers(t)
                        keyword("typealias ")
                        signatureForProjection(t.type)
                        operator(" = ")
                        signatureForTypealiasTarget(t, type)
                    }
                }
            }
        }

    private fun signature(t: DTypeParameter) =
        t.sourceSets.map {
            contentBuilder.contentFor(t, styles = t.stylesIfDeprecated(it), sourceSets = setOf(it)) {
                signatureForProjection(t.variantTypeParameter.withDri(t.dri.withTargetToDeclaration()))
                list(t.nontrivialBounds, prefix = " : ",
                surroundingCharactersStyle = mainStyles + TokenStyle.Operator) { bound ->
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
    ): Unit {
        return when (p) {
            is TypeParameter -> {
                annotationsInline(p)
                link(p.name, p.dri)
            }
            is FunctionalTypeConstructor -> {
                annotationsInline(p)
                +funType(mainDRI.single(), mainSourcesetData, p)
            }
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
