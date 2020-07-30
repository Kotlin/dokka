import React from 'react';
import {render} from 'react-dom';
import RedBox from 'redbox-react';

import App from "./app";
import './app/index.scss';
import { NavigationPaneSearch } from './navigationPaneSearch/navigationPaneSearch';

const appEl = document.getElementById('searchBar');
const rootEl = document.createElement('div');

const renderNavigationPane = () => {
  render(
    <NavigationPaneSearch />,
    document.getElementById('paneSearch')
  )
}

let renderApp = () => {
  render(
      <App/>,
      rootEl
  );
  renderNavigationPane();
};

// @ts-ignore
if (module.hot) {
  const renderAppHot = renderApp;
  const renderError = (error: Error) => {
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

  // @ts-ignore
  module.hot.accept('./app', () => {
    setTimeout(renderApp);
  });
}

renderApp();
appEl!.appendChild(rootEl);
