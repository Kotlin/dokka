/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/** When Dokka is viewed via iframe, local storage could be inaccessible (see https://github.com/Kotlin/dokka/issues/3323)
 * The wrapper around local storage to prevent errors in such cases is defined in the plugin-base scripts assets
 * */
declare const safeLocalStorage: {
  getItem: (key: string) => string | null;
  setItem: (key: string, value: string) => void;
  removeItem: (key: string) => void;
  getKeys: () => string[];
};

/** When Dokka is viewed via iframe, session storage could be inaccessible (see https://github.com/Kotlin/dokka/issues/3323)
 * The wrapper around session storage to prevent errors in such cases is defined in the plugin-base scripts assets
 * */
declare const safeSessionStorage: {
  getItem: (key: string) => string | null;
  setItem: (key: string, value: string) => void;
  removeItem: (key: string) => void;
  getKeys: () => string[];
};
