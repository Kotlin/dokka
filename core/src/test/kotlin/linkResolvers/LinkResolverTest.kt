package org.jetbrains.dokka.tests.linkResolvers
//
//import junit.framework.Assert.assertEquals
//import org.jetbrains.dokka.testApi.DokkaConfigurationImpl
//import org.jetbrains.dokka.Platform
//import org.jetbrains.dokka.links.DRI
//import org.jetbrains.dokka.pages.*
//import org.jetbrains.dokka.renderers.FileWriter
//import org.jetbrains.dokka.renderers.NewHtmlRenderer
//import org.jetbrains.dokka.resolvers.DefaultLocationProvider
//import org.junit.Test
//
//
//class LinkResolverTest {
//
//    fun createModel(): ModulePageNode {
//        val platform = listOf(PlatformData("jvm", Platform.jvm))
//
//        val moduleSymbol = ContentSymbol(
//            listOf(ContentText("moduleName", platform)),
//            platform)
//
//        val packageDRI = DRI("packageName")
//
//        val module = ModulePageNode("",
//            listOf(moduleSymbol, ContentBlock(
//                "packages",
//                listOf(ContentGroup(listOf(ContentLink("packageName", packageDRI, platform)), platform)),
//                platform)
//            ),
//            null)
//
//        val packageSymbol = ContentSymbol(
//            listOf(ContentText("package moduleName.packageName", platform)),
//            platform)
//
//        val packageNode = PackagePageNode("packageName", listOf(packageSymbol), module, DRI("packageName"))
//
//        val classSymbol = ContentSymbol(
//            listOf(ContentText("class ClassName()", platform)),
//            platform)
//
//        val classNode = ClassPageNode("className", listOf(classSymbol), packageNode, DRI("packageName", "className"))
//
//        val memberSymbol = ContentSymbol(
//            listOf(ContentText("fun funName(): String", platform)),
//            platform)
//
//        val memberText = ContentText("This is some docs for funName", platform)
//
//        val memberNode = MemberPageNode("funName",
//            listOf(memberSymbol, ContentComment(listOf(memberText), platform)),
//            classNode,
//            DRI("packageName", "className", "funName", "..."))
//
//        module.appendChild(packageNode)
//        packageNode.appendChild(classNode)
//        classNode.appendChild(memberNode)
//        return module
//    }
//
//    @Test fun memberLink() {
//        val model = createModel()
//        val linkResolver = DefaultLocationProvider(model, DokkaConfigurationImpl())
//        val link = linkResolver.resolve(model.children.first().children.first().children.first())
//        assertEquals("/--root--/package-name/class-name/fun-name", link)
//    }
//
//    @Test fun classLink() {
//        val model = createModel()
//        val linkResolver = DefaultLocationProvider(model, DokkaConfigurationImpl())
//        val link = linkResolver.resolve(model.children.first().children.first())
//        assertEquals("/--root--/package-name/class-name/index", link)
//    }
//
//    @Test fun moduleLink() {
//        val model = createModel()
//        val linkResolver = DefaultLocationProvider(model, DokkaConfigurationImpl())
//        val link = linkResolver.resolve(model)
//        assertEquals("/--root--/index", link)
//    }
//
//    @Test fun writeToFile() {
//        val model = createModel()
//        val linkResolver = DefaultLocationProvider(model, DokkaConfigurationImpl())
//        val fileWriter = FileWriter("/Users/kamildoleglo/IdeaProjects/dokka/build/dokka", ".html")
//        val renderer = NewHtmlRenderer("/Users/kamildoleglo/IdeaProjects/dokka/build/dokka", fileWriter, linkResolver)
//        renderer.render(model)
//    }
//
//}
//
