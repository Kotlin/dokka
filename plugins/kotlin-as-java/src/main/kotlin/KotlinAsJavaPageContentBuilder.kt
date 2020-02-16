package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.base.transformers.documentables.DefaultPageContentBuilder
import org.jetbrains.dokka.base.transformers.documentables.PageContentBuilderFunction
import org.jetbrains.dokka.base.transformers.documentables.type
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.JavaTypeWrapper
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

class KotlinAsJavaPageContentBuilder(
    dri: Set<DRI>,
    platformData: Set<PlatformData>,
    kind: Kind,
    commentsConverter: CommentsToContentConverter,
    logger: DokkaLogger,
    styles: Set<Style> = emptySet(),
    extras: Set<Extra> = emptySet()
) : DefaultPageContentBuilder(dri, platformData, kind, commentsConverter, logger, styles, extras) {

    override fun signature(f: Function) = signature(f) {

        //        DokkaConsoleLogger.info("KotlinAsJavaSignature")
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

    override fun group(
        dri: Set<DRI>,
        platformData: Set<PlatformData>,
        kind: Kind,
        block: PageContentBuilderFunction
    ): ContentGroup = group(dri, platformData, kind, commentsConverter, logger, block)

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