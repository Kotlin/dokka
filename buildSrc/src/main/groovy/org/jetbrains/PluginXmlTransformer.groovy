package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import groovy.xml.XmlUtil
import org.gradle.api.file.FileTreeElement
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

public class PluginXmlTransformer implements Transformer {
    private Map<String, Node> transformedPluginXmlFiles = new HashMap<>();

    @Override
    boolean canTransformResource(FileTreeElement fileTreeElement) {
        return fileTreeElement.relativePath.segments.contains("META-INF") && fileTreeElement.name.endsWith(".xml")
    }

    @Override
    void transform(TransformerContext context) {
        def path = context.path
        def inputStream = context.is
        System.out.println(path)
        Node node = new XmlParser().parse(inputStream)
        relocateXml(node, context)
        transformedPluginXmlFiles.put(path, node)
    }

    @Override
    boolean hasTransformedResource() {
        return !transformedPluginXmlFiles.isEmpty()
    }

    @Override
    void modifyOutputStream(ZipOutputStream zipOutputStream) {
        for (Map.Entry<String, Node> entry : transformedPluginXmlFiles.entrySet()) {
            zipOutputStream.putNextEntry(new ZipEntry(entry.key))
            XmlUtil.serialize(entry.value, zipOutputStream)
        }
    }

    private static void relocateXml(Node node, TransformerContext context) {
        Map attributes = node.attributes()
        RelocateClassContext relocateClassContext = new RelocateClassContext()
        relocateClassContext.stats = context.stats
        for (Map.Entry entry : attributes.entrySet()) {
            relocateClassContext.setClassName((String) entry.getValue())
            entry.setValue(relocateClassName(relocateClassContext, context))
        }
        List<String> localText = node.localText()
        if (localText.size() == 1) {
            relocateClassContext.setClassName(localText[0])
            node.setValue(relocateClassName(relocateClassContext, context))
        }
        node.children().each {
            if (it instanceof Node) {
                relocateXml((Node) it, context)
            }
        }
    }

    private static String relocateClassName(RelocateClassContext relocateContext, TransformerContext context) {
        for (Relocator relocator : context.relocators) {
            if (relocator.canRelocateClass(relocateContext)) {
                return relocator.relocateClass(relocateContext)
            }
        }
        return relocateContext.className
    }
}