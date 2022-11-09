# Jekyll plugin

Jekyll plugin adds the ability to generate documentation in `Jekyll Flavoured Markdown` format. It supports both
multimodule and multiplatform projects.

Jekyll plugin is shipped together with Dokka Gradle Plugin, so you can start using it
right away with one of the following tasks:

* `dokkaJekyll` - generate documentation for a non multi-module project or one specific module.
* `dokkaJekyllMultiModule` - generate documentation for a multi-module project, assemble it together and
  generate navigation page/menu for all the modules.

To use it with Maven or CLI runners, you have to add it as a dependency. You can find it on 
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/jekyll-plugin)

**This plugin is at its early stages**, so you may experience issues and encounter bugs. Feel free to
[report](https://github.com/Kotlin/dokka/issues/new/choose) any errors you see.
