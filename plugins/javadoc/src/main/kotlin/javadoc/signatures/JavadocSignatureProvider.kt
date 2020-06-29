package javadoc.signatures

import javadoc.translators.documentables.JavadocPageContentBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.JvmSignatureUtils
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureUtils
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.utilities.DokkaLogger

class JavadocSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) : SignatureProvider,
    JvmSignatureUtils by JavaSignatureUtils {

    private val contentBuilder = JavadocPageContentBuilder(ctcc, this, logger)

    private val ignoredVisibilities = setOf(JavaVisibility.Default)

    private val ignoredModifiers =
        setOf(KotlinModifier.Open, JavaModifier.Empty, KotlinModifier.Empty, KotlinModifier.Sealed)

    override fun signature(documentable: Documentable): List<ContentNode> = when (documentable) {
        is DFunction -> signature(documentable)
        is DProperty -> signature(documentable)
        is DClasslike -> signature(documentable)
        is DEnumEntry -> signature(documentable)
        is DTypeParameter -> signature(documentable)
        is DParameter -> signature(documentable)
        else -> throw NotImplementedError(
            "Cannot generate signature for ${documentable::class.qualifiedName} ${documentable.name}"
        )
    }

    private fun signature(c: DClasslike): List<ContentNode> =
        javadocSignature(c) {
            annotations {
                annotationsBlock(c)
            }
            modifiers {
                text(c.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")

                if (c is DClass) {
                    text(c.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                    text(c.modifiers()[it]?.toSignatureString() ?: "")
                }

                when (c) {
                    is DClass -> text("class")
                    is DInterface -> text("interface")
                    is DEnum -> text("enum")
                    is DObject -> text("class")
                    is DAnnotation -> text("@interface")
                }
            }
            signatureWithoutModifiers {
                link(c.name!!, c.dri)
                if (c is WithGenerics) {
                    list(c.generics, prefix = "<", suffix = ">") {
                        +buildSignature(it)
                    }
                }
            }
            supertypes {
                if (c is WithSupertypes) {
                    c.supertypes.map { (p, dris) ->
                        list(dris, sourceSets = setOf(p)) {
                            link(it.fqName(), it, sourceSets = setOf(p))
                        }
                    }
                }
            }
        }

    private fun signature(f: DFunction): List<ContentNode> =
        javadocSignature(f) {
            annotations {
                annotationsBlock(f)
            }
            modifiers {
                text(f.modifier[it]?.takeIf { it !in ignoredModifiers }?.name?.plus(" ") ?: "")
                text(f.modifiers()[it]?.toSignatureString() ?: "")
                val returnType = f.type
                signatureForProjection(returnType)
            }
            signatureWithoutModifiers {
                link(f.name, f.dri)
                list(f.generics, prefix = "<", suffix = ">") {
                    +buildSignature(it)
                }
                text("(")
                list(f.parameters) {
                    annotationsInline(it)
                    text(it.modifiers()[it]?.toSignatureString() ?: "")
                    signatureForProjection(it.type)
                    text(Typography.nbsp.toString())
                    link(it.name!!, it.dri)
                }
                text(")")
            }
        }

    private fun signature(p: DProperty): List<ContentNode> =
        javadocSignature(p) {
            annotations {
                annotationsBlock(p)
            }
            modifiers {
                text(p.visibility[it]?.takeIf { it !in ignoredVisibilities }?.name?.plus(" ") ?: "")
                text(p.modifier[it]?.name + " ")
                text(p.modifiers()[it]?.toSignatureString() ?: "")
                signatureForProjection(p.type)
            }
            signatureWithoutModifiers {
                link(p.name, p.dri)
            }
        }

    private fun signature(e: DEnumEntry): List<ContentNode> =
        javadocSignature(e) {
            annotations {
                annotationsBlock(e)
            }
            modifiers {
                text(e.modifiers()[it]?.toSignatureString() ?: "")
            }
            signatureWithoutModifiers {
                link(e.name, e.dri)
            }
        }

    private fun signature(t: DTypeParameter): List<ContentNode> =
        javadocSignature(t) {
            annotations {
                annotationsBlock(t)
            }
            signatureWithoutModifiers {
                text(t.name)
            }
            supertypes {
                list(t.bounds, prefix = " extends ") {
                    signatureForProjection(it)
                }
            }
        }

    private fun signature(p: DParameter): List<ContentNode> =
        javadocSignature(p) {
            modifiers {
                signatureForProjection(p.type)
            }
            signatureWithoutModifiers {
                link(p.name.orEmpty(), p.dri)
            }
        }

    private fun javadocSignature(
        d: Documentable,
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty(),
        block: JavadocPageContentBuilder.JavadocContentBuilder.(DokkaConfiguration.DokkaSourceSet) -> Unit
    ): List<ContentNode> =
        d.sourceSets.map { sourceSet ->
            contentBuilder.contentFor(d, ContentKind.Main) {
                with(contentBuilder) {
                    javadocGroup(d.dri, d.sourceSets, extra) {
                        block(sourceSet)
                    }
                }
            }
        }

    private fun PageContentBuilder.DocumentableContentBuilder.signatureForProjection(p: Projection): Unit = when (p) {
        is OtherParameter -> link(p.name, p.declarationDRI)

        is TypeConstructor -> group(styles = emptySet()) {
            link(p.dri.fqName(), p.dri)
            list(p.projections, prefix = "<", suffix = ">") {
                signatureForProjection(it)
            }
        }

        is Variance -> group(styles = emptySet()) {
            text(p.kind.toString() + " ")
            signatureForProjection(p.inner)
        }

        is Star -> text("?")

        is Nullable -> signatureForProjection(p.inner)

        is JavaObject, is Dynamic -> link("java.lang.Object", DRI("java.lang", "Object"))
        is Void -> text("void")
        is PrimitiveJavaType -> text(p.name)
        is UnresolvedBound -> text(p.name)
    }

    private fun DRI.fqName(): String = "${packageName.orEmpty()}.${classNames.orEmpty()}"
}
