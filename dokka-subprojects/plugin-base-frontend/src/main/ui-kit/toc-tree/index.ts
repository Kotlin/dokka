/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';
import { safeLocalStorage } from '../safeLocalStorage';

declare const pathToRoot: string;

const TOC_STATE_KEY_PREFIX = 'TOC_STATE::';
const TOC_CONTAINER_ID = 'sideMenu';
const TOC_PART_HIDDEN_CLASS = 'toc--part_hidden';
const TOC_PART_CLASS = 'toc--part';
const TOC_LINK_CLASS = 'toc--link';

const displayTableOfContents = () => {
  fetch(pathToRoot + 'navigation.html')
    .then((response) => response.text())
    .then((tableOfContentsHTML) => {
      renderTOC(tableOfContentsHTML);
      updateTOCLinks();
      collapseTOCParts();
      expandPathToCurrentPageInTOC();
      restoreTOCExpandedState();
      scrollTOCToSelectedElement(); // TODO call also when TOC modal is opened
    });
};

// TODO save expanded/collapsed state to local storage not only when clicking but when navigating and revealing parents items

// TODO save navigation position to local storage and restore it when navigating back

function renderTOC(tableOfContentsHTML: string) {
  const containerElement = document.getElementById(TOC_CONTAINER_ID);
  if (containerElement) {
    containerElement.innerHTML = tableOfContentsHTML;
  }
}

function updateTOCLinks() {
  document.querySelectorAll(`.${TOC_LINK_CLASS}`).forEach((tocLink) => {
    tocLink.setAttribute('href', `${pathToRoot}${tocLink.getAttribute('href')}`);
  });
}

function collapseTOCParts() {
  document.querySelectorAll(`.${TOC_PART_CLASS}`).forEach((tocPart) => {
    if (!tocPart.classList.contains(TOC_PART_HIDDEN_CLASS)) {
      tocPart.classList.add(TOC_PART_HIDDEN_CLASS);
    }
  });
}

const expandPathToCurrentPageInTOC = () => {
  const tocParts = [...document.querySelectorAll(`.${TOC_PART_CLASS}`)] as HTMLElement[];
  const currentPageId = document.getElementById('content')?.getAttribute('pageIds');
  if (!currentPageId) {
    return;
  }

  let isPartFound = false;
  let currentPageIdPrefix = currentPageId;
  while (!isPartFound && currentPageIdPrefix !== '') {
    tocParts.forEach((part: HTMLElement) => {
      const partId = part.getAttribute('pageId');
      if (!isPartFound && partId?.includes(currentPageIdPrefix)) {
        isPartFound = true;
        if (part.classList.contains(TOC_PART_HIDDEN_CLASS)) {
          part.classList.remove(TOC_PART_HIDDEN_CLASS);
          part.dataset.active = 'true';
        }
        expandParentPartInTOC(part);
      }
    });
    currentPageIdPrefix = currentPageIdPrefix.substring(0, currentPageIdPrefix.lastIndexOf('/'));
  }
};

const expandParentPartInTOC = (part: HTMLElement) => {
  if (part.classList.contains(TOC_PART_CLASS)) {
    if (part.classList.contains(TOC_PART_HIDDEN_CLASS)) {
      part.classList.remove(TOC_PART_HIDDEN_CLASS);
    }
    expandParentPartInTOC(part.parentNode as HTMLElement);
  }
};

const scrollTOCToSelectedElement = () => {
  const selectedElement = document.querySelector('div.toc--part[data-active]');
  if (selectedElement === null) {
    // nothing selected, probably just the main page opened
    return;
  }
  const hasIcon = selectedElement.querySelectorAll(':scope > div.toc--row span.toc--icon').length > 0;

  // for an instance enums also have children and are expandable but are not package/module elements
  const isPackageElement = selectedElement.children.length > 1 && !hasIcon;
  if (isPackageElement) {
    // if a package is selected or linked, it makes sense to align it to top
    // so that you can see all the members it contains
    selectedElement.scrollIntoView(true);
  } else {
    // if a member within a package is linked, it makes sense to center it since it,
    // this should make it easier to look at surrounding members
    selectedElement.scrollIntoView({
      behavior: 'auto',
      block: 'center',
      inline: 'center',
    });
  }
};

/**
 * Restores the state of the navigation tree from the local storage.
 * LocalStorage keys are in the format of `TOC_STATE::${id}` where `id` is the id of the part
 */
const restoreTOCExpandedState = () => {
  const allLocalStorageKeys = safeLocalStorage.getKeys();
  const tocStateKeys = allLocalStorageKeys.filter((key) => key.startsWith(TOC_STATE_KEY_PREFIX));
  tocStateKeys.forEach((key) => {
    const isExpandedTOCPart = safeLocalStorage.getItem(key) === 'true';
    const tocPartId = key.substring(TOC_STATE_KEY_PREFIX.length);
    const tocPart = document.querySelector(`.toc--part[id="${tocPartId}"]`);
    if (tocPart !== null && isExpandedTOCPart) {
      tocPart.classList.remove(TOC_PART_HIDDEN_CLASS);
    }
  });
};

function handleTOCButtonClick(event: Event, navId: string) {
  const button = document.getElementById(navId);
  if (!button) {
    return;
  }
  button.classList.toggle(TOC_PART_HIDDEN_CLASS);
  const isExpandedTOCPart = !button.classList.contains(TOC_PART_HIDDEN_CLASS);
  safeLocalStorage.setItem(`${TOC_STATE_KEY_PREFIX}${navId}`, `${isExpandedTOCPart}`);
}

// Needs to be exposed to the global scope to be accessible from the HTML
// eslint-disable-next-line @typescript-eslint/no-explicit-any
(window as any).handleTOCButtonClick = handleTOCButtonClick;

/*
    This is a work-around for safari being IE of our times.
    It doesn't fire a DOMContentLoaded, presumably because eventListener is added after it wants to do it
*/
if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', displayTableOfContents);
} else {
  displayTableOfContents();
}
