package parsers

import model.doc.*
import org.jetbrains.dokka.parsers.factories.DocNodesFromStringFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor

class HtmlParser : Parser() {

    inner class NodeFilterImpl : NodeFilter {

        private val nodesCache: MutableMap<Int, MutableList<DocNode>> = mutableMapOf()
        private var currentDepth = 0

        fun collect(): DocNode = nodesCache[currentDepth]!![0]

        override fun tail(node: Node?, depth: Int): NodeFilter.FilterResult {
            val nodeName = node!!.nodeName()
            val nodeAttributes = node.attributes()

            if(nodeName in listOf("#document", "html", "head"))
                return NodeFilter.FilterResult.CONTINUE

            val body: String
            val params: Map<String, String>


            if(nodeName != "#text") {
                body = ""
                params = nodeAttributes.map { it.key to it.value }.toMap()
            } else {
                body = nodeAttributes["#text"]
                params = emptyMap()
            }

            val docNode = if(depth < currentDepth) {
                DocNodesFromStringFactory.getInstance(nodeName, nodesCache.getOrDefault(currentDepth, mutableListOf()).toList(), params, body).also {
                    nodesCache[currentDepth] = mutableListOf()
                    currentDepth = depth
                }
            } else {
                DocNodesFromStringFactory.getInstance(nodeName, emptyList(), params, body)
            }

            nodesCache.getOrDefault(depth, mutableListOf()) += docNode
            return NodeFilter.FilterResult.CONTINUE
        }

        override fun head(node: Node?, depth: Int): NodeFilter.FilterResult {

            val nodeName = node!!.nodeName()

            if(currentDepth < depth) {
                currentDepth = depth
                nodesCache[currentDepth] = mutableListOf()
            }

            if(nodeName in listOf("#document", "html", "head"))
                return NodeFilter.FilterResult.CONTINUE

            return NodeFilter.FilterResult.CONTINUE
        }
    }


    private fun htmlToDocNode(string: String): DocNode {
        val document = Jsoup.parse(string)
        val nodeFilterImpl = NodeFilterImpl()
        NodeTraversor.filter(nodeFilterImpl, document.root())
        return nodeFilterImpl.collect()
    }

    private fun replaceLinksWithHrefs(javadoc: String): String = Regex("\\{@link .*?}").replace(javadoc) {
        val split = it.value.dropLast(1).split(" ")
        if(split.size !in listOf(2, 3))
            return@replace it.value
        if(split.size == 3)
            return@replace "<documentationlink href=\"${split[1]}\">${split[2]}</documentationlink>"
        else
            return@replace "<documentationlink href=\"${split[1]}\">${split[1]}</documentationlink>"
    }

    override fun parseStringToDocNode(extractedString: String) = htmlToDocNode(extractedString)
    override fun preparse(text: String) = replaceLinksWithHrefs(text)
}


