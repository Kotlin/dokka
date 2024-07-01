/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

module.exports = {
  "src/**/**/*.{ts,tsx}": [
    "eslint --fix",
    () => "tsc --noEmit"
  ],
  "src/**/**/*.{ts,tsx,css,scss}": [
    "prettier --write"
  ]
}
