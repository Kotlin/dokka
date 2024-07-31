/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

module.exports = () => ({
  plugins: [
    require('postcss-import'),
    require('postcss-preset-env')
  ]
});
