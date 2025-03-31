/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import { isDesktop, isFocusableElement } from '../utils';

export class FocusTrap {
  private trapElement: HTMLElement;

  constructor(trapElement: HTMLElement) {
    this.trapElement = trapElement;
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.trapElement.addEventListener('keydown', this.handleKeyDown);
  }

  private handleKeyDown(event: KeyboardEvent) {
    const navigationKeys = ['Tab', 'ArrowDown', 'ArrowUp'];
    /**
     * On desktop we only deal with options in the dropdown lists,
     * but on mobile and tablet ToC also behaves like a dropdown
     * */
    const trappedElementsSelector = isDesktop() ? '[role="option"]' : '[role="option"], .toc--link, .toc--button';
    const focusableElements = Array.from(
      this.trapElement.querySelectorAll<HTMLElement>(trappedElementsSelector)
    ).filter(isFocusableElement);
    if (!navigationKeys.includes(event.key) || focusableElements.length === 0) {
      return;
    }

    const dropdownToggles = Array.from(
      this.trapElement.querySelectorAll<HTMLElement>('[data-role="dropdown-toggle"]')
    ).filter(isFocusableElement);
    /**
     * We have two dropdown toggles for each dropdown:
     * one is the regular dropdown toggle button,
     * the other is the "close" button in the modal,
     * which is present only on tablet and mobile.
     * So when the modal is open, only the "close" button is accessible.
     * There is only one exception with the ToC modal on tablet
     * and mobile — in this case there is only one toggle inside the dropdown component.
     * */
    const shownDropdownToggle: HTMLElement | undefined =
      isDesktop() || dropdownToggles.length === 1 ? dropdownToggles[0] : dropdownToggles[1];
    if (shownDropdownToggle) {
      focusableElements.unshift(shownDropdownToggle);
    }

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.key === 'ArrowUp') {
      if (document.activeElement === firstElement) {
        lastElement.focus();
      } else {
        const currentIndex = focusableElements.indexOf(document.activeElement as HTMLElement);
        focusableElements[currentIndex - 1].focus();
      }
    }

    if (event.key === 'ArrowDown') {
      if (document.activeElement === lastElement) {
        firstElement.focus();
      } else {
        const currentIndex = focusableElements.indexOf(document.activeElement as HTMLElement);
        focusableElements[currentIndex + 1].focus();
      }
    }

    if (event.key === 'Tab') {
      if (event.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement.focus();
          event.preventDefault();
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement.focus();
          event.preventDefault();
        }
      }
    }
  }
}
