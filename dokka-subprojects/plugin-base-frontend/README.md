### How to use frontend dev-server:

0. Prebuild Dokka's output for the ui-showcase project:

```bash
# Remove previous build
rm -rf dokka-integration-tests/gradle/build/ui-showcase-result
# Set output path
export DOKKA_TEST_OUTPUT_PATH='build/ui-showcase-result'
# Run gradle task
./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject
 ```

<small> For this repetitive sequence of tasks, it could be convenient to create an alias in the bash profile, something like:</small>

```bash
alias dokkabuild="rm -rf dokka-integration-tests/gradle/build/ui-showcase-result && export DOKKA_TEST_OUTPUT_PATH='build/ui-showcase-result' && ./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject"
```
<small>and then rerun only `dokkabuild`command in terminal</small>

1. Go to the plugin-base-frontend directory:
```bash 
cd dokka-subprojects/plugin-base-frontend
```
2. Run dev server for ui kit:
```bash
npm run start:ui-kit
```

3. Open the browser and go to http://localhost:8001

The dev server will watch for changes in the `plugin-base-frontend/` and rebuild the `ui-showcase` project automatically.
However, for the changes in html structure produced by kotlin templates one needs to rerun `dokkabuild` manually while there is no need to restart the dev server.

