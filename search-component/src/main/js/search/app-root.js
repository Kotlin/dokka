import React, {Component} from 'react';
import {WithFuzzySearchFilter} from './search';
import './app.css';

export default class AppRoot extends Component {
  render() {
    return (
        <div className="search-content">
          <WithFuzzySearchFilter/>
        </div>
    );
  }
}