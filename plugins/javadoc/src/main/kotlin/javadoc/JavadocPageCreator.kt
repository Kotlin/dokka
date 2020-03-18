package javadoc

import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaLogger

open class JavadocPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) {
    fun pageForModule(m: DModule): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    fun pageForPackage(p: DPackage) =
        PackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.map { pageForClasslike(it) } // TODO: nested classlikes
        )

    fun pageForClasslike(c: DClasslike): ClasslikePageNode {
        val constructors = when (c) {
            is DClass -> c.constructors
            is DEnum -> c.constructors
            else -> emptyList()
        }

        return ClasslikePageNode(c.name.orEmpty(), contentForClasslike(c), setOf(c.dri), c, emptyList())
    }

    fun pageForMember(m: Callable): MemberPageNode =
        throw IllegalStateException("$m should not be present here")


    fun contentForModule(m: DModule): ContentNode {}

    fun contentForPackage(p: DPackage): ContentNode {

    }

    fun contentForClasslike(c: DClasslike): ContentNode {

    }
}

