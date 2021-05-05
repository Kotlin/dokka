# Versioning plugin

Versioning plugin aims to provide users with ability to create a versioned documentation.
Therefore, users of the documentation can view different versions of the documentation by going to the main page and change versions.

Versioning plugin is applied by default but not enabled in Dokka so there is no need to apply it manually.

Versioning can be configured using:

* version - a string value representing a version that should be displayed in the dropdown.
* olderVersionsDir - an optional file that represents the parent directory containing folders with previous Dokka outputs.
* olderVersions - an optional list of directories, each containing a previous Dokka output.  Used after the contents of 
  `olderVersionsDir` 
  (if it's specified).
* versionsOrdering - an optional list of strings representing the ordering of versions that should be visible. 
  By default, Dokka will try to use semantic versioning to create such ordering.
  
Above configuration should be placed under the `pluginsConfiguration` block specific for your build tool.
Configuration object is named `org.jetbrains.dokka.versioning.VersioningConfiguration`.


!!! note
    In the current release versioning is available only for the multimodule. Supporting single module is scheduled for next release

### Directory structure required

If you pass previous versions using `olderVersionsDir`, a particular directory structure is required:

```
.
└── older_versions_dir
    └── 1.4.10
        ├── <dokka output>
    └── 1.4.20
        ├── <dokka output>
    ...
```

As can be seen on the diagram, `olderVersionsDir` should be a parent directory of previous output directories.

This can be avoided by manually specifying each past output directory with `olderVersions`, or they can be used 
together.

`olderVersions` directories need to contain a past Dokka output.  For the above example, you would pass 
`older_versions_dir/1.4.10, older_versions_dir/1.4.20`.

### Example

Versioning plugin in gradle can be configured in 2 ways: 

* by manually adding the versioning plugin to classpath and using `pluginsConfiguration`

* by using `pluginsMapConfiguration` and adding the configuration serialized as json under the `org.jetbrains.dokka.versioning.VersioningPlugin` key.


If you choose the first method the configuration may look like this:

```kotlin
buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:versioning-plugin:<dokka_version>")
  }
}

...

pluginConfiguration<org.jetbrains.dokka.versioning.VersioningPlugin, org.jetbrains.dokka.versioning.VersioningConfiguration> {
    version = "1.0"
    olderVersionsDir = projectDir.resolve("olderVersionsDir")
}
```

Alternatively, without adding plugin to classpath:

```kotlin
pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.versioning.VersioningPlugin" to """{ "version": "1.0" }"""))
```

Please consult the [Gradle documentation](../gradle/usage.md#applying-plugins) for more information about configuring Dokka with this build tool.
