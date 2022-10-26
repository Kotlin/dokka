# Sample plugin tutorial

We'll go over creating a simple plugin that covers a very common use case: generate documentation for everything except 
for members annotated with a custom `@Internal` annotation - they should be hidden.

The plugin will be tested with the following code:

```kotlin
package org.jetbrains.dokka.internal.test

annotation class Internal

fun shouldBeVisible() {}

@Internal
fun shouldBeExcludedFromDocumentation() {}
```

Expected behavior: function `shouldBeExcludedFromDocumentation` should not be visible in generated documentation.

Full source code of this tutorial can be found in Dokka's examples under 
[hide-internal-api](https://github.com/Kotlin/dokka/examples/plugin/hide-internal-api).

## Preparing the project

We'll begin by using [Dokka plugin template](https://github.com/Kotlin/dokka-plugin-template). Press the 
`Use this template` button and 
[open this project in IntelliJ IDEA](https://www.jetbrains.com/idea/guide/tutorials/working-with-gradle/opening-a-gradle-project/).

First, let's rename the pre-made `template` package and `MyAwesomeDokkaPlugin` class to something of our own.

For instance, package can be renamed to `org.example.dokka.plugin` and the class to `HideInternalApiPlugin`:

```kotlin
package org.example.dokka.plugin

import org.jetbrains.dokka.plugability.DokkaPlugin

class HideInternalApiPlugin : DokkaPlugin() {

}
```

After you do that, make sure to update the path to this class in
`resources/META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin`:
```kotlin
org.example.dokka.plugin.HideInternalApiPlugin
```

At this point you can also change project name in `settings.gradle.kts` (to `hide-internal-api` in our case)
and `groupId` in `build.gradle.kts`. 

## Extending Dokka

After preparing the project we can begin extending Dokka with our own extension.

Having read through [Core extensions](../architecture/extension_points/core_extensions.md), it's clear that we need
a `PreMergeDocumentableTransformer` extension in order to filter out undesired documentables. 

Moreover, the article mentioned a convenient abstract transformer `SuppressedByConditionDocumentableFilterTransformer`
which is perfect for our use case, so we can try to implement it.

Create a new class, place it next to your plugin and implement the abstract method. You should end up with this:

```kotlin
package org.example.dokka.plugin

import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin

class HideInternalApiPlugin : DokkaPlugin() {}

class HideInternalApiTransformer(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {
   
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        return false
    }
}
```

Now we somehow need to find all annotations applied to `d: Documentable` and see if our `@Internal` annotation is present.
However, it's not very clear how to do that. What usually helps is stopping in debugger and having a look at what fields
and values a given `Documentable` has.

To do that, we'll need to register our extension point first, then we can publish our plugin and set the breakpoint.

Having read through [Introduction to extensions](../architecture/extension_points/introduction.md), we now know
how to register our extensions:

```kotlin
class HideInternalApiPlugin : DokkaPlugin() {
    val myFilterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
    }
}
```

At this point we're ready to debug our plugin locally, it should already work, but do nothing.

## Debugging

Please read through [Debugging Dokka](../workflow.md#debugging-dokka), it goes over the same steps in more detail
and with examples. Below you will find rough instructions.

First, let's begin by publishing our plugin to `mavenLocal()`. 

```bash
./gradlew publishToMavenLocal
```

This will publish your plugin under the `groupId`, `artifactId` and `version` that you've specified in your
`build.gradle.kts`. In our case it's `org.example:hide-internal-api:1.0-SNAPSHOT`.

Open a debug project of your choosing that has Dokka configured, and add our plugin to dependencies:

```kotlin
dependencies {
    dokkaPlugin("org.example:hide-internal-api:1.0-SNAPSHOT")
}
```

Next, in that project let's run `dokkaHtml` with debug enabled:

```bash
./gradlew clean dokkaHtml -Dorg.gradle.debug=true --no-daemon
```

Switch to the plugin project, set a breakpoint inside `shouldBeSuppressed` and run jvm remote debug.

If you've done everything correctly, it should stop in debugger and you should be able to observe the values contained
inside `d: Documentable`.

## Implementing plugin logic

Now that we've stopped at our breakpoint, let's skip until we see `shouldBeExcludedFromDocumentation` function in the
place of `d: Documentable` (observe the changing `name` property).

Looking at what's inside the object, you might notice it has 3 values in `extra`, one of which is `Annotations`.
Sounds like something we need!

Having poked around, we come up with the following monstrosity of a code for determining if a given documentable has
`@Internal` annotation (it can of course be refactored.. later):

```kotlin
override fun shouldBeSuppressed(d: Documentable): Boolean {
   
    val annotations: List<Annotations.Annotation> =
        (d as? WithExtraProperties<*>)
            ?.extra
            ?.allOfType<Annotations>()
            ?.flatMap { it.directAnnotations.values.flatten() }
            ?: emptyList()

    return annotations.any { isInternalAnnotation(it) }
}

private fun isInternalAnnotation(annotation: Annotations.Annotation): Boolean {
   return annotation.dri.packageName == "org.jetbrains.dokka.internal.test"
           && annotation.dri.classNames == "Internal"
}
```

Seems like we're done with writing our plugin and can begin testing it manually.

## Manual testing

At this point, the implementation of your plugin should look roughly like this:

```kotlin
package org.example.dokka.plugin

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin

class HideInternalApiPlugin : DokkaPlugin() {
    val myFilterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
    }
}

class HideInternalApiTransformer(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations: List<Annotations.Annotation> =
            (d as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<Annotations>()
                ?.flatMap { it.directAnnotations.values.flatten() }
                ?: emptyList()

        return annotations.any { isInternalAnnotation(it) }
    }

    private fun isInternalAnnotation(annotation: Annotations.Annotation): Boolean {
        return annotation.dri.packageName == "org.jetbrains.dokka.internal.test"
                && annotation.dri.classNames == "Internal"
    }
}
```

Bump plugin version in `gradle.build.kts`, publish it to maven local, open the debug project and run `dokkaHtml` 
(without debug this time). It should work, you should **not** be able to see `shouldBeExcludedFromDocumentation`
function in generated documentation.

Manual testing is cool and all, but wouldn't it be better if we could somehow write unit tests for it? Indeed!

## Unit testing

You might've noticed that plugin template comes with a pre-made test class. Feel free to move it to another package
and rename it.

We are mostly interested in a single test case - functions annotated with `@Internal` should be hidden, while all other
public functions should be visible.

Plugin API comes with a set of convenient test utilities that are used to test Dokka itself, so it covers a wide range
of use cases. When in doubt, see Dokka's tests for reference.

Below you will find a complete unit test that passes, and the main takeaways below that.

```kotlin
package org.example.dokka.plugin

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.Test
import kotlin.test.assertEquals

class HideInternalApiPluginTest : BaseAbstractTest() {
   
    @Test
    fun `should hide annotated functions`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        val hideInternalPlugin = HideInternalApiPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package org.jetbrains.dokka.internal.test
            |
            |annotation class Internal
            |
            |fun shouldBeVisible() {}
            |
            |@Internal
            |fun shouldBeExcludedFromDocumentation() {}
        """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(hideInternalPlugin)
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testModule = modules.single { it.name == "root" }
                val testPackage = testModule.packages.single { it.name == "org.jetbrains.dokka.internal.test" }

                val packageFunctions = testPackage.functions
                assertEquals(1, packageFunctions.size)
                assertEquals("shouldBeVisible", packageFunctions[0].name)
            }
        }
    }
}
```

Note that the package of the tested code (inside `testInline` function) is the same as the package that we have
hardcoded in our plugin. Make sure to change that to your own if you are following along, otherwise it will fail.

Things to note and remember:

1. Your test class should extend `BaseAbstractTest`, which contains base utility methods for testing.
2. You can configure Dokka to your liking, enable some specific settings, configure 
   [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets), etc. All done via
   `dokkaConfiguration` DSL.
3. `testInline` function is the main entry point for unit tests
4. You can pass plugins to be used in a test, notice `pluginOverrides` parameter
5. You can write asserts for different stages of generating documentation, the main ones being `Documentables` model
   generation, `Pages` generation and `Output` generation. Since we implemented our plugin to work during
   `PreMergeDocumentableTransformer` stage, we can test it on the same level (that is
   `preMergeDocumentablesTransformationStage`).
6. You will need to write asserts using the model of whatever stage you choose. For `Documentable` transformation stage 
   it's `Documentable`, for `Page` generation stage you would have `Page` model, and for `Output` you can have `.html`
   files that you will need to parse with `JSoup` (there are also utilities for that).
