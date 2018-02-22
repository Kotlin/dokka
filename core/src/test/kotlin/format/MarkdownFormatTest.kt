package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.junit.Test

class MarkdownFormatTest {
    private val markdownService = MarkdownFormatService(InMemoryLocationService, KotlinLanguageService(), listOf())

    @Test fun emptyDescription() {
        verifyMarkdownNode("emptyDescription")
    }

    @Test fun classWithCompanionObject() {
        verifyMarkdownNode("classWithCompanionObject")
    }

    @Test fun annotations() {
        verifyMarkdownNode("annotations")
    }

    @Test fun annotationClass() {
        verifyMarkdownNode("annotationClass", withKotlinRuntime = true)
        verifyMarkdownPackage("annotationClass", withKotlinRuntime = true)
    }

    @Test fun exceptionClass() {
        verifyMarkdownNode("exceptionClass", withKotlinRuntime = true)
        verifyMarkdownPackage("exceptionClass", withKotlinRuntime = true)
    }

    @Test fun annotationParams() {
        verifyMarkdownNode("annotationParams", withKotlinRuntime = true)
    }

    @Test fun extensions() {
        verifyOutput("testdata/format/extensions.kt", ".package.md") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members)
        }
        verifyOutput("testdata/format/extensions.kt", ".class.md") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
    }

    @Test fun enumClass() {
        verifyOutput("testdata/format/enumClass.kt", ".md") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
        verifyOutput("testdata/format/enumClass.kt", ".value.md") { model, output ->
            val enumClassNode = model.members.single().members[0]
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(
                    enumClassNode.members.filter { it.name == "LOCAL_CONTINUE_AND_BREAK" })
        }
    }

    @Test fun varargsFunction() {
        verifyMarkdownNode("varargsFunction")
    }

    @Test fun overridingFunction() {
        verifyMarkdownNodes("overridingFunction") { model->
            val classMembers = model.members.single().members.first { it.name == "D" }.members
            classMembers.filter { it.name == "f" }
        }
    }

    @Test fun propertyVar() {
        verifyMarkdownNode("propertyVar")
    }

    @Test fun functionWithDefaultParameter() {
        verifyMarkdownNode("functionWithDefaultParameter")
    }

    @Test fun accessor() {
        verifyMarkdownNodes("accessor") { model ->
            model.members.single().members.first { it.name == "C" }.members.filter { it.name == "x" }
        }
    }

    @Test fun paramTag() {
        verifyMarkdownNode("paramTag")
    }

    @Test fun throwsTag() {
        verifyMarkdownNode("throwsTag")
    }

    @Test fun typeParameterBounds() {
        verifyMarkdownNode("typeParameterBounds")
    }

    @Test fun typeParameterVariance() {
        verifyMarkdownNode("typeParameterVariance")
    }

    @Test fun typeProjectionVariance() {
        verifyMarkdownNode("typeProjectionVariance")
    }

    @Test fun javadocHtml() {
        verifyJavaMarkdownNode("javadocHtml")
    }

    @Test fun javaCodeLiteralTags() {
        verifyJavaMarkdownNode("javaCodeLiteralTags")
    }

    @Test fun javaCodeInParam() {
        verifyJavaMarkdownNode("javaCodeInParam")
    }

    @Test fun javaSpaceInAuthor() {
        verifyJavaMarkdownNode("javaSpaceInAuthor")
    }

    @Test fun nullability() {
        verifyMarkdownNode("nullability")
    }

    @Test fun operatorOverloading() {
        verifyMarkdownNodes("operatorOverloading") { model->
            model.members.single().members.single { it.name == "C" }.members.filter { it.name == "plus" }
        }
    }

    @Test fun javadocOrderedList() {
        verifyJavaMarkdownNodes("javadocOrderedList") { model ->
            model.members.single().members.filter { it.name == "Bar" }
        }
    }

    @Test fun codeBlockNoHtmlEscape() {
        verifyMarkdownNodeByName("codeBlockNoHtmlEscape", "hackTheArithmetic")
    }

    @Test fun companionObjectExtension() {
        verifyMarkdownNodeByName("companionObjectExtension", "Foo")
    }

    @Test fun starProjection() {
        verifyMarkdownNode("starProjection")
    }

    @Test fun extensionFunctionParameter() {
        verifyMarkdownNode("extensionFunctionParameter")
    }

    @Test fun summarizeSignatures() {
        verifyMarkdownNodes("summarizeSignatures") { model -> model.members }
    }

    @Test fun summarizeSignaturesProperty() {
        verifyMarkdownNodes("summarizeSignaturesProperty") { model -> model.members }
    }

    @Test fun reifiedTypeParameter() {
        verifyMarkdownNode("reifiedTypeParameter", withKotlinRuntime = true)
    }

    @Test fun annotatedTypeParameter() {
        verifyMarkdownNode("annotatedTypeParameter", withKotlinRuntime = true)
    }

    @Test fun inheritedMembers() {
        verifyMarkdownNodeByName("inheritedMembers", "Bar")
    }

    @Test fun inheritedExtensions() {
        verifyMarkdownNodeByName("inheritedExtensions", "Bar")
    }

    @Test fun genericInheritedExtensions() {
        verifyMarkdownNodeByName("genericInheritedExtensions", "Bar")
    }

    @Test fun arrayAverage() {
        verifyMarkdownNodeByName("arrayAverage", "XArray")
    }

    @Test fun multipleTypeParameterConstraints() {
        verifyMarkdownNode("multipleTypeParameterConstraints", withKotlinRuntime = true)
    }

    @Test fun inheritedCompanionObjectProperties() {
        verifyMarkdownNodeByName("inheritedCompanionObjectProperties", "C")
    }

    @Test fun shadowedExtensionFunctions() {
        verifyMarkdownNodeByName("shadowedExtensionFunctions", "Bar")
    }

    @Test fun inapplicableExtensionFunctions() {
        verifyMarkdownNodeByName("inapplicableExtensionFunctions", "Bar")
    }

    @Test fun receiverParameterTypeBound() {
        verifyMarkdownNodeByName("receiverParameterTypeBound", "Foo")
    }

    @Test fun extensionWithDocumentedReceiver() {
        verifyMarkdownNodes("extensionWithDocumentedReceiver") { model ->
            model.members.single().members.single().members.filter { it.name == "fn" }
        }
    }

    @Test fun jdkLinks() {
        verifyMarkdownNode("jdkLinks", withKotlinRuntime = true)
    }

    @Test fun codeBlock() {
        verifyMarkdownNode("codeBlock")
    }

    @Test fun exclInCodeBlock() {
        verifyMarkdownNodeByName("exclInCodeBlock", "foo")
    }

    @Test fun backtickInCodeBlock() {
        verifyMarkdownNodeByName("backtickInCodeBlock", "foo")
    }

    @Test fun qualifiedNameLink() {
        verifyMarkdownNodeByName("qualifiedNameLink", "foo", withKotlinRuntime = true)
    }

    @Test fun functionalTypeWithNamedParameters() {
        verifyMarkdownNode("functionalTypeWithNamedParameters")
    }

    @Test fun typeAliases() {
        verifyMarkdownNode("typeAliases")
        verifyMarkdownPackage("typeAliases")
    }

    @Test fun sampleByFQName() {
        verifyMarkdownNode("sampleByFQName")
    }

    @Test fun sampleByShortName() {
        verifyMarkdownNode("sampleByShortName")
    }


    @Test fun suspendParam() {
        verifyMarkdownNode("suspendParam")
        verifyMarkdownPackage("suspendParam")
    }

    @Test fun sinceKotlin() {
        verifyMarkdownNode("sinceKotlin")
        verifyMarkdownPackage("sinceKotlin")
    }

    @Test fun sinceKotlinWide() {
        verifyMarkdownPackage("sinceKotlinWide")
    }

    @Test fun dynamicType() {
        verifyMarkdownNode("dynamicType")
    }

    @Test fun dynamicExtension() {
        verifyMarkdownNodes("dynamicExtension") { model -> model.members.single().members.filter { it.name == "Foo" } }
    }

    @Test fun memberExtension() {
        verifyMarkdownNodes("memberExtension") { model -> model.members.single().members.filter { it.name == "Foo" } }
    }

    @Test fun renderFunctionalTypeInParenthesisWhenItIsReceiver() {
        verifyMarkdownNode("renderFunctionalTypeInParenthesisWhenItIsReceiver")
    }

    @Test fun multiplePlatforms() {
        verifyMultiplatformPackage(buildMultiplePlatforms("multiplatform/simple"), "multiplatform/simple")
    }

    @Test fun multiplePlatformsMerge() {
        verifyMultiplatformPackage(buildMultiplePlatforms("multiplatform/merge"), "multiplatform/merge")
    }

    @Test fun multiplePlatformsMergeMembers() {
        val module = buildMultiplePlatforms("multiplatform/mergeMembers")
        verifyModelOutput(module, ".md", "testdata/format/multiplatform/mergeMembers/foo.kt") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
    }

    @Test fun multiplePlatformsOmitRedundant() {
        val module = buildMultiplePlatforms("multiplatform/omitRedundant")
        verifyModelOutput(module, ".md", "testdata/format/multiplatform/omitRedundant/foo.kt") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
    }

    @Test fun multiplePlatformsImplied() {
        val module = buildMultiplePlatforms("multiplatform/implied")
        verifyModelOutput(module, ".md", "testdata/format/multiplatform/implied/foo.kt") { model, output ->
            MarkdownFormatService(InMemoryLocationService, KotlinLanguageService(), listOf("JVM", "JS"))
                    .createOutputBuilder(output, tempLocation).appendNodes(model.members.single().members)
        }
    }

    @Test fun packagePlatformsWithExtExtensions() {
        val path = "multiplatform/packagePlatformsWithExtExtensions"
        val module = DocumentationModule("test")
        val options = DocumentationOptions(
                outputDir = "",
                outputFormat = "html",
                generateIndexPages = false,
                noStdlibLink = true,
                languageVersion = null,
                apiVersion = null
        )
        appendDocumentation(module, contentRootFromPath("testdata/format/$path/jvm.kt"), defaultPlatforms = listOf("JVM"), withKotlinRuntime = true, options = options)
        verifyMultiplatformIndex(module, path)
        verifyMultiplatformPackage(module, path)
    }

    @Test fun multiplePlatformsPackagePlatformFromMembers() {
        val path = "multiplatform/packagePlatformsFromMembers"
        val module = buildMultiplePlatforms(path)
        verifyMultiplatformIndex(module, path)
        verifyMultiplatformPackage(module, path)
    }

    @Test fun multiplePlatformsGroupNode() {
        val path = "multiplatform/groupNode"
        val module = buildMultiplePlatforms(path)
        verifyModelOutput(module, ".md", "testdata/format/$path/multiplatform.kt") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation)
                    .appendNodes(listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }))
        }
        verifyMultiplatformPackage(module, path)
    }

    @Test fun multiplePlatformsBreadcrumbsInMemberOfMemberOfGroupNode() {
        val path = "multiplatform/breadcrumbsInMemberOfMemberOfGroupNode"
        val module = buildMultiplePlatforms(path)
        verifyModelOutput(module, ".md", "testdata/format/$path/multiplatform.kt") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation)
                    .appendNodes(listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }?.member(NodeKind.Class)?.member(NodeKind.Function)))
        }
    }

    @Test fun linksInEmphasis() {
        verifyMarkdownNode("linksInEmphasis")
    }

    @Test fun linksInStrong() {
        verifyMarkdownNode("linksInStrong")
    }

    @Test fun linksInHeaders() {
        verifyMarkdownNode("linksInHeaders")
    }

    @Test fun tokensInEmphasis() {
        verifyMarkdownNode("tokensInEmphasis")
    }

    @Test fun tokensInStrong() {
        verifyMarkdownNode("tokensInStrong")
    }

    @Test fun tokensInHeaders() {
        verifyMarkdownNode("tokensInHeaders")
    }

    @Test fun unorderedLists() {
        verifyMarkdownNode("unorderedLists")
    }

    @Test fun nestedLists() {
        verifyMarkdownNode("nestedLists")
    }

    @Test fun referenceLink() {
        verifyMarkdownNode("referenceLink")
    }

    @Test fun externalReferenceLink() {
        verifyMarkdownNode("externalReferenceLink")
    }

    @Test fun newlineInTableCell() {
        verifyMarkdownPackage("newlineInTableCell")
    }

    @Test fun indentedCodeBlock() {
        verifyMarkdownNode("indentedCodeBlock")
    }

    @Test fun receiverReference() {
        verifyMarkdownNode("receiverReference")
    }

    @Test fun extensionScope() {
        verifyMarkdownNodeByName("extensionScope", "test")
    }

    @Test fun typeParameterReference() {
        verifyMarkdownNode("typeParameterReference")
    }

    @Test fun notPublishedTypeAliasAutoExpansion() {
        verifyMarkdownNodeByName("notPublishedTypeAliasAutoExpansion", "foo", includeNonPublic = false)
    }

    @Test fun companionImplements() {
        verifyMarkdownNodeByName("companionImplements", "Foo")
    }

    private fun buildMultiplePlatforms(path: String): DocumentationModule {
        val module = DocumentationModule("test")
        val options = DocumentationOptions(
                outputDir = "",
                outputFormat = "html",
                generateIndexPages = false,
                noStdlibLink = true,
                languageVersion = null,
                apiVersion = null
        )
        appendDocumentation(module, contentRootFromPath("testdata/format/$path/jvm.kt"), defaultPlatforms = listOf("JVM"), options = options)
        appendDocumentation(module, contentRootFromPath("testdata/format/$path/js.kt"), defaultPlatforms = listOf("JS"), options = options)
        return module
    }

    private fun verifyMultiplatformPackage(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".package.md", "testdata/format/$path/multiplatform.kt") { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members)
        }
    }

    private fun verifyMultiplatformIndex(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".md", "testdata/format/$path/multiplatform.index.kt") {
            model, output ->
            MarkdownFormatService(InMemoryLocationService, KotlinLanguageService(), listOf())
                    .createOutputBuilder(output, tempLocation).appendNodes(listOf(model))
        }
    }

    @Test fun blankLineInsideCodeBlock() {
        verifyMarkdownNode("blankLineInsideCodeBlock")
    }

    private fun verifyMarkdownPackage(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyOutput("testdata/format/$fileName.kt", ".package.md", withKotlinRuntime = withKotlinRuntime) { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(model.members)
        }
    }

    private fun verifyMarkdownNode(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyMarkdownNodes(fileName, withKotlinRuntime) { model -> model.members.single().members }
    }

    private fun verifyMarkdownNodes(
            fileName: String,
            withKotlinRuntime: Boolean = false,
            includeNonPublic: Boolean = true,
            nodeFilter: (DocumentationModule) -> List<DocumentationNode>
    ) {
        verifyOutput(
                "testdata/format/$fileName.kt",
                ".md",
                withKotlinRuntime = withKotlinRuntime,
                includeNonPublic = includeNonPublic
        ) { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(nodeFilter(model))
        }
    }

    private fun verifyJavaMarkdownNode(fileName: String, withKotlinRuntime: Boolean = false) {
        verifyJavaMarkdownNodes(fileName, withKotlinRuntime) { model -> model.members.single().members }
    }

    private fun verifyJavaMarkdownNodes(fileName: String, withKotlinRuntime: Boolean = false, nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyJavaOutput("testdata/format/$fileName.java", ".md", withKotlinRuntime = withKotlinRuntime) { model, output ->
            markdownService.createOutputBuilder(output, tempLocation).appendNodes(nodeFilter(model))
        }
    }

    private fun verifyMarkdownNodeByName(
            fileName: String,
            name: String,
            withKotlinRuntime: Boolean = false,
            includeNonPublic: Boolean = true
    ) {
        verifyMarkdownNodes(fileName, withKotlinRuntime, includeNonPublic) { model->
            val nodesWithName = model.members.single().members.filter { it.name == name }
            if (nodesWithName.isEmpty()) {
                throw IllegalArgumentException("Found no nodes named $name")
            }
            nodesWithName
        }
    }
}
