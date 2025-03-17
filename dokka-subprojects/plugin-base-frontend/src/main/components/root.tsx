/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import React, { StrictMode } from 'react';
import { render } from 'react-dom';

import App from './app';
import './app/index.scss';

const renderMainSearch = () => {
  render(
    <StrictMode>
      <App />
    </StrictMode>,
    document.getElementById('searchBar')
  );
};

const renderApp = () => {
  renderMainSearch();

  document.removeEventListener('DOMContentLoaded', renderApp);
};

document.addEventListener('DOMContentLoaded', renderApp);
