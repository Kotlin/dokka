/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import fuzzyHighlight from '@jetbrains/ring-ui/components/global/fuzzy-highlight.js';
import Select, { SelectProps, SelectState } from '@jetbrains/ring-ui/components/select/select';
import _ from 'lodash';
import React from 'react';
import { SearchResultRow } from './searchResultRow';
import { OptionWithHighlightComponent, OptionWithSearchResult } from './types';

const orderRecords = (records: OptionWithSearchResult[], searchPhrase: string): OptionWithSearchResult[] => {
  return records.sort((a: OptionWithSearchResult, b: OptionWithSearchResult) => {
    //Prefer higher rank
    const byRank = a.rank - b.rank;
    if (byRank !== 0) {
      return byRank;
    }
    //Prefer exact matches
    const aIncludes = a.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0;
    const bIncludes = b.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0;
    const byIncludes = bIncludes - aIncludes;
    if (byIncludes != 0) {
      return byIncludes;
    }

    //Prefer matches that are closer
    const byFirstMatchedPosition = a.highlight.indexOf('**') - b.highlight.indexOf('**');
    if (byFirstMatchedPosition == 0) {
      return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
    }
    return byFirstMatchedPosition;
  });
};

const highlightMatchedPhrases = (records: OptionWithSearchResult[]): OptionWithHighlightComponent[] => {
  return records.map((record) => {
    return {
      ...record,
      template: <SearchResultRow searchResult={record} />,
    } as Omit<OptionWithSearchResult, 'name'> & { name: unknown } as OptionWithHighlightComponent;
  });
};

export class DokkaFuzzyFilterComponent extends Select {
  componentDidUpdate(prevProps: SelectProps, prevState: SelectState) {
    super.componentDidUpdate(prevProps, prevState);
    if (
      this.props.filter &&
      typeof this.props.filter !== 'boolean' &&
      this.state.filterValue != this.props.filter.value
    ) {
      this.setState({
        filterValue: this.props.filter.value!,
      });
    }
  }

  getListItems(rawFilterString: string) {
    const filterPhrase = (rawFilterString ? rawFilterString : '').trim();
    const matchedRecords: Array<Omit<Partial<OptionWithSearchResult>, 'description' | 'label' | 'key'>> =
      this.props.data
        .map((record) => {
          const maybeRecordWithSearchResults = record as OptionWithSearchResult;
          const searched = maybeRecordWithSearchResults.searchKeys
            .map((value, index) => {
              return {
                ...fuzzyHighlight(filterPhrase, value, false),
                ...record,
                rank: index,
              };
            })
            .filter((e) => e.matched);

          const first = _.head(searched);

          if (first) {
            return first;
          }

          return {
            matched: false,
            ...record,
          };
        })
        .filter((record) => record.matched);

    this.props.onFilter(filterPhrase);

    return highlightMatchedPhrases(orderRecords(matchedRecords as OptionWithSearchResult[], filterPhrase));
  }
}
