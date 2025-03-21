/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

function enableScrollToTopButton() {
  const anchorElement = document.getElementById('go-to-top-link');
  const contentElement = document.getElementById('content');
  if (!anchorElement || !contentElement) {
    return;
  }
  anchorElement.addEventListener('click', (event) => {
    event.preventDefault();
    contentElement.scrollIntoView({ behavior: 'smooth' });
  });
}

document.addEventListener('DOMContentLoaded', enableScrollToTopButton);
