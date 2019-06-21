package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.jetbrains.dokka.Generation.DocumentationMerger
import org.junit.Test

abstract class BaseMarkdownFormatTest(val analysisPlatform: Platform): FileGeneratorTestCase() {
    override val formatService = MarkdownFormatService(fileGenerator, KotlinLanguageService(), listOf())

    protected val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)

    @Test fun emptyDescription() {
        verifyMarkdownNode("emptyDescription", defaultModelConfig)
    }

    @Test fun classWithCompanionObject() {
        verifyMarkdownNode("classWithCompanionObject", defaultModelConfig)
    }

    @Test fun annotations() {
        verifyMarkdownNode("annotations", defaultModelConfig)
    }

    @Test fun annotationClass() {
        verifyMarkdownNode("annotationClass", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
        verifyMarkdownPackage("annotationClass", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun enumClass() {
        verifyOutput("testdata/format/enumClass.kt", ".md", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members.single().members, output)
        }
        verifyOutput("testdata/format/enumClass.kt", ".value.md", defaultModelConfig) { model, output ->
            val enumClassNode = model.members.single().members[0]
            buildPagesAndReadInto(
                    enumClassNode.members.filter { it.name == "LOCAL_CONTINUE_AND_BREAK" },
                    output
            )
        }
    }

    @Test fun varargsFunction() {
        verifyMarkdownNode("varargsFunction", defaultModelConfig)
    }

    @Test fun overridingFunction() {
        verifyMarkdownNodes("overridingFunction", defaultModelConfig) { model->
            val classMembers = model.members.single().members.first { it.name == "D" }.members
            classMembers.filter { it.name == "f" }
        }
    }

    @Test fun propertyVar() {
        verifyMarkdownNode("propertyVar", defaultModelConfig)
    }

    @Test fun functionWithDefaultParameter() {
        verifyMarkdownNode("functionWithDefaultParameter", defaultModelConfig)
    }

    @Test fun accessor() {
        verifyMarkdownNodes("accessor", defaultModelConfig) { model ->
            model.members.single().members.first { it.name == "C" }.members.filter { it.name == "x" }
        }
    }

    @Test fun paramTag() {
        verifyMarkdownNode("paramTag", defaultModelConfig)
    }

    @Test fun throwsTag() {
        verifyMarkdownNode("throwsTag", defaultModelConfig)
    }

    @Test fun typeParameterBounds() {
        verifyMarkdownNode("typeParameterBounds", defaultModelConfig)
    }

    @Test fun typeParameterVariance() {
        verifyMarkdownNode("typeParameterVariance", defaultModelConfig)
    }

    @Test fun typeProjectionVariance() {
        verifyMarkdownNode("typeProjectionVariance", defaultModelConfig)
    }

    @Test fun codeBlockNoHtmlEscape() {
        verifyMarkdownNodeByName("codeBlockNoHtmlEscape", "hackTheArithmetic", defaultModelConfig)
    }

    @Test fun companionObjectExtension() {
        verifyMarkdownNodeByName("companionObjectExtension", "Foo", defaultModelConfig)
    }

    @Test fun starProjection() {
        verifyMarkdownNode("starProjection", defaultModelConfig)
    }

    @Test fun extensionFunctionParameter() {
        verifyMarkdownNode("extensionFunctionParameter", defaultModelConfig)
    }

    @Test fun summarizeSignatures() {
        verifyMarkdownNodes("summarizeSignatures", defaultModelConfig) { model -> model.members }
    }

    @Test fun reifiedTypeParameter() {
        verifyMarkdownNode("reifiedTypeParameter", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun suspendInlineFunctionOrder() {
        verifyMarkdownNode("suspendInlineFunction", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun inlineSuspendFunctionOrderChanged() {
        verifyMarkdownNode("inlineSuspendFunction", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun annotatedTypeParameter() {
        verifyMarkdownNode("annotatedTypeParameter", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun inheritedMembers() {
        verifyMarkdownNodeByName("inheritedMembers", "Bar", defaultModelConfig)
    }

    @Test fun inheritedExtensions() {
        verifyMarkdownNodeByName("inheritedExtensions", "Bar", defaultModelConfig)
    }

    @Test fun genericInheritedExtensions() {
        verifyMarkdownNodeByName("genericInheritedExtensions", "Bar", defaultModelConfig)
    }

    @Test fun arrayAverage() {
        verifyMarkdownNodeByName("arrayAverage", "XArray", defaultModelConfig)
    }

    @Test fun multipleTypeParameterConstraints() {
        verifyMarkdownNode("multipleTypeParameterConstraints", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun inheritedCompanionObjectProperties() {
        verifyMarkdownNodeByName("inheritedCompanionObjectProperties", "C", defaultModelConfig)
    }

    @Test fun shadowedExtensionFunctions() {
        verifyMarkdownNodeByName("shadowedExtensionFunctions", "Bar", defaultModelConfig)
    }

    @Test fun inapplicableExtensionFunctions() {
        verifyMarkdownNodeByName("inapplicableExtensionFunctions", "Bar", defaultModelConfig)
    }

    @Test fun receiverParameterTypeBound() {
        verifyMarkdownNodeByName("receiverParameterTypeBound", "Foo", defaultModelConfig)
    }

    @Test fun extensionWithDocumentedReceiver() {
        verifyMarkdownNodes("extensionWithDocumentedReceiver", defaultModelConfig) { model ->
            model.members.single().members.single().members.filter { it.name == "fn" }
        }
    }

    @Test fun codeBlock() {
        verifyMarkdownNode("codeBlock", defaultModelConfig)
    }

    @Test fun exclInCodeBlock() {
        verifyMarkdownNodeByName("exclInCodeBlock", "foo", defaultModelConfig)
    }

    @Test fun backtickInCodeBlock() {
        verifyMarkdownNodeByName("backtickInCodeBlock", "foo", defaultModelConfig)
    }

    @Test fun qualifiedNameLink() {
        verifyMarkdownNodeByName("qualifiedNameLink", "foo",
            ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun functionalTypeWithNamedParameters() {
        verifyMarkdownNode("functionalTypeWithNamedParameters", defaultModelConfig)
    }

    @Test fun typeAliases() {
        verifyMarkdownNode("typeAliases", defaultModelConfig)
        verifyMarkdownPackage("typeAliases", defaultModelConfig)
    }

    @Test fun sampleByShortName() {
        verifyMarkdownNode("sampleByShortName", defaultModelConfig)
    }


    @Test fun suspendParam() {
        verifyMarkdownNode("suspendParam", defaultModelConfig)
        verifyMarkdownPackage("suspendParam", defaultModelConfig)
    }

    @Test fun sinceKotlin() {
        verifyMarkdownNode("sinceKotlin", defaultModelConfig)
        verifyMarkdownPackage("sinceKotlin", defaultModelConfig)
    }

    @Test fun sinceKotlinWide() {
        verifyMarkdownPackage("sinceKotlinWide", defaultModelConfig)
    }

    @Test fun dynamicType() {
        verifyMarkdownNode("dynamicType", defaultModelConfig)
    }

    @Test fun dynamicExtension() {
        verifyMarkdownNodes("dynamicExtension", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Foo" } }
    }

    @Test fun memberExtension() {
        verifyMarkdownNodes("memberExtension", defaultModelConfig) { model -> model.members.single().members.filter { it.name == "Foo" } }
    }

    @Test fun renderFunctionalTypeInParenthesisWhenItIsReceiver() {
        verifyMarkdownNode("renderFunctionalTypeInParenthesisWhenItIsReceiver", defaultModelConfig)
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
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test fun multiplePlatformsOmitRedundant() {
        val module = buildMultiplePlatforms("multiplatform/omitRedundant")
        verifyModelOutput(module, ".md", "testdata/format/multiplatform/omitRedundant/foo.kt") { model, output ->
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test fun multiplePlatformsImplied() {
        val module = buildMultiplePlatforms("multiplatform/implied")
        verifyModelOutput(module, ".md", "testdata/format/multiplatform/implied/foo.kt") { model, output ->
            val service = MarkdownFormatService(fileGenerator, KotlinLanguageService(), listOf("JVM", "JS"))
            fileGenerator.formatService = service
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test fun packagePlatformsWithExtExtensions() {
        val path = "multiplatform/packagePlatformsWithExtExtensions"
        val module = DocumentationModule("test")
        val passConfiguration = PassConfigurationImpl(
                noStdlibLink = true,
                noJdkLink = true,
                languageVersion = null,
                apiVersion = null
        )

        val dokkaConfiguration = DokkaConfigurationImpl(
            outputDir = "",
            format = "html",
            generateIndexPages = false,
            passesConfigurations = listOf(
                passConfiguration
            )
        )

        appendDocumentation(module, dokkaConfiguration, passConfiguration, ModelConfig(
            roots = arrayOf(contentRootFromPath("testdata/format/$path/jvm.kt")),
            defaultPlatforms = listOf("JVM"),
            withKotlinRuntime = true,
            analysisPlatform = analysisPlatform
            )
        )
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
            buildPagesAndReadInto(
                    listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }),
                    output
            )
        }
        verifyMultiplatformPackage(module, path)
    }

    @Test fun multiplePlatformsBreadcrumbsInMemberOfMemberOfGroupNode() {
        val path = "multiplatform/breadcrumbsInMemberOfMemberOfGroupNode"
        val module = buildMultiplePlatforms(path)
        verifyModelOutput(module, ".md", "testdata/format/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(
                    listOfNotNull(model.members.single().members.find { it.kind == NodeKind.GroupNode }?.member(NodeKind.Function)),
                    output
            )
        }
    }

    @Test fun linksInEmphasis() {
        verifyMarkdownNode("linksInEmphasis", defaultModelConfig)
    }

    @Test fun linksInStrong() {
        verifyMarkdownNode("linksInStrong", defaultModelConfig)
    }

    @Test fun linksInHeaders() {
        verifyMarkdownNode("linksInHeaders", defaultModelConfig)
    }

    @Test fun tokensInEmphasis() {
        verifyMarkdownNode("tokensInEmphasis", defaultModelConfig)
    }

    @Test fun tokensInStrong() {
        verifyMarkdownNode("tokensInStrong", defaultModelConfig)
    }

    @Test fun tokensInHeaders() {
        verifyMarkdownNode("tokensInHeaders", defaultModelConfig)
    }

    @Test fun unorderedLists() {
        verifyMarkdownNode("unorderedLists", defaultModelConfig)
    }

    @Test fun nestedLists() {
        verifyMarkdownNode("nestedLists", defaultModelConfig)
    }

    @Test fun referenceLink() {
        verifyMarkdownNode("referenceLink", defaultModelConfig)
    }

    @Test fun externalReferenceLink() {
        verifyMarkdownNode("externalReferenceLink", defaultModelConfig)
    }

    @Test fun newlineInTableCell() {
        verifyMarkdownPackage("newlineInTableCell", defaultModelConfig)
    }

    @Test fun indentedCodeBlock() {
        verifyMarkdownNode("indentedCodeBlock", defaultModelConfig)
    }

    @Test fun receiverReference() {
        verifyMarkdownNode("receiverReference", defaultModelConfig)
    }

    @Test fun extensionScope() {
        verifyMarkdownNodeByName("extensionScope", "test", defaultModelConfig)
    }

    @Test fun typeParameterReference() {
        verifyMarkdownNode("typeParameterReference", defaultModelConfig)
    }

    @Test fun notPublishedTypeAliasAutoExpansion() {
        verifyMarkdownNodeByName("notPublishedTypeAliasAutoExpansion", "foo", ModelConfig(
            analysisPlatform = analysisPlatform,
            includeNonPublic = false
        ))
    }

    @Test fun companionImplements() {
        verifyMarkdownNodeByName("companionImplements", "Foo", defaultModelConfig)
    }


    private fun buildMultiplePlatforms(path: String): DocumentationModule {
        val moduleName = "test"
        val passConfiguration = PassConfigurationImpl(
                noStdlibLink = true,
                noJdkLink = true,
                languageVersion = null,
                apiVersion = null
        )
        val dokkaConfiguration = DokkaConfigurationImpl(
            outputDir = "",
            format = "html",
            generateIndexPages = false,
            passesConfigurations = listOf(
                passConfiguration
            )

        )
        val module1 = DocumentationModule(moduleName)
        appendDocumentation(
            module1, dokkaConfiguration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/$path/jvm.kt")),
                defaultPlatforms = listOf("JVM"),
                analysisPlatform = Platform.jvm
            )
        )

        val module2 = DocumentationModule(moduleName)
        appendDocumentation(
            module2, dokkaConfiguration, passConfiguration, ModelConfig(
                roots = arrayOf(contentRootFromPath("testdata/format/$path/js.kt")),
                defaultPlatforms = listOf("JS"),
                analysisPlatform = Platform.js
            )
        )

        return DocumentationMerger(listOf(module1, module2), DokkaConsoleLogger).merge()
    }

    private fun verifyMultiplatformPackage(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".package.md", "testdata/format/$path/multiplatform.kt") { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
    }

    private fun verifyMultiplatformIndex(module: DocumentationModule, path: String) {
        verifyModelOutput(module, ".md", "testdata/format/$path/multiplatform.index.kt") {
            model, output ->
            val service = MarkdownFormatService(fileGenerator, KotlinLanguageService(), listOf())
            fileGenerator.formatService = service
            buildPagesAndReadInto(listOf(model), output)
        }
    }

    @Test fun blankLineInsideCodeBlock() {
        verifyMarkdownNode("blankLineInsideCodeBlock", defaultModelConfig)
    }

    protected fun verifyMarkdownPackage(fileName: String, modelConfig: ModelConfig = ModelConfig()) {
        verifyOutput("testdata/format/$fileName.kt", ".package.md", modelConfig) { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
    }

    protected fun verifyMarkdownNode(fileName: String, modelConfig: ModelConfig = ModelConfig()) {
        verifyMarkdownNodes(fileName, modelConfig) { model -> model.members.single().members }
    }

    protected fun verifyMarkdownNodes(
            fileName: String,
            modelConfig: ModelConfig = ModelConfig(),
            nodeFilter: (DocumentationModule) -> List<DocumentationNode>
    ) {
        verifyOutput(
                "testdata/format/$fileName.kt",
                ".md",
                modelConfig
        ) { model, output ->
            buildPagesAndReadInto(nodeFilter(model), output)
        }
    }

    protected fun verifyJavaMarkdownNode(fileName: String, modelConfig: ModelConfig = ModelConfig()) {
        verifyJavaMarkdownNodes(fileName, modelConfig) { model -> model.members.single().members }
    }

    protected fun verifyJavaMarkdownNodes(fileName: String, modelConfig: ModelConfig = ModelConfig(), nodeFilter: (DocumentationModule) -> List<DocumentationNode>) {
        verifyJavaOutput("testdata/format/$fileName.java", ".md", modelConfig) { model, output ->
            buildPagesAndReadInto(nodeFilter(model), output)
        }
    }

    protected fun verifyMarkdownNodeByName(
            fileName: String,
            name: String,
            modelConfig: ModelConfig = ModelConfig()
    ) {
        verifyMarkdownNodes(fileName, modelConfig) { model->
            val nodesWithName = model.members.single().members.filter { it.name == name }
            if (nodesWithName.isEmpty()) {
                throw IllegalArgumentException("Found no nodes named $name")
            }
            nodesWithName
        }
    }

    @Test fun nullableTypeParameterFunction() {
        verifyMarkdownNode("nullableTypeParameterFunction", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }
}

class JSMarkdownFormatTest: BaseMarkdownFormatTest(Platform.js)

class JVMMarkdownFormatTest: BaseMarkdownFormatTest(Platform.jvm) {

    @Test
    fun enumRef() {
        verifyMarkdownNode("enumRef", defaultModelConfig)
    }

    @Test
    fun javaCodeLiteralTags() {
        verifyJavaMarkdownNode("javaCodeLiteralTags", defaultModelConfig)
    }

    @Test
    fun nullability() {
        verifyMarkdownNode("nullability", defaultModelConfig)
    }

    @Test
    fun exceptionClass() {
        verifyMarkdownNode(
            "exceptionClass", ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        )
        verifyMarkdownPackage(
            "exceptionClass", ModelConfig(
                analysisPlatform = analysisPlatform,
                withKotlinRuntime = true
            )
        )
    }

    @Test
    fun operatorOverloading() {
        verifyMarkdownNodes("operatorOverloading", defaultModelConfig) { model->
            model.members.single().members.single { it.name == "C" }.members.filter { it.name == "plus" }
        }
    }

    @Test
    fun extensions() {
        verifyOutput("testdata/format/extensions.kt", ".package.md", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members, output)
        }
        verifyOutput("testdata/format/extensions.kt", ".class.md", defaultModelConfig) { model, output ->
            buildPagesAndReadInto(model.members.single().members, output)
        }
    }

    @Test
    fun summarizeSignaturesProperty() {
        verifyMarkdownNodes("summarizeSignaturesProperty", defaultModelConfig) { model -> model.members }
    }

    @Test
    fun javaSpaceInAuthor() {
        verifyJavaMarkdownNode("javaSpaceInAuthor", defaultModelConfig)
    }

    @Test
    fun javaCodeInParam() {
        verifyJavaMarkdownNodes("javaCodeInParam", defaultModelConfig) {
            selectNodes(it) {
                subgraphOf(RefKind.Member)
                withKind(NodeKind.Function)
            }
        }
    }

    @Test
    fun annotationParams() {
        verifyMarkdownNode("annotationParams", ModelConfig(analysisPlatform = analysisPlatform, withKotlinRuntime = true))
    }

    @Test fun inheritedLink() {
        val filePath = "testdata/format/inheritedLink"
        verifyOutput(
            filePath,
            ".md",
            ModelConfig(
                roots = arrayOf(
                    contentRootFromPath("$filePath.kt"),
                    contentRootFromPath("$filePath.1.kt")
                ),
                withJdk = true,
                withKotlinRuntime = true,
                includeNonPublic = false,
                analysisPlatform = analysisPlatform

            )
        ) { model, output ->
            buildPagesAndReadInto(model.members.single { it.name == "p2" }.members.single().members, output)
        }
    }

    @Test
    fun javadocOrderedList() {
        verifyJavaMarkdownNodes("javadocOrderedList", defaultModelConfig) { model ->
            model.members.single().members.filter { it.name == "Bar" }
        }
    }

    @Test
    fun jdkLinks() {
        verifyMarkdownNode("jdkLinks", ModelConfig(withKotlinRuntime = true, analysisPlatform = analysisPlatform))
    }

    @Test
    fun javadocHtml() {
        verifyJavaMarkdownNode("javadocHtml", defaultModelConfig)
    }
}

class CommonMarkdownFormatTest: BaseMarkdownFormatTest(Platform.common)