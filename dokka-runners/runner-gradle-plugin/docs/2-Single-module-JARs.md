# Dokka Gradle Plugin DSL 2.0 - single module JAR

## Use cases for generation of JAR from Dokka output:

1. Generate javadoc.jar for Maven Central requirements. Why:
    * Maven Central requires having javadoc.jar for any artifact (could be empty)
    * No real policy/doc/guide on what to do with Kotlin projects and javadoc.jar
    * format can be both `javadoc` / `html` / `gfm` (in okio/okhttp), because of the point above
2. Generate javadoc.jar and publish to Maven Central to have versioned documentation on https://javadoc.io. Why:
    * the current versioning plugin is hard
    * the current versioning plugin requires having access to previous output of Dokka, which is not possible with
      serverless environments like GitLab pages or GitHub pages.
      Note regarding GitHub pages: there are two ways to publish something to GitHub pages:
        1. [publishing from the branch](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-from-a-branch)
           (frequently from `github-pages`)
        2. [publishing via GitHub actions](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-with-a-custom-github-actions-workflow)

      The second option doesn't provide access to previous Dokka output as artifacts in GitHub actions have expiration
      limit
3. Generate some jar with HTML output and publish it to Maven Central to have versioned documentation work in serverless
   environment.
   Then during generation of documentation with `versioning` plugin it's possible to fetch documentation for previous
   versions from Maven Central.
   apollo-kotlin does this for multi-module HTML documentation to support versioning
   (not sure if it's published already)
4. Generate some other jar with `html`/`dokka`/`html-docs` classifier to have some documentation published somehow but
   not confusing with `javadoc.jar` (because it's kotlin docs and not javadoc)

## Questions:

* should we provide out-of-the-box JAR generation?
* should we provide support for generation in multiple different formats?
* future question: should we provide generation of JAR for multi-module projects

## Possible Options

Format selection is out of scope for now, as we don't know how it will be setup

### Option 1: do nothing out of the box

Then it will be similar to what we have now

```kotlin
val javadocJar by tasks.registering(Jar::class) {
    // task name can differ if javadoc format is used: buildDokkaJavadoc f.e.
    // or it's could be used with Gradle generated type safe accessors if inside build scripts with dokka applied:
    // `from(tasks.buildDokka)`
    from(tasks.named("buildDokka"))
    // classifier can be any other
    archiveClassifier.set("javadoc")
}

// add to publications (config is dependent on project) 
publishing {
    publications.withType<MavenPublication>().configureEach {
        from(javadocJar)
    }
}
```

Pros:

* nothing needed from us
* we don't force users to use something that is unknown on how to do correctly

Cons:

* every project will copy-paste this config all the time
* Users will still need to manually add this jar to publications (which sometime could be more complex than jar
  generation)

### Option 2: provide a function to create a task

```kotlin
// in build.gradle.kts where dokka plugin is applied

// for *-javadoc.jar 
val javadocJar = dokka.registerDokkaJavadocJarTask() // TaskProvider<Jar>
// or *-html.jar
val customJar = dokka.registerDokkaJarTask(classifier = "html")

// add to publications (config is dependent on project) 
publishing {
    publications.withType<MavenPublication>().configureEach {
        from(javadocJar)
    }
}
```

Pros:

* no copy paste code in user projects
* function is discoverable on extension

Cons:

* not really simplifies the user's code (?)

### Option 3: provide automatic DSL

Do it almost automatically similar to how KGP does with sources.
In this case, we will automatically add this jar to publication:

```kotlin
// API of KGP:
kotlin {
    // `publish=true` by default, so in KGP this API is used only to exclude sources from publishing
    withSourcesJar(publish = true)
    targets.configureEach {
        withSourcesJar(publish = true)
    }
}

kotlin {
    // we can provide an extension here;
    // theoretically it can be enabled by default when dokka and kotlin plugins are applied
    withDokkaJavadocJar()
    // or other jar
    withDokkaJar(classifier = "html-docs")

    // same for targets if needed (?)
    targets.configureEach {
        withDokkaJavadocJar()
        withDokkaJar(classifier = "html-docs")
    }
}
```

Example on how `javadoc` generation is configured in Gradle `java` plugin:

```kotlin
java {
    withSourcesJar()
    withJavadocJar()
}
```

Pros:

* everything is configured via one function
* API like in KGP

Cons:

* not so flexible
* not available in cases when there is no sources / no kotlin plugin (f.e multimodule)
