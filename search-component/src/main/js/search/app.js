import React from 'react';
import {render} from 'react-dom';
import RedBox from 'redbox-react';

import AppRoot from './app-root';
import './app.css';

const appEl = document.getElementById('searchBar');
const rootEl = document.createElement('div');

let renderApp = () => {
  render(
    <AppRoot/>,
    rootEl
  );
};

if (module.hot) {
  const renderAppHot = renderApp;
  const renderError = error => {
    render(
      <RedBox error={error}/>,
      rootEl
    );
  };

  renderApp = () => {
    try {
      renderAppHot();
    } catch (error) {
      renderError(error);
    }
  };

  module.hot.accept('./app-root', () => {
    setTimeout(renderApp);
  });
}

renderApp();
appEl.appendChild(rootEl);
