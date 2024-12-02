/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { onToggleDropdown } from '../dropdown';

function initTocToggle() {
  const tocToggle = document.getElementById('toc-toggle');
  const tocDropdown = document.getElementById('toc-dropdown');
  if (!tocToggle || !tocDropdown) {
    console.warn('Dokka: toc toggle or dropdown is not found');
    return;
  }
  tocToggle.addEventListener('click', (event) => {
    event.stopPropagation();
    onToggleDropdown(tocDropdown);
  });
}

export function toggleTocDropdown(): void {
  const tocDropdown = document.getElementById('toc-dropdown');
  if (!tocDropdown) {
    console.warn('Dokka: toc dropdown is not found');
    return;
  }
  onToggleDropdown(tocDropdown);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).toggleTocDropdown = toggleTocDropdown;

document.addEventListener('DOMContentLoaded', () => {
  initTocToggle();
});
