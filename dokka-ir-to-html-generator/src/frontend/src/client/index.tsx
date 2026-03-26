/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import React from 'react';
import { hydrateRoot } from 'react-dom/client';
import { App, PageModel } from '../shared/App';

// Declare types for the global window object so TypeScript doesn't complain
declare global {
    interface Window {
        __INITIAL_DATA__: PageModel;
    }
}

// 1. Read the data that the server (Kotlin) inserts into the HTML
// This ensures the client state matches what arrived in the HTML
const initialData: PageModel = window.__INITIAL_DATA__;

// 2. Use hydrate instead of render
const container = document.getElementById('root');

if (container) {
    // hydrateRoot takes the container as the first argument and the component as the second
    hydrateRoot(container, <App data={initialData} />);
}