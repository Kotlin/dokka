### How to use frontend dev-server:

1. Prebuild Dokka's output for the ui-showcase project:
   
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

2. Go to the plugin-base-frontend directory:
   ```bash 
   cd dokka-subprojects/plugin-base-frontend
   ```
3. Run dev server for ui kit:
   ```bash
   npm run start:ui-kit
   ```

4. Open the browser and go to http://localhost:8001

   The dev server will watch for changes in the `plugin-base-frontend/` and rebuild the `ui-showcase` project automatically.
   However, for the changes in html structure produced by kotlin templates one needs to rerun `dokkabuild` manually while there is no need to restart the dev server.


### How to create a new UI kit component:

1. Run `npm run create-component -- <component-name-in-kebab-case>`

    It will create all necessary files templates in `src/main/ui-kit/` directory and import them in `src/main/ui-kit/index.ts` and `src/main/ui-kit/index.scss` files.

2. Export component manually from `src/main/ui-kit/index.ts` file to make in available for webpack
