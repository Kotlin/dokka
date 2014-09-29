package org.jetbrains.dokka

public class DocumentationModule(name: String) : DocumentationNode(name, Content.Empty, DocumentationNode.Kind.Module) {
    fun merge(other: DocumentationModule): DocumentationModule {
        val model = DocumentationModule(name)
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }
}

