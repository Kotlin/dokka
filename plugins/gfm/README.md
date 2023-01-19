# GFM plugin

The GFM plugin adds the ability to generate documentation in `GitHub Flavoured Markdown` format. It supports both
multi-module and multiplatform projects.

The GFM plugin is shipped together with the Dokka Gradle Plugin, so you can start using it
right away with one of the following tasks:

* `dokkaGfm` - generate documentation for a single-project build or one specific module.
* `dokkaGfmMultiModule` - generate documentation for a multi-module project, assemble it together and
  generate navigation page/menu for all the modules.

To use it with Maven or the CLI runner, you have to add it as a dependency. You can find it on
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin)

GFM plugin comes built in with the Dokka Gradle plugin. You can also find it on 
[Maven Central](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin).
