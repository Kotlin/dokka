import React from 'react';
import ReactDOMServer from 'react-dom/server';
import { App, PageModel } from '../shared/App';

// this function is called from Kotlin
export function render(jsonData: PageModel): string {
    return ReactDOMServer.renderToString(<App data={jsonData} />);
}