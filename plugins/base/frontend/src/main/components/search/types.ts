import React from "react";

export type Page = {
    name: string;
    kind: string;
    location: string;
    searchKey: string;
    description: string;
}

export type Option = Page & {
    label: string;
    key: number;
    location: string;
    name: string;
}

export type IWindow = typeof window & {
    pathToRoot: string
    pages: Page[]
}

export type Props = {
    data: Option[]
};

export enum SearchRank {
    SearchKeyMatch = 1,
    NameMatch = 0
}
export type OptionWithSearchResult = Option & {
    matched: boolean,
    highlight: string,
    rank: SearchRank
}

export type OptionWithHighlightComponent = Option & {
    name: React.FC<SearchProps>
}

export type SearchProps = {
    searchResult: OptionWithSearchResult,
}
