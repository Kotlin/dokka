/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import { isDesktop, isFocusableElement } from '../utils';

export class FocusTrap {
  // Element that contains the focus inside itself
  private trapElement: HTMLElement;
  // The focus will be moved only between elements that match this selector,
  // it may differ depending on the screen size, that's why it is a function
  private getInteractiveElementsSelector: () => string = () =>
    'button, a, input, textarea, select, [tabindex]:not([tabindex="-1"])';
  // Keys that allow navigation through the focusable elements
  private navigationKeys: string[] = ['Tab'];

  constructor({
    trapElement,
    navigationKeys,
    interactiveElementsSelector,
  }: {
    trapElement: HTMLElement;
    navigationKeys?: string[];
    interactiveElementsSelector?: () => string;
  }) {
    this.trapElement = trapElement;
    if (navigationKeys) {
      this.navigationKeys = navigationKeys;
    }
    if (interactiveElementsSelector) {
      this.getInteractiveElementsSelector = interactiveElementsSelector;
    }
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.trapElement.addEventListener('keydown', this.handleKeyDown);
  }

  private handleKeyDown(event: KeyboardEvent) {
    const focusableElements = Array.from(
      this.trapElement.querySelectorAll<HTMLElement>(this.getInteractiveElementsSelector())
    ).filter(isFocusableElement);
    if (!this.navigationKeys.includes(event.key) || focusableElements.length === 0) {
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
      const activeElementIndex = focusableElements.findIndex(
        (element) => element && document.activeElement === element
      );

      if (activeElementIndex !== -1) {
        const nextElementIndex = event.shiftKey
          ? (activeElementIndex - 1 + focusableElements.length) % focusableElements.length
          : (activeElementIndex + 1) % focusableElements.length;
        (focusableElements[nextElementIndex] as HTMLElement).focus();
        event.preventDefault();
      }
    }
  }

  destroy() {
    this.trapElement.removeEventListener('keydown', this.handleKeyDown);
  }
}
