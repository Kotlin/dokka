import fs from 'fs';

const firstLetterToLoweCase = str => str.charAt(0).toLowerCase() + str.slice(1);

const kebabToCamelCase = str => str.replace(/-./g, x => x.toUpperCase()[1]);

const uiKitPath = 'src/main/ui-kit';
const componentName = process.argv[2];
const componentPath = `${uiKitPath}/${componentName}`;
const lowerCaseComponentName = firstLetterToLoweCase(componentName);
const lowerCaseComponentNameCamelCase = firstLetterToLoweCase(kebabToCamelCase(componentName));

const uiKitIndexTsFile = `${uiKitPath}/index.ts`;
const componentIndexTsFile = `${componentPath}/index.ts`;
const componentScssFile = `${componentPath}/styles.scss`;

const currentYear = new Date().getFullYear();

const componentIndexTsFileContent = `/*
 * Copyright 2014-${currentYear} JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
`;

const componentScssFileContent = `/*!
 * Copyright 2014-${currentYear}  JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@import '../_tokens/index';

.${lowerCaseComponentName} {
}
`;

const uiKitIndexTsFileContent = `import * as ${lowerCaseComponentNameCamelCase} from './${componentName}/index';
`;

fs.mkdir(componentPath, error => {
  if (error) {
    throw error;
  }

  const pathToContentMap = {
    [componentIndexTsFile]: componentIndexTsFileContent,
    [componentScssFile]: componentScssFileContent,
    [uiKitIndexTsFile]: uiKitIndexTsFileContent,
  };

  Object.keys(pathToContentMap).forEach((path) => {
    fs.appendFile(path, pathToContentMap[path], function (err) {
      if (err) {
        return console.error(err);
      }
      console.log(`${path} updated successfully`);
    });
  });

  console.log(`Component ${componentPath} created successfully`);
});
