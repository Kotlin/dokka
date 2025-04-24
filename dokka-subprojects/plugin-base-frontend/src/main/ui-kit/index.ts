/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
export * as breadcrumbs from './breadcrumbs/index';
export * as button from './button/index';
export * as checkbox from './checkbox/index';
export * as codeBlock from './code-block/index';
export * as copyTooltip from './copy-tooltip/index';
export * as dropdown from './dropdown/index';
export * as filterSection from './filter-section/index';
export * as footer from './footer/index';
export * as icon from './icon/index';
export * as inlineCode from './inline-code/index';
export * as layout from './layout/index';
export * as libraryName from './library-name/index';
export * as libraryVersion from './library-version/index';
export * as link from './link/index';
export * as navbar from './navbar/index';
export * as navbarButton from './navbar-button/index';
export * as platformTag from './platform-tag/index';
export * as platformTags from './platform-tags/index';
export * as tabs from './tabs/index';
export * as tocTree from './toc-tree/index';
import { removeBackwardCompatibilityStyles } from './utils';
import './helpers.scss';
import './global.scss';

import '@fontsource/inter/latin-400.css';
import '@fontsource/inter/latin-600.css';
import '@fontsource/jetbrains-mono/latin-400.css';
import '@fontsource/jetbrains-mono/latin-600.css';

document.addEventListener('DOMContentLoaded', () => {
  removeBackwardCompatibilityStyles();
});
