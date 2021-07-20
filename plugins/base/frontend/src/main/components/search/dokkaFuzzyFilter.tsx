import {Select} from "@jetbrains/ring-ui";
import {Option, OptionWithHighlightComponent, OptionWithSearchResult} from "./types";
import fuzzyHighlight from '@jetbrains/ring-ui/components/global/fuzzy-highlight.js'
import React from "react";
import {SearchResultRow, signatureFromSearchResult} from "./searchResultRow";
import _ from "lodash";

const orderRecords = (records: OptionWithSearchResult[], searchPhrase: string): OptionWithSearchResult[] => {
    return records.sort((a: OptionWithSearchResult, b: OptionWithSearchResult) => {
        //Prefer higher rank
        const byRank = a.rank - b.rank
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

    _showPopup(){
        if(this.props.shouldShowPopup){
            if (!this.node) {
                return;
            }
    
            const shownData = this.getListItems(this.filterValue());
            this.setState({
                showPopup: this.props.shouldShowPopup(this.filterValue()),
                shownData
            })
        } else {
            super._showPopup()
        }
    }
    
    getListItems(rawFilterString: string, e: Option[]) {
        const filterPhrase = (rawFilterString ? rawFilterString : '').trim()
        const matchedRecords = this.props.data
            .map((record: Option) => {
                const searched = record.searchKeys.map((value, index) => {
                    return {
                        ...fuzzyHighlight(filterPhrase, value, false),
                        ...record,
                        rank: index
                    }
                }).filter((e) => e.matched)

                const first = _.head(searched)

                if(first){
                    return first
                }

                return {
                    matched: false,
                    ...record,
                }

            })
            .filter((record: OptionWithSearchResult) => record.matched)

        this.props.onFilter(filterPhrase)

        return highlightMatchedPhrases(orderRecords(matchedRecords, filterPhrase))
    }
}