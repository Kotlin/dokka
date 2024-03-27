# Developer documentation

This module contains developer documentation which is published to GitHub pages:
[kotlin.github.io/dokka](https://kotlin.github.io/dokka/).

It is built using the [gradle-mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin).

## Building

You can build the documentation:

```Bash
./gradlew :docs-developer:mkDocsBuild
```

The output directory is `build/mkdocs`.

### Docker

Alternatively, you can use Docker:

```bash
docker run --rm -it -p 8000:8000 -v ./docs-developer/src/doc:/docs squidfunk/mkdocs-material
```

This will build the docs and start a web server under
[localhost:8000/Kotlin/dokka](http://localhost:8000/Kotlin/dokka/).

### Live-reload server

If you are using IntelliJ then a link to the docs will be logged in the console:

```text
Dokka Developer Docs: http://localhost:63342/dokka/docs-developer/build/mkdocs/index.html
```

To automatically rebuild the docs, run in continuous mode:

```Bash
./gradlew :docs-developer:mkDocsBuild --continuous --quiet
```

## Publishing

The documentation is published automatically for all changes in master and for every GitHub release.

See [gh-pages.yml](../.github/workflows/gh-pages-deploy-dev-docs.yml) workflow configuration for more details.
