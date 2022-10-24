import React from 'react';
import {render} from 'react-dom';

import App from "./app";
import './app/index.scss';


const renderMainSearch = () => {
    render(<App/>, document.getElementById('searchBar'));
}

let renderApp = () => {
    renderMainSearch();

    document.removeEventListener('DOMContentLoaded', renderApp);
};

document.addEventListener('DOMContentLoaded', renderApp);
document.addEventListener('updateContentPage', renderMainSearch);