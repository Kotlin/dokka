package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.TypeWrapper
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.psi.JavaTypeWrapper
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinAsJavaPageContentBuilder(
    private val dri: Set<DRI>,
    private val platformData: Set<PlatformData>,
    private val kind: Kind,
    private val commentsConverter: CommentsToContentConverter,
    override val logger: DokkaLogger,
    private val styles: Set<Style> = emptySet(),
    private val extras: Set<Extra> = emptySet()
) : DefaultPageContentBuilder(dri, platformData, kind, commentsConverter, logger, styles, extras) {
    private val contents = mutableListOf<ContentNode>()

    override fun signature(f: Function) = signature(f) {

        val returnType = f.returnType
        if (!f.isConstructor) {
            if (returnType != null &&
                returnType.constructorFqName != Unit::class.qualifiedName
            ) {
                if ((returnType as? JavaTypeWrapper)?.isPrimitive == true)
                    text(returnType.constructorFqName ?: "")
                else
                    type(returnType)
                text(" ")
            } else text("void ")

        }

        link(f.name, f.dri)
        text("(")
        val params = listOfNotNull(f.receiver) + f.parameters
        list(params) {
            if ((it.type as? JavaTypeWrapper)?.isPrimitive == true)
                text(it.type.constructorFqName ?: "")
            else
                type(it.type)

            text(" ")
            link(it.name ?: "receiver", it.dri)
        }
        text(")")
    }

    companion object {
        fun group(
            dri: Set<DRI>,
            platformData: Set<PlatformData>,
            kind: Kind,
            commentsConverter: CommentsToContentConverter,
            logger: DokkaLogger,
            block: PageContentBuilderFunction
        ): ContentGroup =
            KotlinAsJavaPageContentBuilder(dri, platformData, kind, commentsConverter, logger).apply(block).build()
    }
}