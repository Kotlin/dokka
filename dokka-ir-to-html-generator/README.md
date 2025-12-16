# dokka-ir-to-html-generator

A small CLI that renders a static HTML page from a Dokka IR JSON input using a React/TypeScript frontend that is server‑side rendered via GraalJS.

## How to build and run

Prerequisites:
- JDK 8+ (the project targets Java 8 toolchain)
- Gradle wrapper (use `./gradlew`)

1) Build a fat JAR (shadow JAR)
- From the project root, run:
    ```bash
    ./gradlew :dokka-ir-to-html-generator:shadowJar
    ```
- The task bundles all dependencies and also builds the frontend bundle. The resulting JAR will be at:
  - `dokka-ir-to-html-generator/build/libs/docgen.jar`

2) Run it against any input.json
- Provide a path to a JSON file that contains the page data (an example `input.json` is in this module’s directory):
  ```bash
  java -jar build/libs/docgen.jar input.json
  ```

3) Inspect the result
- The program produces `output.html` in the current working directory.
- It also prints the raw rendered HTML to stdout for convenience.

## What this module consists of

This module has two main parts:

1. Kotlin CLI (server‑side rendering bridge)
   - `src/main/kotlin/Main.kt`
     - Entry point that reads the JSON input file.
     - Starts a GraalJS context and evaluates the webpacked server bundle (`server-bundle.js`).
     - Calls the exported `SSR.render` function to produce HTML string.
     - Injects the HTML and the serialized page data into an HTML template and writes `output.html`.
   - Resources
     - `src/main/resources/index.html` (copied from `src/frontend/public/index.html` by Gradle) — the HTML template containing placeholders for app markup and serialized data.

2. React/TypeScript frontend (rendered on the server via GraalJS)
   - Location: `src/frontend`
     - Client entry: `src/frontend/src/client/index.tsx` (hydration/runtime on the client if needed).
     - Server entry: `src/frontend/src/server/index.tsx` (exports `SSR.render(data)` used by Kotlin).
     - Shared app code: `src/frontend/src/shared/App.tsx`.
     - Polyfills for the server bundle: `src/frontend/src/server/polyfills.ts`.
     - Static HTML template: `src/frontend/public/index.html`.
     - Webpack configs: `src/frontend/webpack.config.js` (and `webpack.client.config.js` if present).
   - Build
     - Gradle runs `npm install` and `npm run build` under `src/frontend`, then copies the produced `server-bundle.js` and `index.html` into the JVM resources so they are packaged inside `docgen.jar`.
