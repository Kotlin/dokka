# Dokka MultiModule example

This example demonstrates how to apply and configure Dokka in a  
[multi-project build](https://docs.gradle.org/current/userguide/multi_project_builds.html).

You can also learn how to set Dokka's version in [gradle.properties](gradle.properties) using `pluginManagement` 
configuration block in [settings.gradle.kts](settings.gradle.kts).

____

You can see up-to-date documentation generated for this example on
[GitHub Pages](https://kotlin.github.io/dokka/examples/dokka-multimodule-example/htmlMultiModule/index.html).

![screenshot demonstration of output](demo.png)

### Running

Run `dokkaHtmlMultiModule` task in order to generate documentation for this example:

```bash
./gradlew dokkaHtmlMultiModule
```

It will generate complete documentation for the root project and its subprojects, with a common
Table of Contents.
