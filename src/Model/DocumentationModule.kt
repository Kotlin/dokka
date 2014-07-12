package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.*

public class DocumentationModule(val module: ModuleDescriptor) : DocumentationNode(module, "module", DocumentationContent.Empty, DocumentationNode.Kind.Module) {
    fun merge(other: DocumentationModule): DocumentationModule {
        val model = DocumentationModule(module)
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }
}

fun BindingContext.createDocumentationModule(module: ModuleDescriptor, file: JetFile): DocumentationModule {
    val packageFragment = getPackageFragment(file)
    val documentationModule = DocumentationModule(module)
    if (packageFragment == null) throw IllegalArgumentException("File $file should have package fragment")

    val visitor = DocumentationNodeBuilder(this)
    packageFragment.accept(DocumentationBuildingVisitor(this, visitor), documentationModule)

    checkResolveChildren(documentationModule)

    return documentationModule
}
