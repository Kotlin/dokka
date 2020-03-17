package javadoc

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.pages.*

class JavadocPageBuilder(val rootContentGroup: (Documentable, Kind, PageContentBuilderFunction) -> ContentGroup) {
    fun pageForModule(m: Module): ModulePageNode =
        ModulePageNode(m.name.ifEmpty { "root" }, contentForModule(m), m, m.packages.map { pageForPackage(it) })

    fun pageForPackage(p: Package) =
        PackagePageNode(p.name, contentForPackage(p), setOf(p.dri), p,
            p.classlikes.map { pageForClasslike(it) } // TODO: nested classlikes
        )

    fun pageForClasslike(c: Classlike): ClasslikePageNode {
        val constructors = when (c) {
            is Class -> c.constructors
            is Enum -> c.constructors
            else -> emptyList()
        }

        return ClasslikePageNode(c.name, contentForClasslike(c), setOf(c.dri), c, emptyList())
    }

    fun pageForMember(m: CallableNode): MemberPageNode =
        throw IllegalStateException("$m should not be present here")

    fun group(node: Documentable, content: JavadocPageContentBuilder.() -> Unit) =
        rootContentGroup(node, ContentKind.Main, content)

    fun contentForModule(m: Module): ContentNode =
        group(m) {

        }

    fun contentForPackage(p: Package): ContentNode {

    }

    fun contentForClasslike(c: Classlike): ContentNode {

    }
}

class JavadocPageContentBuilder {

}