import React from "react";
import {OptionWithSearchResult, SearchProps, SearchRank} from "./types";
import Markdown from '@jetbrains/ring-ui/components/markdown/markdown';

export const SearchResultRow: React.FC<SearchProps> = ({searchResult}: SearchProps) => {
    const signatureFromSearchResult = (searchResult: OptionWithSearchResult): string => {
        if(searchResult.rank == SearchRank.SearchKeyMatch){
            return searchResult.name.replace(searchResult.searchKey, searchResult.highlight)
        }
        return searchResult.highlight
    }

    return (
        <div className="template-wrapper">
            <span><Markdown source={signatureFromSearchResult(searchResult)}/></span>
            <span className="template-description">{searchResult.description}</span>
        </div>
    )
}