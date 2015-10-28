package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.MarkdownFormatService
import org.junit.Test

public class MarkdownFormatTest {
    private val markdownService = MarkdownFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun emptyDescription() {
        verifyOutput("test/data/format/emptyDescription.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun classWithCompanionObject() {
        verifyOutput("test/data/format/classWithCompanionObject.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotations() {
        verifyOutput("test/data/format/annotations.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotationClass() {
        verifyOutput("test/data/format/annotationClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotationParams() {
        verifyOutput("test/data/format/annotationParams.kt", ".md", withKotlinRuntime = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun extensions() {
        verifyOutput("test/data/format/extensions.kt", ".package.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
        verifyOutput("test/data/format/extensions.kt", ".class.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun enumClass() {
        verifyOutput("test/data/format/enumClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
        verifyOutput("test/data/format/enumClass.kt", ".value.md") { model, output ->
            val enumClassNode = model.members.single().members[0]
            markdownService.appendNodes(tempLocation, output,
                    enumClassNode.members.filter { it.name == "LOCAL_CONTINUE_AND_BREAK" })
        }
    }

    @Test fun varargsFunction() {
        verifyOutput("test/data/format/varargsFunction.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun overridingFunction() {
        verifyOutput("test/data/format/overridingFunction.kt", ".md") { model, output ->
            val classMembers = model.members.single().members.first { it.name == "D" }.members
            markdownService.appendNodes(tempLocation, output, classMembers.filter { it.name == "f" })
        }

    }

    @Test fun propertyVar() {
        verifyOutput("test/data/format/propertyVar.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun functionWithDefaultParameter() {
        verifyOutput("test/data/format/functionWithDefaultParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun accessor() {
        verifyOutput("test/data/format/accessor.kt", ".md") { model, output ->
            val propertyNode = model.members.single().members.first { it.name == "C" }.members.filter { it.name == "x" }
            markdownService.appendNodes(tempLocation, output, propertyNode)
        }
    }

    @Test fun paramTag() {
        verifyOutput("test/data/format/paramTag.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun throwsTag() {
        verifyOutput("test/data/format/throwsTag.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeParameterBounds() {
        verifyOutput("test/data/format/typeParameterBounds.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeParameterVariance() {
        verifyOutput("test/data/format/typeParameterVariance.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeProjectionVariance() {
        verifyOutput("test/data/format/typeProjectionVariance.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javadocHtml() {
        verifyOutput("test/data/format/javadocHtml.java", ".md", withJdk = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaCodeLiteralTags() {
        verifyOutput("test/data/format/javaCodeLiteralTags.java", ".md", withJdk = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaCodeInParam() {
        verifyOutput("test/data/format/javaCodeInParam.java", ".md", withJdk = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaSpaceInAuthor() {
        verifyOutput("test/data/format/javaSpaceInAuthor.java", ".md", withJdk = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun nullability() {
        verifyOutput("test/data/format/nullability.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun operatorOverloading() {
        verifyOutput("test/data/format/operatorOverloading.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "C" }.members.filter { it.name == "plus" })
        }
    }

    @Test fun javadocOrderedList() {
        verifyOutput("test/data/format/javadocOrderedList.java", ".md", withJdk = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun companionObjectExtension() {
        verifyOutput("test/data/format/companionObjectExtension.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Foo" })
        }
    }

    @Test fun starProjection() {
        verifyOutput("test/data/format/starProjection.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun extensionFunctionParameter() {
        verifyOutput("test/data/format/extensionFunctionParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun summarizeSignatures() {
        verifyOutput("test/data/format/summarizeSignatures.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
    }

    @Test fun summarizeSignaturesProperty() {
        verifyOutput("test/data/format/summarizeSignaturesProperty.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
    }

    @Test fun reifiedTypeParameter() {
        verifyOutput("test/data/format/reifiedTypeParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotatedTypeParameter() {
        verifyOutput("test/data/format/annotatedTypeParameter.kt", ".md", withKotlinRuntime = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun inheritedMembers() {
        verifyOutput("test/data/format/inheritedMembers.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun inheritedExtensions() {
        verifyOutput("test/data/format/inheritedExtensions.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }
}
