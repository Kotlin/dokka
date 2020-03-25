package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.links.DriOfUnit
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.pages.TextStyle
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider {
    private val contentBuilder = PageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default, KotlinVisibility.Public)

    override fun signature(documentable: Documentable): ContentNode = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(e: DEnumEntry)= contentBuilder.contentFor(e, ContentKind.Symbol, setOf(TextStyle.Monospace))

    private fun signature(c: DClasslike) = contentBuilder.contentFor(c, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        platformText(c.visibility) { (it.takeIf { it !in ignoredVisibilities }?.name ?: "") + " " }
        if (c is DClass) {
            platformText(c.modifier){
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
        if(c is DClass){
            val pConstructor = c.constructors.singleOrNull() { it.extra[PrimaryConstructorExtra] != null }
            list(pConstructor?.parameters.orEmpty(), "(", ")", ",", pConstructor?.platformData.orEmpty().toSet()){
                breakLine()
                text(it.name ?: "", styles = mainStyles.plus(TextStyle.Bold).plus(TextStyle.Indented))
                text(": ")
                signatureForProjection(it.type)
            }
        }
        if (c is WithSupertypes) {
            c.supertypes.map { (p, dris) ->
                list(dris, prefix = " : ", platformData = setOf(p)) {
                    link(it.sureClassNames, it, platformData = setOf(p))
                }
            }
        }
    }

    private fun signature(p: DProperty) = contentBuilder.contentFor(p, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
        signatureForProjection(p.type)
    }

    private fun signature(f: DFunction) = contentBuilder.contentFor(f, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
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

    private fun signature(t: DTypeParameter) = contentBuilder.contentFor(t) {
        link(t.name, t.dri)
        list(t.bounds, prefix = " : ") {
            signatureForProjection(it)
        }
    }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
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

    fun funType(dri: DRI, platformData: Set<PlatformData>, type: TypeConstructor) =
        contentBuilder.contentFor(dri, platformData, ContentKind.Symbol, setOf(TextStyle.Monospace)) {
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
