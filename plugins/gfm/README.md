# GFM plugin

`GFM` plugin adds the ability to generate documentation in `GitHub flavoured Markdown` format. It supports both
multimodule and multiplatform projects.

GFM plugin is shipped together with Dokka Gradle Plugin, so you can start using it
right away with one of the following tasks:

* `dokkaGfm` - generate documentation for a non multi-module project or one specific module.
* `dokkaGfmMultiModule` - generate documentation for a multi-module project, assemble it together and
  generate navigation page/menu for all the modules.

To use it with Maven or CLI runners, you have to add it as a dependency. You can find it on
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin)

GFM plugin comes built in with the Dokka Gradle plugin. You can also find it on 
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin).
