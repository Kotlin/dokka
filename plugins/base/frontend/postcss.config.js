/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

module.exports = () => ({
  plugins: [
    require('postcss-import'),
    require('postcss-preset-env')({
      features: {
        stage: 3, // See https://cssdb.org/#staging-process
        features: {
          'nesting-rules': true,
          'custom-properties': {
            preserve: true
          }
        }
      }
    })
  ]
});
