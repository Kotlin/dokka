/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

let instances = [];

const samplesDarkThemeName = 'darcula'
const samplesLightThemeName = 'idea'

// This variable is set by RunnableSamplesInstaller when the user provides a custom server URL
// via the configuration. Leave as null to use the default playground server.
const playgroundServer = null

window.onDarkModeChanged = (darkModeEnabled) => {
    initPlayground(darkModeEnabled ? samplesDarkThemeName : samplesLightThemeName)
}

const initPlayground = (theme) => {
    if (!samplesAreEnabled()) return
    instances.forEach(instance => instance.destroy())
    instances = []

    // Manually tag code fragments as not processed by playground since we also manually destroy all of its instances
    document.querySelectorAll('code.runnablesample').forEach(node => {
        node.removeAttribute("data-kotlin-playground-initialized");

        if (node.parentNode) {
            node.parentNode.setAttribute("runnable-code-sample", "");
        }
    })

    const options = {
        getInstance: playgroundInstance => {
            instances.push(playgroundInstance)
        },
        theme: theme
    };
    if (playgroundServer != null) options.server = playgroundServer;
    KotlinPlayground('code.runnablesample', options);
}

// We check if type is accessible from the current scope to determine if samples script is present
const samplesAreEnabled = () => {
    try {
        if (typeof KotlinPlayground === 'undefined') {
            // KotlinPlayground is exported universally as a global variable or as a module
            // Due to possible interaction with other js scripts KotlinPlayground may not be accessible directly from `window`, so we need an additional check
            KotlinPlayground = exports.KotlinPlayground;
        }
        return typeof KotlinPlayground === 'function';
    } catch (e) {
        return false
    }
}

function refreshPlaygroundSamples() {
    document.querySelectorAll('code.runnablesample').forEach(node => {
        const playground = node.KotlinPlayground;
        /* Some samples may be hidden by filter, they have 0px height  for visible code area
         * after rendering. Call this method for re-calculate code area height */
        playground && playground.view.codemirror.refresh();
    });
}

window.refreshPlaygroundSamples = refreshPlaygroundSamples;
