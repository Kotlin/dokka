package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.pages.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe


class DefaultDocumentationToPageTransformer: DocumentationToPageTransformer {
    override fun transform(modules: Collection<Pair<DokkaConfiguration.PassConfiguration, Module>>): PageNode {
        val module = modules.first().second // TODO only one module for starters
        val platformData = modules.first().first.targets.map { PlatformData(it, modules.first().first.analysisPlatform) }
        return PageBuilder(platformData).pageForModule(module)
    }

    class PageBuilder(private val platformData: List<PlatformData>) {
        fun pageForModule(m: Module) =
            ModulePageNode("root", contentForModule(m), documentationNode = m).apply {
                // TODO change name
                appendChildren(m.packages.map { pageForPackage(it, this) })
            }

        private fun pageForPackage(p: Package, parent: PageNode) =
            PackagePageNode(p.name, contentForPackage(p), parent, p.dri, p).apply {
                appendChildren(p.classes.map { pageForClass(it, this) })
                appendChildren(p.functions.map { pageForMember(it, this) })
                appendChildren(p.properties.map { pageForMember(it, this) })
            }

        private fun pageForClass(c: Class, parent: PageNode): ClassPageNode =
            ClassPageNode(c.name, contentForClass(c), parent, c.dri, c).apply {
                // TODO: Pages for constructors
                appendChildren(c.classes.map { pageForClass(it, this) })
                appendChildren(c.functions.map { pageForMember(it, this) })
                appendChildren(c.properties.map { pageForMember(it, this) })
            }

        private fun pageForMember(m: CallableNode<*>, parent: PageNode): MemberPageNode =
            when (m) {
                is Function -> MemberPageNode(m.name, contentForFunction(m), parent, m.dri, m)
                is Property -> MemberPageNode(m.name, emptyList(), parent, m.dri, m)
                else -> throw IllegalStateException("$m should not be present here")
            }

        private fun contentForModule(m: Module) = listOf(
            ContentHeader(listOf(ContentText("root", platformData)), 1, platformData),
            ContentBlock("Packages", m.packages.map { ContentLink(it.name, it.dri, platformData) }, platformData),
            ContentText("Index", platformData),
            ContentText("Link to allpage here", platformData)
        )

        private fun contentForPackage(p: Package) = listOf(
            ContentHeader(listOf(ContentText("Package ${p.name}", platformData)), 1, platformData),
            ContentBlock("Types", p.classes.map { ContentGroup(
                listOf(
                    ContentLink(it.name, it.dri, platformData),
                    ContentText(it.briefDocstring, platformData),
                ContentText("signature for class", platformData)
                ), platformData)
            }, platformData),
            ContentBlock("Functions", p.functions.map { ContentGroup(
                listOf(
                    ContentLink(it.name, it.dri, platformData),
                    ContentText(it.briefDocstring, platformData),
                    ContentText("signature for function", platformData)
                ), platformData)
            }, platformData)
        )

        private fun contentForClass(c: Class) = listOf(
            ContentHeader(listOf(ContentText(c.name, platformData)), 1, platformData),
            ContentText(c.rawDocstring, platformData),
            ContentBlock("Constructors", c.descriptor.constructors.map { ContentGroup(
                listOf(
                    ContentLink(it.fqNameSafe.asString(), c.dri.copy(callable = Callable(it.fqNameSafe.asString() /* TODO: identifier for filename here */, "", "", it.valueParameters.map {it.fqNameSafe.asString()})), platformData),
                    ContentText("message to Pawel from the future: you forgot about extracting constructors, didn't you?", platformData),
                    ContentText("signature for constructor", platformData)
                ), platformData)
            }, platformData),
            ContentBlock("Functions", c.functions.map { ContentGroup(
                listOf(
                    ContentLink(it.name, it.dri, platformData),
                    ContentText(it.briefDocstring, platformData),
                    ContentText("signature for function", platformData)
                ), platformData)
            }, platformData)
        )

        private fun contentForFunction(f: Function) = listOf(
            ContentHeader(listOf(ContentText(f.name, platformData)), 1, platformData),
            ContentText("signature for function", platformData),
            ContentText(f.rawDocstring, platformData),
            ContentBlock("Parameters", f.parameters.map { ContentGroup(
                listOf(
                    ContentText(it.name ?: "?", platformData),
                    ContentText(it.rawDocstring, platformData)
                ), platformData)
            }, platformData)
        )
    }
}

fun DocumentationNode<*>.identifier(platformData: List<PlatformData>): List<ContentNode> {
//    when(this) {
//        is Class -> ContentText(this.descriptor.toString(), platforms), ContentText("(") this.properties.map { ContentText(it.descriptor.visibility + " " + it.descriptor.name + ":" + ),}
//        is Function ->
//        is Property ->
//        else -> return emptyList()
//    }
    TODO()
}
// take this ^ from old dokka
/*
pages are equal if the content and the children are equal
we then can merge the content by merging the platforms
and take an arbitrary set of the children
but we need to recursively process all of the children anyway
 */