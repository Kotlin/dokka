package javadoc.renderer

import com.soywiz.korte.*
import javadoc.JavadocLocationProvider
import javadoc.pages.*
import javadoc.renderer.JavadocContentToHtmlTranslator.Companion.buildLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.links.DRI
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
        is JavadocModulePageNode,
        is JavadocPackagePageNode -> "tabPage.korte"
        is JavadocClasslikePageNode -> "class.korte"
        is AllClassesPage -> "listPage.korte"
        is TreeViewPage -> "treePage.korte"
        else -> ""
    }

    private fun CoroutineScope.renderNode(node: PageNode, path: String = "") {
        if (node is JavadocPageNode) {
            renderJavadocPageNode(node)
        } else if (node is RendererSpecificPage) {
            renderSpecificPage(node, path)
        }
    }

    private fun CoroutineScope.renderModulePageNode(node: JavadocModulePageNode) {
        val link = "."
        val name = "index"
        val pathToRoot = ""

        val contentMap = JavadocContentToTemplateMapTranslator(locationProvider, context).templateMapForPageNode(node, pathToRoot)

        writeFromTemplate(outputWriter, "$link/$name".toNormalized(), "tabPage.korte", contentMap.toList())
        node.children.forEach { renderNode(it, link) }
    }

    private fun CoroutineScope.renderJavadocPageNode(node: JavadocPageNode) {
        val link = locationProvider.resolve(node, skipExtension = true)
        val dir = Paths.get(link).parent?.let { it.toNormalized() }.orEmpty()
        val pathToRoot = dir.split("/").filter { it.isNotEmpty() }.joinToString("/") { ".." }.let {
            if (it.isNotEmpty()) "$it/" else it
        }

        val contentMap = JavadocContentToTemplateMapTranslator(locationProvider, context).templateMapForPageNode(node, pathToRoot)
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
        """<th class="colFirst" scope="row">${first}</th>\n<td class="colLast">${second}</td>"""

    fun DRI.toLink(context: PageNode? = null) = locationProvider.resolve(this, emptySet(), context)

    private fun Path.toNormalized() = this.normalize().toFile().toString()
    private fun String.toNormalized() = Paths.get(this).toNormalized()

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
                val translator = JavadocContentToHtmlTranslator(locationProvider, context)
                (buildLink(
                    locationProvider.resolve(link, dir.orEmpty()),
                    link.name
                ) to translator.htmlForContentNodes(doc, dir)).pairToTag().trim()
            },
            TeFunction("createListRow") { args ->
                val link = args.first() as LinkJavadocListEntry
                val dir = args[1] as String?
                buildLink(
                    locationProvider.resolve(link, dir.orEmpty()),
                    link.name
                )
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

                fun drawRec(node: TreeViewPage.InheritanceNode): String =
                    "<li class=\"circle\">" + node.dri.let { dri ->
                        listOfNotNull(
                            dri.packageName,
                            dri.classNames
                        ).joinToString(".") + node.interfaces.takeUnless { node.isInterface || it.isEmpty() }
                            ?.let {
                                " implements " + it.joinToString(", ") { n ->
                                    listOfNotNull(
                                        n.packageName,
                                        buildLink(n.toLink(), n.classNames.orEmpty())
                                    ).joinToString(".")
                                }
                            }.orEmpty()
                    } + node.children.filterNot { it.isInterface }.takeUnless { it.isEmpty() }?.let {
                        "<ul>" + it.joinToString("\n", transform = ::drawRec) + "</ul>"
                    }.orEmpty() + "</li>"

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
    val templateRenderer = Templates(
        ResourceTemplateProvider(
            resourceDir
        ), config = config, cache = true)

    class ResourceTemplateProvider(val basePath: String) : TemplateProvider {
        override suspend fun get(template: String): String =
            javaClass.classLoader.getResourceAsStream("$basePath/$template")?.bufferedReader()?.lines()?.toArray()
                ?.joinToString("\n") ?: throw IllegalStateException("Template not found: $basePath/$template")
    }

}