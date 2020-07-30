import {Select} from "@jetbrains/ring-ui";
import {Option, OptionWithHighlightComponent, OptionWithSearchResult, SearchRank} from "./types";
import fuzzyHighlight from '@jetbrains/ring-ui/components/global/fuzzy-highlight.js'
import React from "react";
import {SearchResultRow} from "./searchResultRow";

const orderRecords = (records: OptionWithSearchResult[], searchPhrase: string): OptionWithSearchResult[] => {
    return records.sort((a: OptionWithSearchResult, b: OptionWithSearchResult) => {
        //Prefer higher rank
        const byRank = b.rank - a.rank
        if(byRank !== 0){
            return byRank
        }
        //Prefer exact matches
        const aIncludes = a.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0
        const bIncludes = b.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0
        const byIncludes = bIncludes - aIncludes
        if(byIncludes != 0){
            return byIncludes
        }

        //Prefer matches that are closer
        const byFirstMatchedPosition = a.highlight.indexOf("**") - b.highlight.indexOf("**")
        if(byFirstMatchedPosition == 0) {
            return a.name.toLowerCase().localeCompare(b.name.toLowerCase())
        }
        return byFirstMatchedPosition
    })
}

const highlightMatchedPhrases = (records: OptionWithSearchResult[]): OptionWithHighlightComponent[] => {
    // @ts-ignore
    return records.map(record => {
        return {
            ...record,
            template: <SearchResultRow searchResult={record}/>
        }
    })
}

export class DokkaFuzzyFilterComponent extends Select {
    componentDidUpdate(prevProps, prevState) {
        super.componentDidUpdate(prevProps, prevState)
        if(this.props.filter && this.state.filterValue != this.props.filter.value){
            this.setState({
                filterValue: this.props.filter.value,
            })
        }
    }
    
    getListItems(rawFilterString: string, _: Option[]) {
        const filterPhrase = (rawFilterString ? rawFilterString : '').trim()
        const matchedRecords = this.props.data
            .map((record: Option) => {
                const bySearchKey = fuzzyHighlight(filterPhrase, record.searchKey, false)
                if(bySearchKey.matched){
                    return {
                        ...bySearchKey,
                        ...record,
                        rank: SearchRank.SearchKeyMatch
                    }
                }
                return {
                    ...fuzzyHighlight(filterPhrase, record.name, false),
                    ...record,
                    rank: SearchRank.NameMatch
                }
            })
            .filter((record: OptionWithSearchResult) => record.matched)

        this.props.onFilter(filterPhrase, matchedRecords)

        return highlightMatchedPhrases(orderRecords(matchedRecords, filterPhrase))
    }
}