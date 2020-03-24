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
        runBlocking(Dispatchers.Default) {
            renderModulePageNode(r as JavadocModulePageNode)
            r.children.forEach { renderNode(it) }
        }
    }

    private fun buildContent(content: JavadocContentNode, parent: String) {
        if (content is JavadocContentGroup) {
            content.children.forEach { buildContent(it, parent) }
        } else if (content is ListNode) {
            content.children.forEach {
                buildListNode(it, parent)
            }
        }
    }

    private fun buildListNode(node: JavadocListEntry, parent: String) {
        when (node) {
            is CompoundJavadocListEntry -> when (node.name) {
                "row" -> node.build {
                    val linkEntry = (node.content.first() as LinkJavadocListEntry)
                    linkEntry.build { name, dris, _, pd ->
                        createLinkTag(
                            locationProvider.resolve(dris.first(), pd).relativizePath(parent),
                            name
                        )
                    }
                    val doc = (node.content.last() as SimpleJavadocListEntry).content
                    (linkEntry.stringTag to doc).pairToTag()
                }
            }
        }
    }

    fun CoroutineScope.renderNode(node: PageNode, path: String = "") {
        when (node) {
            is JavadocPackagePageNode -> renderPackagePageNode(node)
            is JavadocClasslikePageNode -> renderClasslikePage(node)
            is RendererSpecificPage -> renderSpecificPage(node, path)
        }
    }

    fun CoroutineScope.renderModulePageNode(node: JavadocModulePageNode) {
        val link = "."
        val name = "index"
        val pathToRoot = ""

        buildContent(node.content, link)
        val contentMap = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot
        ) + node.contentMap.unpack()
        writeFromTemplate(outputWriter, "$link/$name".toNormalized(), "listPage.korte", contentMap.toList())
        node.children.forEach {
            logger.info("${node.name} -> ${it.name}")
            renderNode(it, link)
        }
    }

    fun CoroutineScope.renderPackagePageNode(node: JavadocPackagePageNode) {
        val link = locationProvider.resolve(node.dri.first(), emptyList())
        val dir = Paths.get(link).parent.toNormalized()
        val pathToRoot = dir.split("/").joinToString("/") { ".." }.let {
            if (it.isNotEmpty()) "$it/" else it
        }

        buildContent(node.content, dir)
        val contentMap = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot
        ) + node.contentMap.unpack()
        writeFromTemplate(outputWriter, link, "listPage.korte", contentMap.toList())
        node.children.forEach {
            logger.info("${node.name} -> ${it.name}")
            renderNode(it, link.toNormalized())
        }
    }

    fun CoroutineScope.renderClasslikePage(node: JavadocClasslikePageNode) {
        val link = locationProvider.resolve(node.dri.first(), emptyList())
        val dir = Paths.get(link).parent.toNormalized()
        val pathToRoot = dir.split("/").joinToString("/") { ".." }.let {
            if (it.isNotEmpty()) "$it/" else it
        }
        buildContent(node.content, dir)

        val contentMap: Map<String, Any?> = mapOf(
            "docName" to "docName", // todo docname
            "pathToRoot" to pathToRoot,
            "list" to emptyList<String>()
        ) + node.contentMap.unpack()

        writeFromTemplate(outputWriter, link, "class.korte", contentMap.toList())
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
    fun ContentDRILink.toLinkTag() =
        createLinkTag(locationProvider.resolve(address, emptyList()), (children.first() as ContentText).text)

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

    fun getTemplateConfig() = TemplateConfig().also {
        it.register(TeFunction("curDate") { LocalDate.now() })
        it.register(TeFunction("jQueryVersion") { "3.1" })
        it.register(TeFunction("jQueryMigrateVersion") { "1.2.1" })
        it.register(TeFunction("rowColor") { if ((it.first() as Int) % 2 == 0) "altColor" else "rowColor" })
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