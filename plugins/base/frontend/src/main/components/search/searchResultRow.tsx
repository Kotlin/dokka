import React from "react";
import {OptionWithSearchResult, SearchProps, SearchRank} from "./types";

export const SearchResultRow: React.FC<SearchProps> = ({searchResult}: SearchProps) => {
    const signatureFromSearchResult = (searchResult: OptionWithSearchResult): string => {
        if(searchResult.rank == SearchRank.SearchKeyMatch){
            return searchResult.name.replace(searchResult.searchKey, searchResult.highlight)
        }
        return searchResult.highlight
    }

    const renderHighlightMarkersAsHtml = (record: string): string => {
        return record.replace(/\*\*(.*?)\*\*/g, '<span class="phraseHighlight">$1</span>')
    }

    return (
        <div className="template-wrapper">
            <span dangerouslySetInnerHTML={
                {__html: renderHighlightMarkersAsHtml(signatureFromSearchResult(searchResult)) }
            }/>
            <span className="template-description">{searchResult.description}</span>
        </div>
    )
}