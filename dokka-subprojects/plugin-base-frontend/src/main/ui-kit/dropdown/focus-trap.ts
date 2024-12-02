/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

export class FocusTrap {
  private trapElement: HTMLElement;

  constructor(trapElement: HTMLElement) {
    this.trapElement = trapElement;
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.trapElement.addEventListener('keydown', this.handleKeyDown);
  }

  private handleKeyDown(event: KeyboardEvent) {
    const navigationKeys = ['Tab', 'ArrowDown', 'ArrowUp'];
    const focusableElements = Array.from(this.trapElement.querySelectorAll<HTMLElement>('[role="option"]')).filter(
      (element) => element.style.display !== 'none' && element.tabIndex !== -1
    );
    if (!navigationKeys.includes(event.key) || focusableElements.length === 0) {
      return;
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

  public destroy() {
    this.trapElement.removeEventListener('keydown', this.handleKeyDown);
  }
}
