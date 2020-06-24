package org.jetbrains.dokka.base.parsers

import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.base.parsers.factories.DocTagsFromStringFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor

class HtmlParser : Parser() {

    inner class NodeFilterImpl : NodeFilter {

        private val nodesCache: MutableMap<Int, MutableList<DocTag>> = mutableMapOf()
        private var currentDepth = 0

        fun collect(): DocTag = nodesCache[currentDepth]!![0]

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
                DocTagsFromStringFactory.getInstance(nodeName, nodesCache.getOrDefault(currentDepth, mutableListOf()).toList(), params, body).also {
                    nodesCache[currentDepth] = mutableListOf()
                    currentDepth = depth
                }
            } else {
                DocTagsFromStringFactory.getInstance(nodeName, emptyList(), params, body)
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


    private fun htmlToDocNode(string: String): DocTag {
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


