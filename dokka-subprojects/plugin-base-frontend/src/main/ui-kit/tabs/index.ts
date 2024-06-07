/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

/** When Dokka is viewed via iframe, local storage could be inaccessible (see https://github.com/Kotlin/dokka/issues/3323)
 * This is a wrapper around local storage to prevent errors in such cases
 * */
const safeLocalStorage = (() => {
  let isLocalStorageAvailable = false;
  try {
    const testKey = '__testLocalStorageKey__';
    localStorage.setItem(testKey, testKey);
    localStorage.removeItem(testKey);
    isLocalStorageAvailable = true;
  } catch (e) {
    console.error('Local storage is not available', e);
  }

  return {
    getItem: (key) => {
      if (!isLocalStorageAvailable) {
        return null;
      }
      return localStorage.getItem(key);
    },
    setItem: (key, value) => {
      if (!isLocalStorageAvailable) {
        return;
      }
      localStorage.setItem(key, value);
    }
  };
})();

export function initTabs() {
  // we could have only a single type of data - classlike or package
  const mainContent = document.querySelector('.main-content');
  const type = mainContent ? mainContent.getAttribute("data-page-type") : null;
  const localStorageKey = "active-tab-" + type;
  document.querySelectorAll('div[tabs-section]').forEach(element => {
    showCorrespondingTabBody(element);
    element.addEventListener('click', ({target}) => {
      const togglable = target ? target.getAttribute("data-togglable") : null;
      if (!togglable) return;

      safeLocalStorage.setItem(localStorageKey, JSON.stringify(togglable));
      toggleSections(target);
    });
  });

  const cached = safeLocalStorage.getItem(localStorageKey);
  if (!cached) return;

  const tab = document.querySelector(
    'div[tabs-section] > button[data-togglable="' + JSON.parse(cached) + '"]'
  );
  if (!tab) return;

  toggleSections(tab);
}

function showCorrespondingTabBody(element) {
  const buttonWithKey = element.querySelector("button[data-active]")
  if (buttonWithKey) {
    toggleSections(buttonWithKey)
  }
}

function toggleSections(target) {
  const activateTabs = (containerClass) => {
    for (const element of document.getElementsByClassName(containerClass)) {
      for (const child of element.children) {
        if (child.getAttribute("data-togglable") === target.getAttribute("data-togglable")) {
          child.setAttribute("data-active", "")
        } else {
          child.removeAttribute("data-active")
        }
      }
    }
  }
  const toggleTargets = target.getAttribute("data-togglable").split(",")
  const activateTabsBody = (containerClass) => {
    document.querySelectorAll("." + containerClass + " *[data-togglable]")
      .forEach(child => {
        if (toggleTargets.includes(child.getAttribute("data-togglable"))) {
          child.setAttribute("data-active", "")
        } else if (!child.classList.contains("sourceset-dependent-content")) { // data-togglable is used to switch source set as well, ignore it
          child.removeAttribute("data-active")
        }
      })
  }
  activateTabs("tabs-section")
  activateTabsBody("tabs-section-body")
}