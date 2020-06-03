package org.jetbrains.dokka.base.signatures

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
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.text.Typography.nbsp

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider,
    JvmSignatureUtils by KotlinSignatureUtils {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Public, KotlinVisibility.Public)
    private val ignoredModifiers = setOf(JavaModifier.Final, KotlinModifier.Final)

    override fun signature(documentable: Documentable): ContentNode = when (documentable) {
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

    private fun signature(e: DEnumEntry) =
        contentBuilder.contentFor(e, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
            link(e.name, e.dri)
        }

    private fun actualTypealiasedSignature(dri: DRI, name: String, aliasedTypes: SourceSetDependent<Bound>) =
        aliasedTypes.entries.groupBy({ it.value }, { it.key }).map { (bound, platforms) ->
            contentBuilder.contentFor(dri, platforms.toSet(), ContentKind.Symbol, setOf(TextStyle.Monospace)) {
                text("actual typealias ")
                link(name, dri)
                text(" = ")
                signatureForProjection(bound)
            }
        }

    private fun <T : DClasslike> classlikeSignature(c: T) =
        (c as? WithExtraProperties<out DClasslike>)?.let {
            c.extra[ActualTypealias]?.let {
                contentBuilder.contentFor(c) {
                    +regularSignature(c, sourceSets = c.sourceSets.toSet() - it.underlyingType.keys)
                    +actualTypealiasedSignature(c.dri, c.name.orEmpty(), it.underlyingType)
                }
            } ?: regularSignature(c)
        } ?: regularSignature(c)

    private fun regularSignature(c: DClasslike, sourceSets: Set<SourceSetData> = c.sourceSets.toSet()) =
        contentBuilder.contentFor(c, ContentKind.Symbol, setOf(TextStyle.Monospace), sourceSets = sourceSets) {
            annotationsBlock(c)
            platformText(
                c.visibility,
                sourceSets
            ) { it.takeIf { it !in ignoredVisibilities }?.name?.let { "$it " } ?: "" }
            if (c is DClass) {
                platformText(c.modifier, sourceSets) {
                    if (it !in ignoredModifiers)
                        if (c.extra[AdditionalModifiers]?.content?.contains(ExtraModifiers.KotlinOnlyModifiers.Data) == true) ""
                        else (if (it is JavaModifier.Empty) KotlinModifier.Open else it).let { it.name + " " }
                    else
                        ""
                }
            }
            when (c) {
                is DClass -> text("class ")
                is DInterface -> text("interface ")
                is DEnum -> text("enum ")
                is DObject -> text("object ")
                is DAnnotation -> text("annotation class ")
            }
            link(c.name!!, c.dri)
            if (c is WithGenerics) {
                list(c.generics, prefix = "<", suffix = "> ") {
                    +buildSignature(it)
                }
            }
            if (c is DClass) {
                val pConstructor = c.constructors.singleOrNull { it.extra[PrimaryConstructorExtra] != null }
                if (pConstructor?.annotations()?.isNotEmpty() == true) {
                    text(nbsp.toString())
                    annotationsInline(pConstructor)
                    text("constructor")
                }
                list(
                    pConstructor?.parameters.orEmpty(),
                    "(",
                    ")",
                    ",",
                    pConstructor?.sourceSets.orEmpty().toSet()
                ) {
                    annotationsInline(it)
                    text(it.name ?: "", styles = mainStyles.plus(TextStyle.Bold))
                    text(": ")
                    signatureForProjection(it.type)
                }
            }
            if (c is WithSupertypes) {
                c.supertypes.filter { it.key in sourceSets }.map { (s, dris) ->
                    list(dris, prefix = " : ", sourceSets = setOf(s)) {
                        link(it.sureClassNames, it, sourceSets = setOf(s))
                    }
                }
            }
        }


    private fun propertySignature(p: DProperty, sourceSets: Set<SourceSetData> = p.sourceSets.toSet()) =
        contentBuilder.contentFor(p, ContentKind.Symbol, setOf(TextStyle.Monospace), sourceSets = sourceSets) {
            annotationsBlock(p)
            platformText(p.visibility) { it.takeIf { it !in ignoredVisibilities }?.name?.let { "$it " } ?: "" }
            platformText(p.modifier) {
                it.takeIf { it !in ignoredModifiers }?.let {
                    if (it is JavaModifier.Empty) KotlinModifier.Open else it
                }?.name?.let { "$it " } ?: ""
            }
            platformText(p.modifiers()) { it.toSignatureString() }
            p.setter?.let { text("var ") } ?: text("val ")
            list(p.generics, prefix = "<", suffix = "> ") {
                +buildSignature(it)
            }
            p.receiver?.also {
                signatureForProjection(it.type)
                text(".")
            }
            link(p.name, p.dri)
            text(": ")
            signatureForProjection(p.type)
        }

    private fun functionSignature(f: DFunction, sourceSets: Set<SourceSetData> = f.sourceSets.toSet()) =
        contentBuilder.contentFor(f, ContentKind.Symbol, setOf(TextStyle.Monospace), sourceSets = sourceSets) {
            annotationsBlock(f)
            platformText(f.visibility) { it.takeIf { it !in ignoredVisibilities }?.name?.let { "$it " } ?: "" }
            platformText(f.modifier) {
                it.takeIf { it !in ignoredModifiers }?.let {
                    if (it is JavaModifier.Empty) KotlinModifier.Open else it
                }?.name?.let { "$it " } ?: ""
            }
            platformText(f.modifiers()) { it.toSignatureString() }
            text("fun ")
            list(f.generics, prefix = "<", suffix = "> ") {
                +buildSignature(it)
            }
            f.receiver?.also {
                signatureForProjection(it.type)
                text(".")
            }
            link(f.name, f.dri)
            list(f.parameters, "(", ")") {
                annotationsInline(it)
                platformText(it.modifiers()) { it.toSignatureString() }
                text(it.name!!)
                text(": ")
                signatureForProjection(it.type)
            }
            if (f.documentReturnType()) {
                text(": ")
                signatureForProjection(f.type)
            }
        }

    private fun DFunction.documentReturnType() = when {
        this.isConstructor -> false
        this.type is TypeConstructor && (this.type as TypeConstructor).dri == DriOfUnit -> false
        this.type is Void -> false
        else -> true
    }

    private fun signature(t: DTypeAlias) =
        contentBuilder.contentFor(t) {
            t.underlyingType.entries.groupBy({ it.value }, { it.key }).map { (type, platforms) ->
                +contentBuilder.contentFor(
                    t,
                    ContentKind.Symbol,
                    setOf(TextStyle.Monospace),
                    sourceSets = platforms.toSet()
                ) {
                    platformText(t.visibility) { it.takeIf { it !in ignoredVisibilities }?.name?.let { "$it " } ?: "" }
                    platformText(t.modifiers()) { it.toSignatureString() }
                    text("typealias ")
                    signatureForProjection(t.type)
                    text(" = ")
                    signatureForProjection(type)
                }
            }
        }

    private fun signature(t: DTypeParameter) = contentBuilder.contentFor(t) {
        link(t.name, t.dri.withTargetToDeclaration())
        list(t.bounds, prefix = " : ") {
            signatureForProjection(it)
        }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit =
        when (p) {
            is OtherParameter -> link(p.name, p.declarationDRI)

            is TypeConstructor -> if (p.function)
                +funType(mainDRI.single(), mainPlatformData, p)
            else
                group(styles = emptySet()) {
                    link(p.dri.classNames.orEmpty(), p.dri)
                    list(p.projections, prefix = "<", suffix = ">") {
                        signatureForProjection(it)
                    }
                }

            is Variance -> group(styles = emptySet()) {
                text(p.kind.toString() + " ")
                signatureForProjection(p.inner)
            }

            is Star -> text("*")

            is Nullable -> group(styles = emptySet()) {
                signatureForProjection(p.inner)
                text("?")
            }

            is JavaObject -> link("Any", DriOfAny)
            is Void -> link("Unit", DriOfUnit)
            is PrimitiveJavaType -> signatureForProjection(p.translateToKotlin())
            is Dynamic -> text("dynamic")
        }

    private fun funType(dri: DRI, sourceSets: Set<SourceSetData>, type: TypeConstructor) =
        contentBuilder.contentFor(dri, sourceSets, ContentKind.Main) {
            if (type.extension) {
                signatureForProjection(type.projections.first())
                text(".")
            }

            val args = if (type.extension)
                type.projections.drop(1)
            else
                type.projections

            text("(")
            args.subList(0, args.size - 1).forEachIndexed { i, arg ->
                signatureForProjection(arg)
                if (i < args.size - 2) text(", ")
            }
            text(") -> ")
            signatureForProjection(args.last())
        }
}

private fun PrimitiveJavaType.translateToKotlin() = TypeConstructor(
    dri = DRI("kotlin", name.capitalize()),
    projections = emptyList()
)

val TypeConstructor.function
    get() = modifier == FunctionModifiers.FUNCTION || modifier == FunctionModifiers.EXTENSION

val TypeConstructor.extension
    get() = modifier == FunctionModifiers.EXTENSION
