package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.links.DriOfUnit
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default, KotlinVisibility.Public)

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

    private fun signature(e: DEnumEntry) = contentBuilder.contentFor(e, ContentKind.Symbol, setOf(TextStyle.Monospace))

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
            platformText(c.visibility, sourceSets) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
            if (c is DClass) {
                platformText(c.modifier, sourceSets) {
                    if (c.extra[AdditionalModifiers]?.content?.contains(ExtraModifiers.DATA) == true && it.name == "final") "data "
                    else it.name + " "
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
            if (c is DClass) {
                val pConstructor = c.constructors.singleOrNull { it.extra[PrimaryConstructorExtra] != null }
                list(pConstructor?.parameters.orEmpty(), "(", ")", ",", pConstructor?.sourceSets.orEmpty().toSet()) {
                    text(it.name ?: "", styles = mainStyles.plus(TextStyle.Bold).plus(TextStyle.Indented))
                    text(": ")
                    signatureForProjection(it.type)
                }
            }
            if (c is WithSupertypes) {
                c.supertypes.filter { it.key in sourceSets }.map { (p, dris) ->
                    list(dris, prefix = " : ", sourceSets = setOf(p)) {
                        link(it.sureClassNames, it, sourceSets = setOf(p))
                    }
                }
            }
        }

    private fun propertySignature(p: DProperty, sourceSets: Set<SourceSetData> = p.sourceSets.toSet()) =
        contentBuilder.contentFor(p, ContentKind.Symbol, setOf(TextStyle.Monospace), sourceSets = sourceSets) {
            platformText(p.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
            platformText(p.modifier){ it.name + " "}
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
            platformText(f.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
            platformText(f.modifier) { it.name + " " }
            text("fun ")
            list(f.generics, prefix = "<", suffix = "> ") {
                +buildSignature(it)
            }
            f.receiver?.also {
                signatureForProjection(it.type)
                text(".")
            }
            link(f.name, f.dri)
            text("(")
            list(f.parameters) {
                text(it.name!!)
                text(": ")

                signatureForProjection(it.type)
            }
            text(")")
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
                    platformText(t.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
                    text("typealias ")
                    signatureForProjection(t.type)
                    text(" = ")
                    signatureForProjection(type)
                }
            }
        }

    private fun signature(t: DTypeParameter) = contentBuilder.contentFor(t) {
        link(t.name, t.dri)
        list(t.bounds, prefix = " : ") {
            signatureForProjection(it)
        }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit =
        when (p) {
            is OtherParameter -> text(p.name)

            is TypeConstructor -> if (p.function)
                +funType(this.mainDRI, this.mainPlatformData, p)
            else
                group {
                    link(p.dri.classNames.orEmpty(), p.dri)
                    list(p.projections, prefix = "<", suffix = ">") {
                        signatureForProjection(it)
                    }
                }

            is Variance -> group {
                text(p.kind.toString() + " ")
                signatureForProjection(p.inner)
            }

            is Star -> text("*")

            is Nullable -> group {
                signatureForProjection(p.inner)
                text("?")
            }

            is JavaObject -> link("Any", DriOfAny)
            is Void -> link("Unit", DriOfUnit)
            is PrimitiveJavaType -> signatureForProjection(p.translateToKotlin())
        }

    private fun funType(dri: DRI, sourceSets: Set<SourceSetData>, type: TypeConstructor) =
        contentBuilder.contentFor(dri, sourceSets, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
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
