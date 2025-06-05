/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import List from '@jetbrains/ring-ui/components/list/list';
import Select, { SelectItem } from '@jetbrains/ring-ui/components/select/select';
import React, { useCallback, useEffect, useState } from 'react';
import '@jetbrains/ring-ui/components/input-size/input-size.css';
import './search.scss';
import { FocusTrap } from '../../ui-kit/dropdown/focus-trap';
import { relativizeUrlForRequest } from '../utils/requests';
import { DokkaFuzzyFilterComponent } from './dokkaFuzzyFilter';
import { DokkaSearchAnchor } from './dokkaSearchAnchor';
import { CustomAnchorProps, IWindow, Option, Props } from './types';

const WithFuzzySearchFilterComponent: React.FC<Props> = ({ data }: Props) => {
  const [selected, onSelected] = useState<Option>(data[0]);
  const onChangeSelected = useCallback((selectItem: SelectItem<unknown> | null) => {
    if (!selectItem) {
      return;
    }
    const maybeOption: Option = selectItem as Option;
    window.location.replace(`${(window as IWindow).pathToRoot}${maybeOption.location}?query=${maybeOption.name}`);
    onSelected(maybeOption);
  }, []);

  return (
    <div className="search-container">
      <div className="search">
        <DokkaFuzzyFilterComponent
          id="pages-search"
          selectedLabel="Search"
          label="Search"
          filter={true}
          type={Select.Type.CUSTOM}
          clear
          renderOptimization
          disableScrollToActive
          selected={selected}
          data={data}
          popupClassName={'popup-wrapper'}
          onSelect={onChangeSelected}
          maxHeight={510}
          customAnchor={({ wrapperProps, buttonProps, popup }: CustomAnchorProps) => (
            <DokkaSearchAnchor wrapperProps={wrapperProps} buttonProps={buttonProps} popup={popup} />
          )}
          onOpen={onSearchPopupOpen}
          onClose={onSearchPopupClose}
          onFilter={resetSearchOptionsListScrollPosition}
        />
      </div>
    </div>
  );
};

export const WithFuzzySearchFilter = () => {
  const [navigationList, setNavigationList] = useState<Option[]>([]);

  useEffect(() => {
    fetch(relativizeUrlForRequest('scripts/pages.json'))
      .then((response) => response.json())
      .then(
        (result) => {
          setNavigationList(
            result.map((record: Option, idx: number) => {
              return {
                ...record,
                label: record.name,
                key: idx,
                type: record.kind,
                rgItemType: List.ListProps.Type.CUSTOM,
              };
            })
          );
        },
        (error) => {
          console.error('failed to fetch pages data', error);
          setNavigationList([]);
        }
      );
  }, []);

  return <WithFuzzySearchFilterComponent data={navigationList} />;
};

let focusTrap: FocusTrap | null = null;

function onSearchPopupOpen() {
  const clearButton = document.querySelector('[data-test="ring-input-clear"]');
  if (clearButton) {
    const closeButton = createSearchPopupCloseButton();
    clearButton.after(closeButton);
    closeButton.addEventListener('click', handleCloseSearchPopupButtonClick);
    clearButton.addEventListener('click', handleClearButtonClick);
  }
  const inputContainer = document.querySelector('[data-test="ring-input"]');
  if (inputContainer) {
    inputContainer.addEventListener('keydown', handleInputKeyDown);
    focusTrap = new FocusTrap({
      trapElement: inputContainer as HTMLElement,
      interactiveElementsSelector: () =>
        '[data-test-custom="ring-select-popup-filter-input"], [data-test="ring-input-clear"], #search-close-button',
    });
  }
  document.body.style.overflow = 'hidden';
  initActiveSearchOptionObserver();
}

function createSearchPopupCloseButton() {
  const closeButton = document.createElement('button');
  closeButton.id = 'search-close-button';
  closeButton.className = 'button button_dropdown button_dropdown_active search--close-button';
  closeButton.setAttribute('aria-label', 'Close search popup');
  return closeButton;
}

function onSearchPopupClose() {
  document.body.style.overflow = '';
  destroyActiveSearchOptionsObserver();
}

function handleInputKeyDown(event: Event) {
  const { key } = event as KeyboardEvent;
  if (key === 'Tab') {
    // Prevent the ring ui component from closing the popup
    event.preventDefault();
    event.stopPropagation();
  }
  if (key === 'Enter' && (event.target as HTMLElement).tagName.toLowerCase() !== 'input') {
    event.stopPropagation();
  }
}

function handleClearButtonClick() {
  const inputElement = document.querySelector('[data-test-custom="ring-select-popup-filter-input"]');
  if (inputElement) {
    (inputElement as HTMLInputElement).focus();
  }
}

function handleCloseSearchPopupButtonClick(event: Event) {
  const target = event.target as HTMLElement;
  if (target?.id === 'search-close-button') {
    const inputContainer = document.querySelector('[data-test="ring-input"]');
    if (inputContainer) {
      inputContainer.removeEventListener('keydown', handleInputKeyDown);
    }
    const clearButton = document.querySelector('[data-test="ring-input-clear"]') as HTMLButtonElement;
    if (clearButton) {
      clearButton.removeEventListener('click', handleClearButtonClick);
    }
    const closeButton = document.getElementById('search-close-button') as HTMLButtonElement;
    if (closeButton) {
      closeButton.removeEventListener('click', handleCloseSearchPopupButtonClick);
    }
    if (focusTrap) {
      focusTrap.destroy();
      focusTrap = null;
    }
    const searchAnchor = document.getElementById('pages-search');
    if (searchAnchor) {
      searchAnchor.focus();
      // Closes the search popup
      searchAnchor.click();
    }
  }
}

let activeSearchOptionsObserver: MutationObserver | null = null;

function initActiveSearchOptionObserver() {
  destroyActiveSearchOptionsObserver();

  activeSearchOptionsObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      const searchOptionElement = mutation.target as HTMLElement;

      if (isSearchOptionActive(searchOptionElement) && !isSearchOptionVisible(searchOptionElement)) {
        searchOptionElement.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'center',
        });
      }
    });
  });

  const listWrapper = document.querySelector('.ReactVirtualized__Grid');

  if (!listWrapper || !activeSearchOptionsObserver) {
    return;
  }
  activeSearchOptionsObserver.observe(listWrapper, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['class'],
  });
}

function destroyActiveSearchOptionsObserver() {
  if (activeSearchOptionsObserver !== null) {
    activeSearchOptionsObserver.disconnect();
    activeSearchOptionsObserver = null;
  }
}

function resetSearchOptionsListScrollPosition() {
  const listWrapper = document.querySelector('.ReactVirtualized__Grid');
  if (listWrapper) {
    listWrapper.scrollTop = 0;
  }
}

function isSearchOptionActive(element: HTMLElement): boolean {
  return [...element.classList].some((className) => className.includes('hover'));
}

function isSearchOptionVisible(element: HTMLElement): boolean {
  const elementRect = element.getBoundingClientRect();
  const listWrapper = element.closest('.ReactVirtualized__Grid');

  if (!listWrapper) {
    return false;
  }

  const listRect = listWrapper.getBoundingClientRect();
  return elementRect.top >= listRect.top && elementRect.bottom <= listRect.bottom;
}
