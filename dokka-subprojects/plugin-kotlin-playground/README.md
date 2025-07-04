# Kotlin Playground Plugin

This plugin adds support for interactive Kotlin Playground samples in Dokka documentation.

## Overview

By default, Dokka renders `@sample` tags as static code blocks. This plugin transforms those static code blocks into interactive Kotlin Playground instances that can be executed in the browser.

## Usage

To enable the plugin, add it to your Dokka configuration:

### Gradle (Kotlin DSL)

```kotlin
dokka {
    pluginsConfiguration {
        plug("kotlin-playground") {
            // Optional: Use a custom playground script
            playgroundScript = "https://example.com/custom-playground.js"
            
            // Optional: Use a custom playground server URL
            playgroundServerUrl = "https://custom.playground.server"
        }
    }
}
```

### Gradle (Groovy DSL)

```groovy
dokka {
    pluginsConfiguration {
        plug("kotlin-playground") {
            playgroundScript "https://example.com/custom-playground.js"
            playgroundServerUrl "https://custom.playground.server"
        }
    }
}
```

## Configuration

The plugin accepts the following configuration options:

- `playgroundScript` (optional): URL to a custom Kotlin Playground script. If not provided, uses the default playground script.
- `playgroundServerUrl` (optional): Base URL for a custom playground server. Requires a custom playground script that supports the custom server.

## How it Works

1. The plugin identifies code blocks that were generated from `@sample` tags
2. It adds the `RunnableSample` style to these code blocks
3. It embeds the Kotlin Playground script in the generated documentation
4. The Kotlin Playground JavaScript library then transforms these code blocks into interactive playground instances

## Requirements

- The samples must be valid Kotlin code that can run in the Kotlin Playground environment
- External dependencies must be available in the playground environment (or a custom playground server must be used)