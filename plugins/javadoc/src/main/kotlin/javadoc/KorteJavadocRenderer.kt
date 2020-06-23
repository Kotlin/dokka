package javadoc

import com.soywiz.korte.*
import javadoc.pages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.ImplementedInterfaces
import org.jetbrains.dokka.model.InheritedFunction
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

typealias TemplateMap = Map<String, Any?>

class KorteJavadocRenderer(val outputWriter: OutputWriter, val context: DokkaContext, val resourceDir: String) :
    Renderer {
    private lateinit var locationProvider: JavadocLocationProvider

    override fun render(root: RootPageNode) = root.let { preprocessors.fold(root) { r, t -> t.invoke(r) } }.let { r ->
        locationProvider = JavadocLocationProvider(r, context)
        runBlocking(Dispatchers.IO) {
            renderModulePageNode(r as JavadocModulePageNode)
        }
    }

    private fun templateForNode(node: JavadocPageNode) = when (node) {
        is JavadocClasslikePageNode -> "class.korte"
        is JavadocPackagePageNode -> "tabPage.korte"
        is JavadocModulePageNode -> "tabPage.korte"
        is AllClassesPage -> "listPage.korte"
        is TreeViewPage -> "treePage.korte"
        else -> ""
    }

    private fun CoroutineScope.renderNode(node: PageNode, path: String = "") {
        if (node is JavadocPageNode) {
            renderJavadocNode(node)
        } else if (node is RendererSpecificPage) {
            renderSpecificPage(node, path)
        }
    }

    private fun CoroutineScope.renderModulePageNode(node: JavadocModulePageNode) {
        val link = "."
        val name = "index"
        val pathToRoot = ""

        val contentMap = mapOf<String, Any?>(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot,
            "kind" to "main",
        ) + renderJavadocContentNode(node.content)

        writeFromTemplate(outputWriter, "$link/$name".toNormalized(), "tabPage.korte", contentMap.toList())
        node.children.forEach { renderNode(it, link) }
    }

    private fun CoroutineScope.renderJavadocNode(node: JavadocPageNode) {
        val link = locationProvider.resolve(node, skipExtension = true)
        val dir = Paths.get(link).parent?.let { it.toNormalized() }.orEmpty()
        val pathToRoot = dir.split("/").filter { it.isNotEmpty() }.joinToString("/") { ".." }.let {
            if (it.isNotEmpty()) "$it/" else it
        }

        val contentMap = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot,
            "dir" to dir
        ) + renderContentNodes(node)
        writeFromTemplate(outputWriter, link, templateForNode(node), contentMap.toList())
        node.children.forEach { renderNode(it, link.toNormalized()) }
    }

    fun CoroutineScope.renderSpecificPage(node: RendererSpecificPage, path: String) = launch {
        when (val strategy = node.strategy) {
            is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, "")
            is RenderingStrategy.Write -> outputWriter.writeHtml(path, strategy.text)
            is RenderingStrategy.Callback -> outputWriter.writeResources(
                path,
                strategy.instructions(this@KorteJavadocRenderer, node)
            )
            RenderingStrategy.DoNothing -> Unit
        }
    }

    fun Pair<String, String>.pairToTag() =
        "\n<th class=\"colFirst\" scope=\"row\">${first}</th>\n<td class=\"colLast\">${second}</td>"

    fun LinkJavadocListEntry.toLinkTag(parent: String? = null) =
        createLinkTag(locationProvider.resolve(dri.first(), sourceSets.toList()).let {
            if (parent != null) it.relativizePath(parent)
            else it
        }, name)

    fun DRI.toLink(context: PageNode? = null) = locationProvider.resolve(this, emptyList(), context)

    fun createLinkTag(address: String, name: String) =
        address.let { if (it.endsWith(".html")) it else "$it.html" }.let {
            """<a href="$it">$name</a>"""
        }

    private fun String.parent() = Paths.get(this).parent.toNormalized()
    private fun Path.toNormalized() = this.normalize().toFile().toString()
    private fun String.toNormalized() = Paths.get(this).toNormalized()
    private fun String.relativizePath(parent: String) = Paths.get(parent).relativize(Paths.get(this)).toNormalized()

    private suspend fun OutputWriter.writeHtml(path: String, text: String) = write(path, text, ".html")
    private fun CoroutineScope.writeFromTemplate(
        writer: OutputWriter,
        path: String,
        template: String,
        args: List<Pair<String, *>>
    ) = launch {
        val tmp = templateRenderer.render(template, *(args.toTypedArray()))
        writer.writeHtml(path, tmp)
    }


    private fun htmlForContentNodes(content: List<ContentNode>): String =
        content.joinToString("") { htmlForContentNode(it) }

    fun getTemplateConfig() = TemplateConfig().also { config ->
        listOf(
            TeFunction("curDate") { LocalDate.now() },
            TeFunction("jQueryVersion") { "3.1" },
            TeFunction("jQueryMigrateVersion") { "1.2.1" },
            TeFunction("rowColor") { args -> if ((args.first() as Int) % 2 == 0) "altColor" else "rowColor" },
            TeFunction("h1Title") { args -> if ((args.first() as? String) == "package") "title=\"Package\" " else "" },
            TeFunction("createTabRow") { args ->
                val (link, doc) = args.first() as RowJavadocListEntry
                val dir = args[1] as String?
                (createLinkTag(
                    locationProvider.resolve(link, dir.orEmpty()),
                    link.name
                ) to htmlForContentNodes(doc)).pairToTag().trim()
            },
            TeFunction("createListRow") { args ->
                val link = args.first() as LinkJavadocListEntry
                val dir = args[1] as String?
//                link.toLinkTag(dir)
                createLinkTag(locationProvider.resolve(link, dir.orEmpty()), link.name)
            },
            TeFunction("createPackageHierarchy") { args ->
                val list = args.first() as List<JavadocPackagePageNode>
                list.mapIndexed { i, p ->
                    val content = if (i + 1 == list.size) "" else ", "
                    val name = p.name
                    "<li><a href=\"$name/package-tree.html\">$name</a>$content</li>"
                }.joinToString("\n")
            },
            TeFunction("renderInheritanceGraph") { args ->
                val rootNodes = args.first() as List<TreeViewPage.InheritanceNode>

                fun drawRec(node: TreeViewPage.InheritanceNode): String {
                    val returnValue = "<li class=\"circle\">" + node.dri.let { dri ->
                        listOfNotNull(
                            dri.packageName,
                            dri.classNames
                        ).joinToString(".") + node.interfaces.takeUnless { node.isInterface || it.isEmpty() }
                            ?.let {
                                " implements " + it.joinToString(", ") { n ->
                                    listOfNotNull(
                                        n.packageName,
                                        createLinkTag(n.toLink(), n.classNames.orEmpty())
                                    ).joinToString(".")
                                }
                            }.orEmpty()
                    } + node.children.filterNot { it.isInterface }.takeUnless { it.isEmpty() }?.let {
                        "<ul>" + it.joinToString("\n", transform = ::drawRec) + "</ul>"
                    }.orEmpty() + "</li>"
                    return returnValue
                }
                rootNodes.joinToString { drawRec(it) }
            },
            Filter("length") { subject.dynamicLength() },
            TeFunction("hasAnyDescription") { args ->
                args.first().safeAs<List<HashMap<String, String>>>()
                    ?.any { it["description"]?.trim()?.isNotEmpty() ?: false }
            }
        ).forEach {
            when (it) {
                is TeFunction -> config.register(it)
                is Filter -> config.register(it)
                is Tag -> config.register(it)
            }
        }
    }

    val config = getTemplateConfig()
    val templateRenderer = Templates(ResourceTemplateProvider(resourceDir), config = config, cache = true)

    class ResourceTemplateProvider(val basePath: String) : TemplateProvider {
        override suspend fun get(template: String): String =
            javaClass.classLoader.getResourceAsStream("$basePath/$template")?.bufferedReader()?.lines()?.toArray()
                ?.joinToString("\n") ?: throw IllegalStateException("Template not found: $basePath/$template")
    }

    private fun renderContentNodes(node: JavadocPageNode): TemplateMap =
        when (node) {
            is JavadocClasslikePageNode -> renderClasslikeNode(node)
            is JavadocFunctionNode -> renderFunctionNode(node)
            is JavadocPackagePageNode -> renderPackagePageNode(node)
            is TreeViewPage -> renderTreeViewPage(node)
            is AllClassesPage -> renderAllClassesPage(node)
            else -> emptyMap()
        }


    private fun renderAllClassesPage(node: AllClassesPage): TemplateMap {
        return mapOf(
            "title" to "All Classes",
            "list" to node.classEntries
        )
    }

    private fun renderTreeViewPage(node: TreeViewPage): TemplateMap {
        return mapOf(
            "title" to node.title,
            "name" to node.name,
            "kind" to node.kind,
            "list" to node.packages.orEmpty() + node.classes.orEmpty(),
            "classGraph" to node.classGraph,
            "interfaceGraph" to node.interfaceGraph
        )
    }

    private fun renderPackagePageNode(node: JavadocPackagePageNode): TemplateMap {
        return mapOf(
            "kind" to "package"
        ) + renderJavadocContentNode(node.content)
    }

    private fun renderFunctionNode(node: JavadocFunctionNode): TemplateMap {
        val (modifiers, signature) = node.modifiersAndSignature
        return mapOf(
            "signature" to htmlForContentNode(node.signature),
            "brief" to htmlForContentNode(node.brief),
            "parameters" to node.parameters.map { renderParameterNode(it) },
            "inlineParameters" to node.parameters.joinToString { "${it.type} ${it.name}" },
            "modifiers" to htmlForContentNode(modifiers),
            "signatureWithoutModifiers" to htmlForContentNode(signature),
            "name" to node.name
        )
    }

    private fun renderParameterNode(node: JavadocParameterNode): TemplateMap =
        mapOf(
            "description" to htmlForContentNode(node.description),
            "name" to node.name,
            "type" to node.type
        )

    private fun renderClasslikeNode(node: JavadocClasslikePageNode): TemplateMap =
        mapOf(
            "constructors" to node.constructors.map { renderContentNodes(it) },
            "signature" to htmlForContentNode(node.signature),
            "methods" to renderClasslikeMethods(node.methods),
            "entries" to node.entries.map { renderEntryNode(it) },
            "properties" to node.properties.map { renderPropertyNode(it) },
            "classlikes" to node.classlikes.map { renderNestedClasslikeNode(it) },
            "implementedInterfaces" to renderImplementedInterfaces(node),
            "kind" to node.kind,
            "packageName" to node.packageName
        ) + renderJavadocContentNode(node.content)

    private fun renderImplementedInterfaces(node: JavadocClasslikePageNode) =
        node.extras[ImplementedInterfaces]?.interfaces?.entries?.firstOrNull { it.key.platform == Platform.jvm }?.value?.map { it.displayable() } // TODO: REMOVE HARDCODED JVM DEPENDENCY
            .orEmpty()

    private fun renderClasslikeMethods(nodes: List<JavadocFunctionNode>): TemplateMap {
        val (inherited, own) = nodes.partition { it.extras[InheritedFunction]?.inheritedFrom?.any {
            it.key.platform == Platform.jvm // TODO: REMOVE HARDCODED JVM DEPENDENCY
        } ?: false }
        return mapOf(
            "own" to own.map { renderContentNodes(it) },
            "inherited" to inherited.map { renderInheritedMethod(it) }
                .groupBy { it["inheritedFrom"] as String }.entries.map {
                    mapOf(
                        "inheritedFrom" to it.key,
                        "names" to it.value.map { it["name"] as String }.sorted().joinToString()
                    )
                }
        )
    }

    private fun renderInheritedMethod(node: JavadocFunctionNode): TemplateMap {
        val inheritedFrom = node.extras[InheritedFunction]?.inheritedFrom
        return mapOf(
            "inheritedFrom" to inheritedFrom?.entries?.firstOrNull { it.key.platform == Platform.jvm }?.value?.displayable() // TODO: REMOVE HARDCODED JVM DEPENDENCY
                .orEmpty(),
            "name" to node.name
        )
    }

    private fun renderNestedClasslikeNode(node: JavadocClasslikePageNode): TemplateMap {
        return mapOf(
            "modifiers" to (node.modifiers + "static" + node.kind).joinToString(separator = " "),
            "signature" to node.name,
            "description" to node.description
        )
    }

    private fun renderPropertyNode(node: JavadocPropertyNode): TemplateMap {
        val (modifiers, signature) = node.modifiersAndSignature
        return mapOf(
            "modifiers" to htmlForContentNode(modifiers),
            "signature" to htmlForContentNode(signature),
            "description" to htmlForContentNode(node.brief)
        )
    }

    private fun renderEntryNode(node: JavadocEntryNode): TemplateMap {
        return mapOf(
            "signature" to htmlForContentNode(node.signature),
            "brief" to node.brief
        )
    }


    //TODO is it possible to use html renderer?
    private fun htmlForContentNode(node: ContentNode): String =
        when (node) {
            is ContentGroup -> node.children.joinToString(separator = "") { htmlForContentNode(it) }
            is ContentText -> node.text
            is TextNode -> node.text
            is ContentLink -> """<a href="TODO">${node.children.joinToString { htmlForContentNode(it) }} </a>"""
            else -> ""
        }

    private fun renderJavadocContentNode(node: JavadocContentNode): TemplateMap = when (node) {
        is TitleNode -> renderTitleNode(node)
        is JavadocContentGroup -> renderJavadocContentGroup(node)
        is TextNode -> renderTextNode(node)
        is ListNode -> renderListNode(node)
        else -> emptyMap()
    }

    private fun renderTitleNode(node: TitleNode): TemplateMap {
        return mapOf(
            "title" to node.title,
            "version" to node.version,
            "packageName" to node.parent
        )
    }

    private fun renderJavadocContentGroup(note: JavadocContentGroup): TemplateMap {
        return note.children.fold(emptyMap<String, Any?>()) { map, child ->
            map + renderJavadocContentNode(child)
        }
    }

    private fun renderTextNode(node: TextNode): TemplateMap {
        return mapOf("text" to node.text)
    }

    private fun renderListNode(node: ListNode): TemplateMap {
        return mapOf(
            "tabTitle" to node.tabTitle,
            "colTitle" to node.colTitle,
            "list" to node.children
        )
    }

    private fun DRI.displayable(): String = "${packageName}.${sureClassNames}"
}