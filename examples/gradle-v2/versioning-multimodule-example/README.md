# Dokka Versioning MultiModule example

This example demonstrates configuration of Dokka's [versioning plugin](../../../dokka-subprojects/plugin-versioning),
which
allows readers to navigate through different versions of your API reference documentation.

The example contains some code that exists only in the current documentation version `1.0`. You will not see
this code in the previous version `0.9`, which is located in the [previousDocVersions](previousDocVersions) directory.

___

You can see up-to-date documentation generated for this example on
[GitHub Pages](https://kotlin.github.io/dokka/examples/dokka-versioning-multimodule-example/htmlMultiModule/index.html).

![screenshot demonstration of output](demo.png)

### Running

Run `dokkaHtmlMultiModule` task to generate documentation for this example:

```bash
./gradlew dokkaHtmlMultiModule
```

It will generate complete documentation for the root project and its subprojects, with the version navigation
dropdown menu.
