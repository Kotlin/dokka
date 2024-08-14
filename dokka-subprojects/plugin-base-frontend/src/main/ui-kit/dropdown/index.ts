/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { hasAncestorWithClass } from '../utils';

// page objects selectors
const DROPDOWN = '[data-role="dropdown"]';
const DROPDOWN_TOGGLE = '[data-role="dropdown-toggle"]';
const DROPDOWN_LIST = '[data-role="dropdown-listbox"]';

function initDropdowns(): void {
  const dropdowns = document.querySelectorAll(DROPDOWN);
  dropdowns.forEach((dropdown: Element) =>
    dropdown.querySelectorAll(DROPDOWN_TOGGLE)?.forEach((button: Element) => {
      button.addEventListener('click', (event) => onToggleDropdown(event, dropdown));
    })
  );
}

function onToggleDropdown(event: Event, dropdown: Element): void {
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

document.addEventListener('DOMContentLoaded', () => {
  initDropdowns();

  document.addEventListener('click', (event) => {
    const target = event.target as HTMLElement;
    if (!hasAncestorWithClass(target, 'dropdown') && target.id !== 'platform-tags-toggle') {
      const dropdowns = document.querySelectorAll('.button_dropdown');
      dropdowns.forEach((dropdown) => {
        dropdown.classList.remove('button_dropdown_active');
        dropdown.parentNode?.querySelector('.dropdown--list')?.classList.remove('dropdown--list_expanded');
      });
    }
  });
});

function onToggleOption(event: PointerEvent): void {
  const target = event.target as HTMLButtonElement;
  target.classList.toggle('dropdown--option_active');
  target.querySelector('.dropdown--checkbox')?.toggleAttribute('checked');
}

function onToggleOptionByKey(event: KeyboardEvent): void {
  const target = event.target as HTMLButtonElement;
  if (event.key === 'Enter' || event.key === ' ') {
    target.classList.toggle('dropdown--option_active');
    target.querySelector('.dropdown--checkbox')?.toggleAttribute('checked');
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).onToggleOption = onToggleOption;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).onToggleOptionByKey = onToggleOptionByKey;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).initDropdowns = initDropdowns;
