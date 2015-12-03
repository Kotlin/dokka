package org.jetbrains.dokka.tests

import org.jetbrains.dokka.KotlinLanguageService
import org.jetbrains.dokka.MarkdownFormatService
import org.junit.Test

public class MarkdownFormatTest {
    private val markdownService = MarkdownFormatService(InMemoryLocationService, KotlinLanguageService())

    @Test fun emptyDescription() {
        verifyOutput("testdata/format/emptyDescription.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun classWithCompanionObject() {
        verifyOutput("testdata/format/classWithCompanionObject.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotations() {
        verifyOutput("testdata/format/annotations.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotationClass() {
        verifyOutput("testdata/format/annotationClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotationParams() {
        verifyOutput("testdata/format/annotationParams.kt", ".md", withKotlinRuntime = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun extensions() {
        verifyOutput("testdata/format/extensions.kt", ".package.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
        verifyOutput("testdata/format/extensions.kt", ".class.md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun enumClass() {
        verifyOutput("testdata/format/enumClass.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
        verifyOutput("testdata/format/enumClass.kt", ".value.md") { model, output ->
            val enumClassNode = model.members.single().members[0]
            markdownService.appendNodes(tempLocation, output,
                    enumClassNode.members.filter { it.name == "LOCAL_CONTINUE_AND_BREAK" })
        }
    }

    @Test fun varargsFunction() {
        verifyOutput("testdata/format/varargsFunction.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun overridingFunction() {
        verifyOutput("testdata/format/overridingFunction.kt", ".md") { model, output ->
            val classMembers = model.members.single().members.first { it.name == "D" }.members
            markdownService.appendNodes(tempLocation, output, classMembers.filter { it.name == "f" })
        }

    }

    @Test fun propertyVar() {
        verifyOutput("testdata/format/propertyVar.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun functionWithDefaultParameter() {
        verifyOutput("testdata/format/functionWithDefaultParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun accessor() {
        verifyOutput("testdata/format/accessor.kt", ".md") { model, output ->
            val propertyNode = model.members.single().members.first { it.name == "C" }.members.filter { it.name == "x" }
            markdownService.appendNodes(tempLocation, output, propertyNode)
        }
    }

    @Test fun paramTag() {
        verifyOutput("testdata/format/paramTag.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun throwsTag() {
        verifyOutput("testdata/format/throwsTag.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeParameterBounds() {
        verifyOutput("testdata/format/typeParameterBounds.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeParameterVariance() {
        verifyOutput("testdata/format/typeParameterVariance.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun typeProjectionVariance() {
        verifyOutput("testdata/format/typeProjectionVariance.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javadocHtml() {
        verifyJavaOutput("testdata/format/javadocHtml.java", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaCodeLiteralTags() {
        verifyJavaOutput("testdata/format/javaCodeLiteralTags.java", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaCodeInParam() {
        verifyJavaOutput("testdata/format/javaCodeInParam.java", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun javaSpaceInAuthor() {
        verifyJavaOutput("testdata/format/javaSpaceInAuthor.java", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun nullability() {
        verifyOutput("testdata/format/nullability.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun operatorOverloading() {
        verifyOutput("testdata/format/operatorOverloading.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.single { it.name == "C" }.members.filter { it.name == "plus" })
        }
    }

    @Test fun javadocOrderedList() {
        verifyJavaOutput("testdata/format/javadocOrderedList.java", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun companionObjectExtension() {
        verifyOutput("testdata/format/companionObjectExtension.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Foo" })
        }
    }

    @Test fun starProjection() {
        verifyOutput("testdata/format/starProjection.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun extensionFunctionParameter() {
        verifyOutput("testdata/format/extensionFunctionParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun summarizeSignatures() {
        verifyOutput("testdata/format/summarizeSignatures.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
    }

    @Test fun summarizeSignaturesProperty() {
        verifyOutput("testdata/format/summarizeSignaturesProperty.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members)
        }
    }

    @Test fun reifiedTypeParameter() {
        verifyOutput("testdata/format/reifiedTypeParameter.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun annotatedTypeParameter() {
        verifyOutput("testdata/format/annotatedTypeParameter.kt", ".md", withKotlinRuntime = true) { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members)
        }
    }

    @Test fun inheritedMembers() {
        verifyOutput("testdata/format/inheritedMembers.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }

    @Test fun inheritedExtensions() {
        verifyOutput("testdata/format/inheritedExtensions.kt", ".md") { model, output ->
            markdownService.appendNodes(tempLocation, output, model.members.single().members.filter { it.name == "Bar" })
        }
    }
}
