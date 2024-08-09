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
