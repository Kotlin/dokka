import React from "react";
import {OptionWithSearchResult, SearchProps, SearchRank} from "./types";
import _ from "lodash";

type HighlighterProps = {
    label: string
}

const Highlighter: React.FC<HighlighterProps> = ({label}: HighlighterProps) => {
    return <strong>{label}</strong>
}

export const SearchResultRow: React.FC<SearchProps> = ({searchResult}: SearchProps) => {
    const signatureFromSearchResult = (searchResult: OptionWithSearchResult): string => {
        if(searchResult.rank == SearchRank.SearchKeyMatch){
            return searchResult.name.replace(searchResult.searchKey, searchResult.highlight)
        }
        return searchResult.highlight
    }

    /*
    This is a work-around for an issue: https://youtrack.jetbrains.com/issue/RG-2108
    */
    const out = _.chunk(signatureFromSearchResult(searchResult).split('**'), 2).flatMap(([txt, label]) => [
        txt,
        label ? <Highlighter label={label}></Highlighter> : null,
      ]);

    return (
        <div className="template-wrapper">
            <span>{out}</span>
            <span className="template-description">{searchResult.description}</span>
        </div>
    )
}