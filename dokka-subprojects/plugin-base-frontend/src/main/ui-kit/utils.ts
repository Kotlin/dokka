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

export function getScreenType(): ScreenType {
  return isMobile() ? 'mobile' : isTablet() ? 'tablet' : 'desktop';
}
