/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

const COPY_CODE_SELECTOR = 'span.copy-icon';
const COPY_ANCHOR_SELECTOR = 'span.anchor-icon';
const POPUP_TIMEOUT = 1200;

document.addEventListener('click', function (event) {
  if (!(event.target instanceof Element)) {
    return;
  }

  if (event.target.matches(COPY_CODE_SELECTOR)) {
    copyCodeAreaContent(event.target);
  } else if (event.target.matches(COPY_ANCHOR_SELECTOR)) {
    copyLink(event.target);
  }
});

function hrefWithoutAnchor() {
  return window.location.origin + window.location.pathname + window.location.search;
}

function copyCodeAreaContent(element: Element) {
  const sampleContainer = element.closest('.sample-container')?.querySelector('code');

  if (sampleContainer) {
    const text = sampleContainer.textContent || '';

    copyToClipboard(text, element);
  }
}

function copyLink(element: Element) {
  if (!element.hasAttribute('pointing-to')) {
    return;
  }

  const url = hrefWithoutAnchor() + '#' + element.getAttribute('pointing-to');

  copyToClipboard(url, element);
}

function copyToClipboard(text: string, triggerElement: Element) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(text)
      .then(() => showPopup(triggerElement))
      .catch((err) => {
        console.error('Failed to write to clipboard using navigator.clipboard:', err);
        fallbackCopyUsingTextarea(text, triggerElement);
      });
  } else {
    fallbackCopyUsingTextarea(text, triggerElement);
  }
}

function fallbackCopyUsingTextarea(text: string, triggerElement: Element) {
  const textarea = document.createElement('textarea');
  textarea.textContent = text;
  textarea.style.position = 'fixed';
  document.body.appendChild(textarea);
  textarea.select();

  try {
    document.execCommand('copy');
    showPopup(triggerElement);
  } catch (err) {
    console.error('Fallback copy using textarea failed:', err);
  } finally {
    document.body.removeChild(textarea);
  }
}

function showPopup(element: Element) {
  const popupWrapper = element.parentElement?.querySelector('.copy-popup-wrapper');

  if (popupWrapper) {
    popupWrapper.classList.add('active-popup');

    setTimeout(() => {
      popupWrapper.classList.remove('active-popup');
    }, POPUP_TIMEOUT);
  }
}
