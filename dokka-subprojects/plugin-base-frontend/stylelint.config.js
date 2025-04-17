/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

module.exports = {
  extends: '@jetbrains/stylelint-config',
  customSyntax: 'postcss-scss',
  rules: {
    'selector-max-specificity': ['0,2,0', { severity: 'warning' }],
    'media-query-no-invalid': null, // allow queries with scss variables as values
  },
};
