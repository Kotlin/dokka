import React from "react";

export type Page = {
    name: string;
    kind: string;
    location: string;
    searchKeys: string[];
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

export type OptionWithSearchResult = Option & {
    matched: boolean,
    highlight: string,
    rank: number
}

export type OptionWithHighlightComponent = Option & {
    name: React.FC<SearchProps>
}

export type SearchProps = {
    searchResult: OptionWithSearchResult,
}
