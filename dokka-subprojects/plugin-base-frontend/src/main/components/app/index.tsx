/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import React from 'react';
import {WithFuzzySearchFilter} from '../search/search';
import './index.scss';

const App: React.FC = () => {
    return <div className="search-content">
        <WithFuzzySearchFilter/>
    </div>
}

export default App
