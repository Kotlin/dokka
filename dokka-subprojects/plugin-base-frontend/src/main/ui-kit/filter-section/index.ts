/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

const DESKTOP_MIN_WIDTH = 900;
const TAGS_MARGIN = 4;
const DROPDOWN_BUTTON_WIDTH = 40;

/**
 * Filter section items are tags with platform names, they should fit in one line.
 * If there is not enough space, extra tags should be hidden and dropdown options should be shown instead.
 * */
type FilterSectionItem = {
  tag: Element;
  option: Element;
};

function displayItemAsTag(item: FilterSectionItem): void {
  item.tag.removeAttribute('style');
  item.option.setAttribute('style', 'display: none');
}

function displayItemAsOption(item: FilterSectionItem): void {
  item.tag.setAttribute('style', 'display: none');
  item.option.removeAttribute('style');
}

function initFilterSection(): void {
  const navigation = document.getElementById('navigation-wrapper');
  const libraryVersion = document.getElementById('library-version');
  const filterSection = document.getElementById('filter-section');
  const firstButtonAfterFilterSection = document.querySelector('#filter-section + .navigation-controls--btn');
  const dropdownButton = document.getElementById('filter-section-dropdown');

  if (!navigation || !libraryVersion || !filterSection || !firstButtonAfterFilterSection || !dropdownButton) {
    console.error('Navbar elements are not found');
    return;
  }

  const options = filterSection?.querySelectorAll('.dropdown--option');
  const tags = filterSection?.querySelectorAll('.platform-selector');

  if (!tags || !options) {
    console.error('Filter section items are not found');
    return;
  }
  if (tags.length !== options.length) {
    console.error('Filter section items are not equal');
    return;
  }

  const items: FilterSectionItem[] = Array.from({ length: tags.length }).map((_, index) => ({
    tag: tags[index],
    option: options[index],
  }));

  /**
   * Saved widths of each tag while they were visible.
   * */
  const tagsWidths: number[] = items.map(({ tag }) => tag.getBoundingClientRect().width);

  /**
   * According to the design, filter section tags should fit between library version and navigation buttons.
   */
  function getAvailableWidthForFilterSection(): number {
    if (!libraryVersion || !firstButtonAfterFilterSection) {
      return 0;
    }
    return firstButtonAfterFilterSection.getBoundingClientRect().left - libraryVersion.getBoundingClientRect().right;
  }

  /**
   * If there is not enough space for all tags, the last tag should be hidden and displayed as a dropdown option.
   * But on narrow screens, all tags should be displayed as dropdown options.
   */
  function displayFilterSectionItems(): void {
    if (!navigation || !dropdownButton) {
      return;
    }
    const navigationWidth = navigation.getBoundingClientRect().width;
    if (navigationWidth < DESKTOP_MIN_WIDTH) {
      items.forEach(displayItemAsOption);
      dropdownButton.removeAttribute('style');
      return;
    }
    const availableWidth = getAvailableWidthForFilterSection() - DROPDOWN_BUTTON_WIDTH;
    let accumulatedWidth = 0;
    dropdownButton.setAttribute('style', 'display: none');
    items.forEach((item, index) => {
      accumulatedWidth += tagsWidths[index] + TAGS_MARGIN;
      if (accumulatedWidth < availableWidth) {
        displayItemAsTag(item);
      } else {
        displayItemAsOption(item);
        dropdownButton.removeAttribute('style');
      }
    });
  }

  const resizeObserver = new ResizeObserver(() => {
    displayFilterSectionItems();
    resizeObserver.unobserve(navigation);
  });

  const initResizeObserver = (): void => {
    resizeObserver.observe(navigation);
  };

  function addOptionEventListener(): void {
    options.forEach((option) => {
      option.addEventListener('click', (event) => {
        toggleFilterForOption(event.target as Element);
      });
      option.addEventListener('keydown', (event) => {
        const key = (event as KeyboardEvent).key;
        if (key === 'Enter' || key === ' ') {
          toggleFilterForOption(event.target as Element);
        }
      });
    });
  }

  displayFilterSectionItems();
  initResizeObserver();
  addOptionEventListener();
  window.addEventListener('resize', initResizeObserver);
}

document.addEventListener('DOMContentLoaded', initFilterSection);

declare global {
  const filteringContext: {
    activeFilters: (string | null | undefined)[];
  };

  function refreshFiltering(): void;
}

/**
 * This syncs platform tags and dropdown options filtering behavior.
 */
function toggleFilterForOption(option: Element): void {
  const dataFilter = option.querySelector('.checkbox--input')?.getAttribute('data-filter');
  const index = filteringContext.activeFilters.findIndex((item) => item === dataFilter);
  if (index === -1) {
    filteringContext.activeFilters.push(dataFilter);
  } else {
    filteringContext.activeFilters.splice(index, 1);
  }
  refreshFiltering();
}
