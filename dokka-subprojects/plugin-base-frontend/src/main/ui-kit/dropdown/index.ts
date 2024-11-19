/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { hasAncestorWithClass } from '../utils';
import { FocusTrap } from './focus-trap';

// page objects selectors
const DROPDOWN = '[data-role="dropdown"]';
const DROPDOWN_TOGGLE = '[data-role="dropdown-toggle"]';
const DROPDOWN_LIST = '[data-role="dropdown-listbox"]';

function initDropdowns(): void {
  const dropdowns = document.querySelectorAll(DROPDOWN);
  dropdowns.forEach((dropdown: Element) => {
    dropdown.querySelectorAll(DROPDOWN_TOGGLE)?.forEach((button: Element) => {
      button.addEventListener('click', () => onToggleDropdown(dropdown));
    });
    addKeyboardNavigation(dropdown as HTMLElement);
  });
}

export function onToggleDropdown(dropdown: Element): void {
  const buttons = dropdown.querySelectorAll(DROPDOWN_TOGGLE);
  buttons?.forEach(toggleDropdownButton);
  const list = dropdown.querySelector(DROPDOWN_LIST);
  const buttonWidth = (buttons[0] as HTMLElement).offsetWidth;
  toggleDropdownList(list, buttonWidth);
}

function toggleDropdownButton(button: Element): void {
  if (button.classList.contains('button_dropdown')) {
    button.classList.toggle('button_dropdown_active');
  }
}

function toggleDropdownList(list: Element | null, minWidth?: number): void {
  list?.classList.toggle('dropdown--list_expanded');
  if (minWidth) {
    (list as HTMLElement).style.minWidth = `${minWidth}px`;
  } else {
    (list as HTMLElement).style.minWidth = 'auto';
  }
}

function handleOutsideClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!hasAncestorWithClass(target, 'dropdown') || target.className === 'dropdown--overlay') {
    const dropdowns = document.querySelectorAll(DROPDOWN);
    dropdowns.forEach((dropdown) => {
      dropdown.querySelectorAll(DROPDOWN_TOGGLE)?.forEach((button: Element) => {
        button.classList.remove('button_dropdown_active');
      });
      dropdown.querySelectorAll(DROPDOWN_LIST)?.forEach((list: Element) => {
        list.classList.remove('dropdown--list_expanded');
      });
    });
  }
}

function addKeyboardNavigation(dropdown: HTMLElement): void {
  new FocusTrap(dropdown);
  dropdown.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      onToggleDropdown(dropdown);
      (dropdown.querySelector(DROPDOWN_TOGGLE) as HTMLElement)?.focus();
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  initDropdowns();
  document.addEventListener('click', handleOutsideClick);
});
