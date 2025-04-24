/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import List from '@jetbrains/ring-ui/components/list/list';
import Select, { SelectItem } from '@jetbrains/ring-ui/components/select/select';
import React, { useCallback, useEffect, useState } from 'react';
import '@jetbrains/ring-ui/components/input-size/input-size.css';
import './search.scss';
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

  useEffect(() => {
    addCloseSearchPopupButtonEventListener();
    return removeCloseSearchPopupButtonEventListener;
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

function addCloseSearchPopupButtonEventListener() {
  document.addEventListener('click', handleCloseSearchPopupButtonClick, true);
}

function removeCloseSearchPopupButtonEventListener() {
  document.removeEventListener('click', handleCloseSearchPopupButtonClick, true);
}

function handleCloseSearchPopupButtonClick(event: Event) {
  const target = event.target as HTMLElement;
  if (target?.id === 'search-close-button') {
    document.getElementById('pages-search')?.click();
  }
}

function onSearchPopupOpen() {
  const cleatButton = document.querySelector('[data-test="ring-input-clear"]');
  if (cleatButton) {
    const closeButton = document.createElement('button');
    closeButton.id = 'search-close-button';
    closeButton.className = 'button button_dropdown button_dropdown_active search--close-button';
    closeButton.setAttribute('aria-label', 'Close search popup');
    cleatButton.after(closeButton);
  }
  document.body.style.overflow = 'hidden';
}

function onSearchPopupClose() {
  document.body.style.overflow = '';
}
