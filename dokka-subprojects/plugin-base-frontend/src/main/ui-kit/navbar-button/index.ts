/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { DROPDOWN_TOGGLED_EVENT, onToggleDropdown, TDropdownToggledDto } from '../dropdown';

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

function toggleTocDropdown(): void {
  const tocDropdown = document.getElementById('toc-dropdown');
  if (!tocDropdown) {
    console.warn('Dokka: toc dropdown is not found');
    return;
  }
  onToggleDropdown(tocDropdown);
}

document.addEventListener(DROPDOWN_TOGGLED_EVENT, (event) => {
  const { dropdownId, isExpanded } = (event as CustomEvent<TDropdownToggledDto>).detail;
  if (dropdownId === 'toc-dropdown') {
    if (isExpanded) {
      focusTocCloseButton();
      scrollActiveTocPartIntoView();
    } else {
      focusTocOpenButton();
    }
  }
});

function scrollActiveTocPartIntoView(): void {
  const activePart = document.querySelector('.toc--part[data-active="true"]')?.querySelector('.toc--link');
  if (activePart) {
    activePart.scrollIntoView({ block: 'center' });
  }
}

function focusTocCloseButton(): void {
  const tocCloseButton = document
    .getElementById('leftColumn')
    ?.querySelector('[data-role="dropdown-toggle"]') as HTMLElement;
  if (tocCloseButton) {
    tocCloseButton.focus();
  }
}

function focusTocOpenButton(): void {
  const tocOpenButton = document.getElementById('toc-toggle') as HTMLElement;
  if (tocOpenButton) {
    tocOpenButton.focus();
  }
}

// Needs to be exposed to the global scope to be called from the outside
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).toggleTocDropdown = toggleTocDropdown;

document.addEventListener('DOMContentLoaded', () => {
  initTocToggle();
});
