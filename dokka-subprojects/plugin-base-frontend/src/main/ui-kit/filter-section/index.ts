/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

// resize observer code for checking if .filter-section overflows nav tag when the window is resized
const filterSection = document.getElementById('filter-section');
const navigation = document.getElementById('navigation-wrapper');
const libraryVersion = document.getElementById('library-version');

const tags = filterSection?.querySelectorAll('.platform-selector');
const options = filterSection?.querySelectorAll('.dropdown--option');

if (options) {
  options.forEach((option) => {
    option.setAttribute('style', 'display: none');
  });
}

const tagsWidths: number[] = [];
if (tags) {
  tags.forEach((tag) => {
    tagsWidths.push(tag.getBoundingClientRect().width);
  });
}

let currentHiddenTagIndex = tags!.length;

console.log('tagsWidths', tagsWidths);

const resizeObserver = new ResizeObserver(() => {
  console.log('resizeObserver');
  if (!filterSection || !navigation || !libraryVersion) {
    return;
  }
  const navigationWidth = navigation.getBoundingClientRect().width;
  if (navigationWidth < 900) {
    options?.forEach((option) => {
      option.removeAttribute('style');
    });
    resizeObserver.unobserve(navigation);
    return;
  }

  const filterSectionLeft = filterSection.getBoundingClientRect().left;
  const libraryVersionRight = libraryVersion.getBoundingClientRect().right;
  const distance = filterSectionLeft - libraryVersionRight;

  console.log('distance', distance);

  if (distance <= 1) {
    currentHiddenTagIndex--;
    tags?.item(currentHiddenTagIndex).setAttribute('style', 'display: none');
    options?.item(currentHiddenTagIndex).removeAttribute('style');
    resizeObserver.unobserve(navigation);
  } else if (distance > 200) {
    tags?.item(currentHiddenTagIndex).removeAttribute('style');
    options?.item(currentHiddenTagIndex).setAttribute('style', 'display: none');
    currentHiddenTagIndex++;
    resizeObserver.unobserve(navigation);
  }
});

function initResizeObserver(): void {
  if (!navigation || !filterSection || !libraryVersion) {
    return;
  }
  resizeObserver.observe(navigation);
}

document.addEventListener('DOMContentLoaded', () => {
  initResizeObserver();
  window.addEventListener('resize', initResizeObserver);
});
