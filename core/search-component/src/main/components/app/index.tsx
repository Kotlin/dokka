import React from 'react';
import {WithFuzzySearchFilter} from '../search/search';
import './index.scss';

const App: React.FC = () => (
    <div className="search-content">
        <WithFuzzySearchFilter/>
    </div>
)

export default App
