/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
const TOC_STATE_KEY_PREFIX = 'TOC_STATE::';
const TOC_CONTAINER_ID = 'sideMenu';
const TOC_SCROLL_CONTAINER_ID = 'leftColumn';
const TOC_PART_CLASS = 'toc--part';
const TOC_PART_HIDDEN_CLASS = 'toc--part_hidden';
const TOC_LINK_CLASS = 'toc--link';
const TOC_SKIP_LINK_CLASS = 'toc--skip-link';

(function () {
  function displayToc() {
    fetch(pathToRoot + 'navigation.html')
      .then((response) => response.text())
      .then((tocHTML) => {
        renderToc(tocHTML);
        updateTocLinks();
        collapseTocParts();
        expandTocPathToCurrentPage();
        restoreTocExpandedState();
        restoreTocScrollTop();
      });
  }

  function renderToc(tocHTML) {
    const containerElement = document.getElementById(TOC_CONTAINER_ID);
    if (containerElement) {
      containerElement.innerHTML = tocHTML;
    }
  }

  function updateTocLinks() {
    document.querySelectorAll(`.${TOC_LINK_CLASS}`).forEach((tocLink) => {
      tocLink.setAttribute('href', `${pathToRoot}${tocLink.getAttribute('href')}`);
      tocLink.addEventListener('keydown', preventScrollBySpaceKey);
    });
    document.querySelectorAll(`.${TOC_SKIP_LINK_CLASS}`).forEach((skipLink) => {
      skipLink.setAttribute('href', `#main`);
      skipLink.addEventListener('keydown', preventScrollBySpaceKey);
    })
  }

  function collapseTocParts() {
    document.querySelectorAll(`.${TOC_PART_CLASS}`).forEach((tocPart) => {
      if (!tocPart.classList.contains(TOC_PART_HIDDEN_CLASS)) {
        tocPart.classList.add(TOC_PART_HIDDEN_CLASS);
        const tocToggleButton = tocPart.querySelector('button');
        if (tocToggleButton) {
          tocToggleButton.setAttribute("aria-expanded", "false");
        }
      }
    });
  }

  const expandTocPathToCurrentPage = () => {
    const tocParts = [...document.querySelectorAll(`.${TOC_PART_CLASS}`)];
    const currentPageId = document.getElementById('content')?.getAttribute('pageIds');
    if (!currentPageId) {
      return;
    }

    let isPartFound = false;
    let currentPageIdPrefix = currentPageId;
    while (!isPartFound && currentPageIdPrefix !== '') {
      tocParts.forEach((part) => {
        const partId = part.getAttribute('pageId');
        if (!isPartFound && partId?.includes(currentPageIdPrefix)) {
          isPartFound = true;
          expandTocPart(part);
          expandTocPathToParent(part);
          part.dataset.active = 'true';
        }
      });
      currentPageIdPrefix = currentPageIdPrefix.substring(0, currentPageIdPrefix.lastIndexOf('/'));
    }
  };

  const expandTocPathToParent = (part) => {
    if (part.classList.contains(TOC_PART_CLASS)) {
      expandTocPart(part);
      expandTocPathToParent(part.parentNode);
    }
  };

  const expandTocPart = (tocPart) => {
    if (tocPart.classList.contains(TOC_PART_HIDDEN_CLASS)) {
      tocPart.classList.remove(TOC_PART_HIDDEN_CLASS);
      const tocToggleButton = tocPart.querySelector('button');
      if (tocToggleButton) {
        tocToggleButton.setAttribute("aria-expanded", "true");
      }
      const tocPartId = tocPart.getAttribute('id');
      safeLocalStorage.setItem(`${TOC_STATE_KEY_PREFIX}${tocPartId}`, 'true');
    }
  };

  /**
   * Restores the state of the navigation tree from the local storage.
   * LocalStorage keys are in the format of `TOC_STATE::${id}` where `id` is the id of the part
   */
  const restoreTocExpandedState = () => {
    const allLocalStorageKeys = safeLocalStorage.getKeys();
    const tocStateKeys = allLocalStorageKeys.filter((key) => key.startsWith(TOC_STATE_KEY_PREFIX));
    tocStateKeys.forEach((key) => {
      const isExpandedTOCPart = safeLocalStorage.getItem(key) === 'true';
      const tocPartId = key.substring(TOC_STATE_KEY_PREFIX.length);
      const tocPart = document.querySelector(`.toc--part[id="${tocPartId}"]`);
      if (tocPart !== null && isExpandedTOCPart) {
        tocPart.classList.remove(TOC_PART_HIDDEN_CLASS);
        const tocToggleButton = tocPart.querySelector('button');
        if (tocToggleButton) {
          tocToggleButton.setAttribute("aria-expanded", "true");
        }
      }
    });
  };

  function saveTocScrollTop() {
    const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
    if (container) {
      const currentScrollTop = container.scrollTop;
      safeLocalStorage.setItem(`${TOC_STATE_KEY_PREFIX}SCROLL_TOP`, `${currentScrollTop}`);
    }
  }

  function restoreTocScrollTop() {
    const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
    if (container) {
      const storedScrollTop = safeLocalStorage.getItem(`${TOC_STATE_KEY_PREFIX}SCROLL_TOP`);
      if (storedScrollTop) {
        container.scrollTop = Number(storedScrollTop);
      }
    }
  }

  function initTocScrollListener() {
    const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
    if (container) {
      container.addEventListener('scroll', saveTocScrollTop);
    }
  }

  function preventScrollBySpaceKey(event) {
    if (event.key === ' ') {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    displayToc();
    initTocScrollListener();
  });
})();

function handleTocButtonClick(event, navId) {
  const tocPart = document.getElementById(navId);
  if (!tocPart) {
    return;
  }
  tocPart.classList.toggle(TOC_PART_HIDDEN_CLASS);
  const isExpandedTOCPart = !tocPart.classList.contains(TOC_PART_HIDDEN_CLASS);
  const button = tocPart.querySelector('button');
  button?.setAttribute("aria-expanded", `${isExpandedTOCPart}`);
  safeLocalStorage.setItem(`${TOC_STATE_KEY_PREFIX}${navId}`, `${isExpandedTOCPart}`);
}
