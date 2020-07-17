import React from 'react';
import {WithFuzzySearchFilter} from '../search/search';
import './index.scss';

const App: React.FC = () => {
    return <div className="search-content">
        <WithFuzzySearchFilter/>
    </div>
}

export default App
