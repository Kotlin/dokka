# Migration Guide: Kotlin Playground for Samples

Starting from Dokka 2.0.0, Kotlin Playground for `@sample` rendering is disabled by default and has been extracted into a separate plugin.

## What Changed

### Before (Dokka 1.x)
```kotlin
/**
 * @sample com.example.MyClass.sampleFunction  
 */
fun myFunction() { }
```

By default, this would render as an interactive Kotlin Playground that users could run in the browser.

### After (Dokka 2.0.0)
```kotlin
/**
 * @sample com.example.MyClass.sampleFunction  
 */
fun myFunction() { }
```

By default, this now renders as a **static code block** (non-runnable).

## Migration Options

### Option 1: Keep Static Samples (Recommended)

No changes needed. Your samples will render as static code blocks, which works better for:
- Libraries with external dependencies
- Code that requires special setup
- Documentation that should focus on the code structure rather than execution

### Option 2: Enable Interactive Playground

If you want to keep the old behavior with interactive samples, add the Kotlin Playground plugin:

#### Gradle (Kotlin DSL)
```kotlin
dependencies {
    dokkaPlugin("org.jetbrains.dokka:kotlin-playground-plugin:2.0.0")
}
```

#### Gradle (Groovy DSL)  
```groovy
dependencies {
    dokkaPlugin 'org.jetbrains.dokka:kotlin-playground-plugin:2.0.0'
}
```

#### Maven
```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <configuration>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>kotlin-playground-plugin</artifactId>
                <version>2.0.0</version>
            </plugin>
        </plugins>
    </configuration>
</plugin>
```

### Option 3: Custom Playground Configuration

For advanced setups with custom dependencies:

```kotlin
dokka {
    pluginsConfiguration {
        plug("kotlin-playground") {
            // Use your own playground script with custom dependencies
            playgroundScript = "https://your-custom-playground.example.com/playground.js"
            
            // Point to your custom playground server
            playgroundServerUrl = "https://your-playground-server.example.com"
        }
    }
}
```

## Benefits of the Change

1. **Better default experience**: Most samples can't run in the default playground due to external dependencies
2. **Faster documentation loading**: No playground script loaded unless explicitly needed
3. **More flexible**: Separating the plugin allows for custom playground configurations
4. **Better debugging**: Clearer error messages when samples can't be resolved

## Error Message Improvements

The new version also provides better error messages when sample links can't be resolved:

```
The sample link 'com.example.MissingFunction' used in 'com.example.MyClass.myFunction' could not be resolved. 
Please make sure it points to a reachable Kotlin function and that the sample source is included in the 'samples' configuration.
```