package org.jetbrains.dokka.javadoc.renderer

import com.soywiz.korte.*
import org.jetbrains.dokka.javadoc.location.JavadocLocationProvider
import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.javadoc.renderer.JavadocContentToHtmlTranslator.Companion.buildLink
import org.jetbrains.dokka.javadoc.toNormalized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.time.LocalDate

typealias TemplateMap = Map<String, Any?>

class KorteJavadocRenderer(private val outputWriter: OutputWriter, val context: DokkaContext, resourceDir: String) :
    Renderer {
    private lateinit var locationProvider: JavadocLocationProvider
    private val registeredPreprocessors = context.plugin<JavadocPlugin>().query { javadocPreprocessors }

    private val contentToHtmlTranslator by lazy {
        JavadocContentToHtmlTranslator(locationProvider, context)
    }

    private val contentToTemplateMapTranslator by lazy {
        JavadocContentToTemplateMapTranslator(locationProvider, context)
    }

    override fun render(root: RootPageNode) = root.let { registeredPreprocessors.fold(root) { r, t -> t.invoke(r) } }.let { newRoot ->
        locationProvider = context.plugin<JavadocPlugin>().querySingle { locationProviderFactory }.getLocationProvider(newRoot) as JavadocLocationProvider
        runBlocking(Dispatchers.IO) {
            renderPage(newRoot)
            SearchScriptsCreator(locationProvider).invoke(newRoot).forEach { renderSpecificPage(it, "") }
        }
    }

    private fun templateForNode(node: JavadocPageNode) = when (node) {
        is JavadocModulePageNode,
        is JavadocPackagePageNode -> "tabPage.korte"
        is JavadocClasslikePageNode -> "class.korte"
        is AllClassesPage -> "listPage.korte"
        is TreeViewPage -> "treePage.korte"
        is IndexPage -> "indexPage.korte"
        is DeprecatedPage -> "deprecated.korte"
        else -> ""
    }

    private fun CoroutineScope.renderPage(node: PageNode, path: String = "") {
        when(node){
            is JavadocModulePageNode -> renderModulePageNode(node)
            is JavadocPageNode -> renderJavadocPageNode(node)
            is RendererSpecificPage -> renderSpecificPage(node, path)
        }
    }

    private fun CoroutineScope.renderModulePageNode(node: JavadocModulePageNode) {
        val link = "."
        val name = "index"

        val contentMap = contentToTemplateMapTranslator.templateMapForPageNode(node)

        writeFromTemplate(outputWriter, "$link/$name".toNormalized(), "tabPage.korte", contentMap.toList())
        node.children.forEach { renderPage(it, link) }
    }

    private fun CoroutineScope.renderJavadocPageNode(node: JavadocPageNode) {
        val link = locationProvider.resolve(node, skipExtension = true)
        val contentMap = contentToTemplateMapTranslator.templateMapForPageNode(node)
        writeFromTemplate(outputWriter, link, templateForNode(node), contentMap.toList())
        node.children.forEach { renderPage(it, link.toNormalized()) }
    }

    private fun CoroutineScope.renderSpecificPage(node: RendererSpecificPage, path: String) = launch {
        when (val strategy = node.strategy) {
            is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, "")
            is RenderingStrategy.Write -> outputWriter.writeHtml(node.name, strategy.text)
            is RenderingStrategy.Callback -> outputWriter.writeResources(
                path,
                strategy.instructions(this@KorteJavadocRenderer, node)
            )
            RenderingStrategy.DoNothing -> Unit
        }
        node.children.forEach { renderPage(it, locationProvider.resolve(node, skipExtension = true).toNormalized()) }
    }

    private fun Pair<String, String>.pairToTag() =
        """<th class="colFirst" scope="row">${first}</th><td class="colLast">${second}</td>"""

    private fun DRI.toLink(context: PageNode? = null) = locationProvider.resolve(this, emptySet(), context)

    private suspend fun OutputWriter.writeHtml(path: String, text: String) = write(path, text, "")
    private fun CoroutineScope.writeFromTemplate(
        writer: OutputWriter,
        path: String,
        template: String,
        args: List<Pair<String, *>>
    ) = launch {
        val tmp = templateRenderer.render(template, *(args.toTypedArray()))
        writer.writeHtml("$path.html", tmp)
    }

    private fun getTemplateConfig() = TemplateConfig().also { config ->
        listOf(
            TeFunction("curDate") { LocalDate.now() },
            TeFunction("jQueryVersion") { "3.1" },
            TeFunction("jQueryMigrateVersion") { "1.2.1" },
            TeFunction("rowColor") { args -> if ((args.first() as Int) % 2 == 0) "altColor" else "rowColor" },
            TeFunction("h1Title") { args -> if ((args.first() as? String) == "package") "title=\"Package\" " else "" },
            TeFunction("createTabRow") { args ->
                val (link, doc) = args.first() as RowJavadocListEntry
                val contextRoot = args[1] as PageNode?
                (buildLink(
                    locationProvider.resolve(link, contextRoot),
                    link.name
                ) to contentToHtmlTranslator.htmlForContentNodes(doc, contextRoot)).pairToTag().trim()
            },
            TeFunction("createListRow") { args ->
                val link = args.first() as LinkJavadocListEntry
                val contextRoot = args[1] as PageNode?
                buildLink(
                    locationProvider.resolve(link, contextRoot),
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
                                        n.toLink()?.let{ buildLink(it, n.classNames.orEmpty()) } ?: n
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

    private val config = getTemplateConfig()
    private val templateRenderer = Templates(
        ResourceTemplateProvider(
            resourceDir
        ), config = config, cache = true
    )

    private class ResourceTemplateProvider(val basePath: String) : TemplateProvider {
        override suspend fun get(template: String): String =
            javaClass.classLoader.getResourceAsStream("$basePath/$template")?.bufferedReader()?.lines()?.toArray()
                ?.joinToString("\n") ?: throw IllegalStateException("Template not found: $basePath/$template")
    }

}
