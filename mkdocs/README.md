# MkDocs documentation

This module contains documentation which is published to GitHub pages: 
[kotlin.github.io/dokka](https://kotlin.github.io/dokka/).

It is built using the [gradle-mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin).

## Building

You can build the documentation locally:

```Bash
./gradlew :mkdocs:mkdocsBuild
```

The output directory is `build/mkdocs`

### Livereload server

Alternatively, you can run a livereload server that automatically rebuilds documentation on every change:

```Bash
./gradlew :mkdocs:mkdocsServe
```

By default, it is run under [127.0.0.1:3001](http://127.0.0.1:3001/), but you can change it in 
[mkdocs.yml](src/doc/mkdocs.yml) by setting the `dev_addr` option.

## Publishing

The documentation is published automatically for all changes in master and for every GitHub release.

See [gh-pages.yml](../.github/workflows/gh-pages.yml) workflow configuration for more details.
