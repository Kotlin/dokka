# Extension points

In this section you can learn how to create new extension points, how to configure existing ones, and how to query for 
registered extensions when generating documentation.

## Declaring extension points

If you are writing a plugin, you can create your own extension points that other developers (or you) can use in other 
plugins / parts of code.

```kotlin
class MyPlugin : DokkaPlugin() {
    val sampleExtensionPoint by extensionPoint<SampleExtensionPointInterface>()
}

interface SampleExtensionPointInterface {
    fun doSomething(input: Input): List<Output>
}

class Input
class Output
```

Usually, you would want to provide some default implementations for your extension points. You can do that
within the same plugin class by extending an extension point you've just created. See 
[Extending from extension points](#extending-from-extension-points) for examples.

## Extending from extension points

You can use extension points to provide your own implementations in order to customize a plugin's behaviour.

If you want to provide an implementation for an extension point declared in an external plugin (including `DokkaBase`), 
you can use plugin querying API to do that. 

The example below shows how to extend `MyPlugin` (that was created above) with an implementation of 
`SampleExtensionPointInterface`.

```kotlin
class MyExtendedPlugin : DokkaPlugin() {
    
    val mySampleExtensionImplementation by extending {
        plugin<MyPlugin>().sampleExtensionPoint with SampleExtensionImpl()
    }
}

class SampleExtensionImpl : SampleExtensionPointInterface {
    override fun doSomething(input: Input): List<Output> = listOf()
}
```

Alternatively, if it is your own plugin, you can do that within the same class as the extension point itself:

```kotlin
open class MyPlugin : DokkaPlugin() {
    val sampleExtensionPoint by extensionPoint<SampleExtensionPointInterface>()

    val defaultSampleExtension by extending {
        sampleExtensionPoint with DefaultSampleExtension()
    }
}

class DefaultSampleExtension : SampleExtensionPointInterface {
    override fun doSomething(input: Input): List<Output> = listOf()
}
```

### Providing

If you need to have access to `DokkaContext` when creating an extension, you can use the `providing` keyword instead. 

```kotlin
val defaultSampleExtension by extending {
    sampleExtensionPoint providing { context ->
        // can use context to query other extensions or get configuration 
        DefaultSampleExtension() 
    }
}
```

You can read more on what you can do with `context` in [Obtaining extension instance](#obtaining-extension-instance).

### Override

By extending an extension point, you are registering an _additional_ extension. This behaviour is expected by some
extension points, for example the `Documentable` transformers, because all registered transformer extensions do their own 
transformations independently and one after the other.

However, a plugin can expect only a single extension to be registered for an extension point. In this case, you can use 
the `override` keyword to override the existing registered extension:

```kotlin
class MyExtendedPlugin : DokkaPlugin() {
    private val myPlugin by lazy { plugin<MyPlugin>() }

    val mySampleExtensionImplementation by extending {
        (myPlugin.sampleExtensionPoint
                with SampleExtensionImpl()
                override myPlugin.defaultSampleExtension)
    }
}
```

This is also useful if you wish to override some extension from `DokkaBase`, to disable or alter it.

### Order

Sometimes, the order in which extensions are invoked matters. This is something you can control as well using the 
`order` construct:

```kotlin
class MyExtendedPlugin : DokkaPlugin() {
    private val myPlugin by lazy { plugin<MyPlugin>() }

    val mySampleExtensionImplementation by extending {
        myPlugin.sampleExtensionPoint with SampleExtensionImpl() order {
            before(myPlugin.firstExtension)
            after(myPlugin.thirdExtension)
        }
    }
}
```

### Conditional apply

If you want your extension to be registered only if some condition is `true`, you can use the `applyIf` construct:

```kotlin
class MyExtendedPlugin : DokkaPlugin() {
    private val myPlugin by lazy { plugin<MyPlugin>() }
    
    val mySampleExtensionImplementation by extending {
        myPlugin.sampleExtensionPoint with SampleExtensionImpl() applyIf {
            Random.Default.nextBoolean()
        }
    }
}
```

## Obtaining extension instance

After an extension point has been [created](#declaring-extension-points) and some extensions have been
[registered](#extending-from-extension-points), you can use `query` and `querySingle` functions to find all or just a 
single implementation.

```kotlin
class MyExtension(context: DokkaContext) {
    // returns all registered extensions for the extension point
    val allSampleExtensions = context.plugin<MyPlugin>().query { sampleExtensionPoint }
    
    // will throw an exception if more than one extension is found.
    // use if you expect only a single extension to be registered for the extension point
    val singleSampleExtensions = context.plugin<MyPlugin>().querySingle { sampleExtensionPoint }
    
    fun invoke() {
        allSampleExtensions.forEach { it.doSomething(Input()) }
        
        singleSampleExtensions.doSomething(Input())
    }
}
```

In order to have access to `DokkaContext`, you can use the [providing](#providing) keyword when registering an extension.
