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
    if (event.key === 'Tab') {
      const focusableElements = Array.from(this.trapElement.querySelectorAll<HTMLElement>('[role="option"]')).filter(
        (element) => element.style.display !== 'none' && element.tabIndex !== -1
      );
      if (!focusableElements.length) {
        return;
      }

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

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
