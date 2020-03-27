package javadoc

import com.soywiz.korte.TeFunction
import com.soywiz.korte.TemplateConfig
import com.soywiz.korte.TemplateProvider
import com.soywiz.korte.Templates
import javadoc.pages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.renderers.Renderer
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

class KorteJavadocRenderer(val outputWriter: OutputWriter, val context: DokkaContext, val resourceDir: String) :
    Renderer {
    private val locationProvider = JavadocLocationProvider()
    private val logger = context.logger

    override fun render(root: RootPageNode) = root.let { preprocessors.fold(root) { r, t -> t.invoke(r) } }.let { r ->
        runBlocking(Dispatchers.IO) {
            renderModulePageNode(r as JavadocModulePageNode)
        }
    }

    private fun templateForNode(node: JavadocPageNode) = when {
        node is JavadocClasslikePageNode -> "class.korte"
        node is JavadocPackagePageNode || node is JavadocModulePageNode -> "tabPage.korte"
        else -> "listPage.korte"
    }

    private fun CoroutineScope.renderNode(node: PageNode, path: String = "") {
        if (node is JavadocPageNode) {
            renderJavadocNode(node)
        }
        else if (node is RendererSpecificPage) {
            renderSpecificPage(node, path)
        }
    }

    private fun CoroutineScope.renderModulePageNode(node: JavadocModulePageNode) {
        val link = "."
        val name = "index"
        val pathToRoot = ""

        val contentMap = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot
        ) + node.contentMap
        writeFromTemplate(outputWriter, "$link/$name".toNormalized(), "tabPage.korte", contentMap.toList())
        node.children.forEach { renderNode(it, link) }
    }

    private fun CoroutineScope.renderJavadocNode(node: JavadocPageNode) {
        val link = locationProvider.resolve(node.dri.first(), emptyList(), node)
        val dir = Paths.get(link).parent?.let{it.toNormalized()}.orEmpty()
        val pathToRoot = dir.split("/").joinToString("/") { ".." }.let {
            if (it.isNotEmpty()) "$it/" else it
        }

//        val fileLink = when(node) {
//            is JavadocClasslikePageNode -> node.name
//            else -> link
//        }

        val contentMap = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot,
            "dir" to dir
        ) + node.contentMap
        writeFromTemplate(outputWriter, link, templateForNode(node), contentMap.toList())
        node.children.forEach { renderNode(it, link.toNormalized()) }
    }

    fun CoroutineScope.renderSpecificPage(node: RendererSpecificPage, path: String) = launch {
        when (val strategy = node.strategy) {
            is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, "")
            is RenderingStrategy.Write -> outputWriter.writeHtml(path, strategy.text)
            is RenderingStrategy.Callback -> outputWriter.writeHtml(
                path,
                strategy.instructions(this@KorteJavadocRenderer, node)
            )
            RenderingStrategy.DoNothing -> Unit
        }
    }

    fun Pair<String, String>.pairToTag() = "\n<td>${first}</td>\n<td>${second}</td>"
    fun LinkJavadocListEntry.toLinkTag(parent: String? = null) =
        createLinkTag(locationProvider.resolve(dri.first(), platformData).let {
            if (parent != null) it.relativizePath(parent)
            else it
        }, name)

    fun createLinkTag(address: String, name: String) =
        address.let { if (it.endsWith(".html")) it else "$it.html" }.let {
            """<a href="$it">$name</a>"""
        }

    private fun String.parent() = Paths.get(this).parent.toNormalized()
    private fun Path.toNormalized() = this.normalize().toFile().toString()
    private fun String.toNormalized() = Paths.get(this).toNormalized()
    private fun String.relativizePath(parent: String) = Paths.get(parent).relativize(Paths.get(this)).toNormalized()

    private fun Map<String, ContentValue>.unpack() =
        entries.map { (k, v) -> k to if (v is StringValue) v.text else (v as ListValue).list }

    private fun OutputWriter.writeHtml(path: String, text: String) = write(path, text, ".html")
    private fun CoroutineScope.writeFromTemplate(
        writer: OutputWriter,
        path: String,
        template: String,
        args: List<Pair<String, *>>
    ) =
        launch {
            writer.writeHtml(
                path,
                templateRenderer.render(template, *(args.toTypedArray()))
            )
        }

    fun getTemplateConfig() = TemplateConfig().also {config ->
        listOf(
            TeFunction("curDate") { LocalDate.now() },
            TeFunction("jQueryVersion") { "3.1" },
            TeFunction("jQueryMigrateVersion") { "1.2.1" },
            TeFunction("rowColor") {args -> if ((args.first() as Int) % 2 == 0) "altColor" else "rowColor" },
            TeFunction("h1Title") { args -> if ((args.first() as? String) == "package") "title=\"Package\" " else "" },
            TeFunction("createTabRow") { args ->
                val (link, doc) = args.first() as RowJavadocListEntry
                val dir = args[1] as String?
                (link.toLinkTag(dir) to doc).pairToTag().trim()
            },
            TeFunction("createListRow") { args ->
                val link = args.first() as LinkJavadocListEntry
                val dir = args[1] as String?
                link.toLinkTag(dir)
            }
        ).forEach { config.register(it) }
    }

    val config = getTemplateConfig()
    val templateRenderer = Templates(ResourceTemplateProvider(resourceDir), config = config, cache = true)

    class ResourceTemplateProvider(val basePath: String) : TemplateProvider {
        override suspend fun get(template: String): String? = kotlin.runCatching {
            ClassLoader.getSystemResource("$basePath/$template").file.let(::File).readLines()
                .joinToString("\n")
        }.let {
            if (it.isFailure) throw IllegalStateException("No template for $basePath/$template")
            else it.getOrNull()
        }
    }
}