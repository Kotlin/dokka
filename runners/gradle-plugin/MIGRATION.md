### Configuring a dokka task
Dokka 1.4.x will create dedicated tasks for each format. 
You can expect the following formats being registered and configured by default: 
- `dokkaHtml`
- `dokkaJavadoc`
- `dokkaGfm`
- `dokkaJekyll`

Therefore, you need to either select specifically which task you want to configure or configure all with Type `DokkaTask`

```kotlin
/* 0.10.x */ 
tasks.dokka.configure { /*...*/ }

/* 1.4.x */
// configure all formats
tasks.withType<DokkaTask>().configureEach { /*...*/ }

// configure only html format e.g.
tasks.dokkaHtml.configure { /*...*/ }
```

#### properties
```kotlin
/* 0.10.x */    outputFormat = "html"
/* 1.4.x        No equivalent. 
                Formats are only configured by plugins. 
                See `dokkaHtml`, `dokkaJavadoc`,... tasks */

/* 0.10.x */    outputDirectory = "$buildDir/javadoc"
/* 1.4.x */     outputDirectory.set(buildDir.resolve("javadoc"))
    
    
/* 0.10.x */    subProjects = ["subproject1", "subproject2"]
/* 1.4.x        No equivalent.
                    See `DokkaCollectorTask` and `DokkaMultiModuleTask` */

        
/* 0.10.x */    disableAutoconfiguration = false 
/* 1.4.x        No equivalent.
                Source sets are synced with Kotlin Model by default. 
                All settings can still be overridden */

/* 0.10.x */    cacheRoot = "default" 
/* 1.4.x */     cacheRoot.set(file("default"))

```

### Configure a source set
```kotlin
/* 0.10.x */
tasks.dokka.configure { 
    configuration {
        // ...
    }
}

/* 1.4.x */
tasks.dokkaHtml.configure { 
    dokkaSourceSets {
        named("main") { /* configure main source set */ }
        configureEach {  /* configure all source sets */ }
        register("custom") { /* register custom source set */ }
    }
}
```

#### Properties
```kotlin
/* 0.10.x */    moduleName = "myModule"
/* 1.4.x */     /* Use AbstractDokkaTask#moduleName instead */

/* 0.10.x */    includeNonPublic = false
/* 1.4.x */     includeNonPublic.set(false)

/* 0.10.x */    skipDeprecated = false 
/* 1.4.x */     skipDeprecated.set(false) 

/* 0.10.x */    reportUndocumented = true 
/* 1.4.x */     reportUndocumented.set(true) 

/* 0.10.x */    skipEmptyPackages = true
/* 1.4.x */     skipEmptyPackages.set(true)

/* 0.10.x */    targets = ["JVM"] 
/* 1.4.x */     /* No equivalent */
                /* Use platform and displayName instead */

/* 0.10.x */    platform = "JVM"  
/* 1.4.x */     platform.set(org.jetbrains.dokka.Platform.jvm)
/* 1.4.x */     platform.set(Platform.jvm) // with import

/* 0.10.x */    classpath = [new File("$buildDir/other.jar")]
/* 1.4.x */     classpath.setFrom(buildDir.resolve("other.jar")) // setting classpath
/* 1.4.x */     classpath.from(buildDir.resolve("other.jar")) // adding to existing classpath
 
/* 0.10.x */    sourceRoots = [files("src/main/kotlin")]
/* 1.4.x */     sourceRoots.setFrom(file("src/main/kotlin")) // setting all source roots
/* 1.4.x */     sourceRoots.from(file("src/main/kotlin")) // adding to existing source roots

/* 0.10.x */    includes = ["packages.md", "extra.md"]
/* 1.4.x */     includes.setFrom(files("packages.md", "extra.md")) // setting all includes
/* 1.4.x */     includes.from(files("packages.md", "extra.md")) // adding to existing includes

/* 0.10.x */    samples = ["samples/basic.kt", "samples/advanced.kt"]
/* 1.4.x */     samples.setFrom(files("samples/basic.kt", "samples/advanced.kt"))
/* 1.4.x */     samples.from(files("samples/basic.kt", "samples/advanced.kt"))

/* 0.10.x */    kotlinTasks { /* ... */ }
/* 1.4.x */     /* No *direct* equivalent */
                /* Source sets synced with Kotlin Gradle Plugin will be configured properly */
                /* Custom source sets can use extension `kotlinSourceSet(...)` */

/* 0.10.x */    jdkVersion = 6
/* 1.4.x */     jdkVersion.set(6)

/* 0.10.x */    noStdlibLink = false
/* 1.4.x */     noStdlibLink.set(false)

/* 0.10.x */    noJdkLink = false
/* 1.4.x */     noJdkLink.set(false)

sourceLink {
    /* 0.10.x */    path = "src/main/kotlin"
    /* 1.4.x */     localDirectory.set(file("src/main/kotlin"))

    /* 0.10.x */    url = "https://github.com/myproject/blob/master/src/main/kotlin"
    /* 1.4.x */     remoteUrl.set(java.net.URL("https://github.com/myproject/blob/master/src/main/kotlin"))
    /* 1.4.x */     remoteUrl.set(uri("https://github.com/myproject/blob/master/src/main/kotlin").toURL())

    /* 0.10.x */    lineSuffix = "#L"
    /* 1.4.x */     remoteLineSuffix.set("#L")
}


externalDocumentationLink {
    /* 0.10.x */    url = URL("https://example.com/docs/")
    /* 1.4.x */     url.set(URL("https://example.com/docs/"))

    /* 0.10.x */    packageListUrl = URL("file:///home/user/localdocs/package-list")
    /* 1.4.x */     packageListUrl.set(URL("file:///home/user/localdocs/package-list"))
}

// Allows to customize documentation generation options on a per-package basis
// Repeat for multiple packageOptions
perPackageOption {
    /* 0.10.x */    prefix = "kotlin"
    /* 1.4.x */     prefix.set("kotlin")

    /* 0.10.x */    skipDeprecated = false
    /* 1.4.x */     skipDeprecated.set(false)

    /* 0.10.x */    reportUndocumented = true
    /* 1.4.x */     reportUndocumented.set(true)

    /* 0.10.x */    includeNonPublic = false
    /* 1.4.x */     includeNonPublic.set(false)

    /* 0.10.x */    suppress = true
    /* 1.4.x */     suppress.set(true)

}
```

For more information or help, feel free to ask questions in the [official Kotlin Slack Channel](https://kotlinlang.slack.com/)
