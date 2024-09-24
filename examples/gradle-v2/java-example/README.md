# Java Example project

This project demonstrates how Dokka Gradle Plugin can be applied to a pure Java project
to generate documentation.

This project has multiple modules.

* [my-java-application](my-java-application) is a Java Application
* [my-java-application](my-java-features) is a Java Library, demonstrating
  [feature variants](https://docs.gradle.org/current/userguide/feature_variants.html).
* [my-java-application](my-java-library) is a Java Library

### Demonstration

To generate HTML documentation, run

```shell
gradle :dokkaGenerate
```

The HTML documentation will be generated into [build/dokka/html](./build/dokka/html/).

### Implementation details

Note that the `org.jetbrains.dokka:kotlin-as-java-plugin` Dokka Plugin
must be applied for Java sources to be rendered as Java.
(Despite the plugin's name, it also affects how Java sources are rendered.)

This example applies the `org.jetbrains.dokka:kotlin-as-java-plugin`
Dokka Plugin in the
[`./buildSrc/src/main/kotlin/dokka-conventions.gradle.kts` convention plugin](buildSrc/src/main/kotlin/dokka-convention.gradle.kts).
