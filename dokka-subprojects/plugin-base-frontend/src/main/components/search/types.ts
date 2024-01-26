/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import React, {ButtonHTMLAttributes, HTMLAttributes, ReactNode, RefCallback} from "react";

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

export interface DataTestProps {
    'data-test'?: string | null | undefined
}

export interface CustomAnchorProps {
    wrapperProps: HTMLAttributes<HTMLElement> & DataTestProps & {ref: RefCallback<HTMLElement>}
    buttonProps: Pick<ButtonHTMLAttributes<HTMLButtonElement>, 'id' | 'disabled' | 'children'> &
        {onClick: () => void} &
        DataTestProps,
    popup: ReactNode
}
