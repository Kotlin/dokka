/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

export function hasAncestorWithClass(element: HTMLElement, className: string): boolean {
  if (element && element.classList.contains(className)) {
    return true;
  }
  if (element.parentElement) {
    return hasAncestorWithClass(element.parentElement as HTMLElement, className);
  }
  return false;
}

export const DESKTOP_MIN_WIDTH = 900;
export const TABLET_MIN_WIDTH = 440;

export type ScreenType = 'mobile' | 'tablet' | 'desktop';

export function isDesktop(): boolean {
  return window.innerWidth >= DESKTOP_MIN_WIDTH;
}

export function isTablet(): boolean {
  return window.innerWidth >= TABLET_MIN_WIDTH && window.innerWidth < DESKTOP_MIN_WIDTH;
}

export function isMobile(): boolean {
  return window.innerWidth < TABLET_MIN_WIDTH;
}

/** Returns the current screen type: 'mobile', 'tablet' or 'desktop' */
export function getScreenType(): ScreenType {
  return isMobile() ? 'mobile' : isTablet() ? 'tablet' : 'desktop';
}

export function isFocusableElement(element: HTMLElement): boolean {
  let currentElement: HTMLElement | null = element;
  if (currentElement.tabIndex === -1) {
    return false;
  }
  while (currentElement) {
    const computedStyle = getComputedStyle(currentElement);
    if (computedStyle.display === 'none' || computedStyle.visibility === 'hidden') {
      return false;
    }
    currentElement = currentElement.parentElement;
  }
  return true;
}

export function getActualScrollBarWidth(): number {
  const scrollDiv = document.createElement('div');
  scrollDiv.style.width = '100px';
  scrollDiv.style.height = '100px';
  scrollDiv.style.overflow = 'scroll';
  document.body.appendChild(scrollDiv);
  const width = scrollDiv.offsetWidth - scrollDiv.clientWidth;
  document.body.removeChild(scrollDiv);
  return width;
}

/**
 * This is used to remove styles that were added for backward compatibility,
 * for example, in the version selector component in which we have new markup
 * but only old CSS styles for previous versions are loaded
 */
export function removeBackwardCompatibilityStyles(): void {
  document.querySelectorAll('[data-remove-style="true"]').forEach((element: Element) => {
    element.removeAttribute('style');
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function debounce(func: (...args: any[]) => any, delay: number) {
  let timer: number | null = null;

  return function (this: unknown, ...args: unknown[]) {
    const onComplete = () => {
      func.apply(this, args);
      timer = null;
    };

    if (timer) {
      clearTimeout(timer);
    }

    timer = setTimeout(onComplete, delay);
  };
}
