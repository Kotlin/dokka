## Building sample plugin

In order to load a plugin into Dokka, your class must extend `DokkaPlugin` class. A fully qualified name of that class must be placed in a file named `org.jetbrains.dokka.plugability.DokkaPlugin` under `resources/META-INF/services`.
All instances are automatically loaded during Dokka setup using `java.util.ServiceLoader`.

Dokka provides a set of entry points, for which user can create their own implementations. They must be delegated using `DokkaPlugin.extending(definition: ExtendingDSL.() -> Extension<T, *, *>)` function,that returns a delegate `ExtensionProvider` with supplied definition.

To create a definition, you can use one of two infix functions`with(T)` or `providing( (DokkaContext) -> T)` where `T` is the type of an extended endpoint. You can also use infix functions:

* `applyIf( () -> Boolean )` to add additional condition specifying whether or not the extension should be used
* `order((OrderDsl.() -> Unit))` to determine if your extension should be used before or after another particular extension for the same endpoint
* `override( Extension<T, *, *> )` to override other extension. Overridden extension won't be loaded and overridding one will inherit ordering from it.

Following sample provides custom translator object as a `DokkaCore.sourceToDocumentableTranslator`

```kotlin
package org.jetbrains.dokka.sample

import org.jetbrains.dokka.plugability.DokkaPlugin

class SamplePlugin : DokkaPlugin() {
    extension by extending {
        DokkaCore.sourceToDocumentableTranslator with CustomSourceToDocumentableTranslator
    }
}

object CustomSourceToDocumentableTranslator: SourceToDocumentableTranslator {
    override fun invoke(sourceSet: SourceSetData, context: DokkaContext): DModule
}
```
