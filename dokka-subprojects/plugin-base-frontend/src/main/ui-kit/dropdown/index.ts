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
      button.addEventListener('click', (event) => onToggleDropdown(event, dropdown));
    });
    addKeyboardNavigation(dropdown as HTMLElement);
  });
}

function onToggleDropdown(_: Event, dropdown: Element): void {
  const buttons = dropdown.querySelectorAll(DROPDOWN_TOGGLE);
  buttons?.forEach(toggleDropdownButton);
  const list = dropdown.querySelector(DROPDOWN_LIST);
  toggleDropdownList(list);
}

function toggleDropdownButton(button: Element): void {
  if (button.classList.contains('button_dropdown')) {
    button.classList.toggle('button_dropdown_active');
  }
}

function toggleDropdownList(list: Element | null): void {
  list?.classList.toggle('dropdown--list_expanded');
}

function handleOutsideClick(event: MouseEvent): void {
  const target = event.target as HTMLElement;
  if (!hasAncestorWithClass(target, 'dropdown') || target.id === 'filter-section-overlay') {
    const dropdowns = document.querySelectorAll('.button_dropdown');
    dropdowns.forEach((dropdown) => {
      dropdown.classList.remove('button_dropdown_active');
      dropdown.parentNode?.querySelector('.dropdown--list')?.classList.remove('dropdown--list_expanded');
    });
  }
}

function addKeyboardNavigation(dropdown: HTMLElement): void {
  new FocusTrap(dropdown);
  dropdown.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      onToggleDropdown(event, dropdown);
      (dropdown.querySelector(DROPDOWN_TOGGLE) as HTMLElement)?.focus();
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  initDropdowns();
  document.addEventListener('click', handleOutsideClick);
});
