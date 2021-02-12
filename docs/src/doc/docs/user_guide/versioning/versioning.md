# Versioning plugin

Versioning plugin aims to provide users with ability to create a versioned documentation.
Therefore, users of the documentation can view different versions of the documentation by going to the main page 
(for a single module projects this is module page, and for multimodule ones it is the all modules page) and change versions.

Versioning plugin is enabled by default in dokka so there is no need to apply it manually.

Versioning can be configured using:

* version - a string value representing a version that should be displayed in the dropdown.
* olderVersionsDir - an optional file that represents the parent directory containing folders with previous dokka outputs.
* versionsOrdering - an optional list of strings representing the ordering of versions that should be visible. 
  By default, dokka will try to use semantic versioning to create such ordering.
  
Above configuration should be placed under the `pluginsConfiguration` block specific for your build tool.
Configuration object is named `org.jetbrains.dokka.versioning.VersioningConfiguration`.

### Directory structure required

Versioning plugins requires documentation authors to keep previous outputs in a set structure:

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

