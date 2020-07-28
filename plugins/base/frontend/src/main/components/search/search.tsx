import React, {useCallback, useState} from 'react';
import {Select, List} from '@jetbrains/ring-ui';
import fuzzyHighlight from '@jetbrains/ring-ui/components/global/fuzzy-highlight.js'
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import './search.scss';
import {IWindow, Option, Props, Page} from "./types";

type OptionWithSearchResult = Option & {
    matched: boolean,
    highlight: string
}

type OptionWithHighlightComponent = Option & {
    name: React.FC<SearchProps>
}

type SearchProps = {
    page: Option,
    label: string
}

const orderRecords = (records: OptionWithSearchResult[], searchPhrase: string): OptionWithSearchResult[] => {
    return records.sort((a: OptionWithSearchResult, b: OptionWithSearchResult) => {
        //Prefer exact matches
        const aIncludes = a.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0
        const bIncludes = b.name.toLowerCase().includes(searchPhrase.toLowerCase()) ? 1 : 0
        const byIncludes = bIncludes - aIncludes
        if(byIncludes != 0 && aIncludes == 1){
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

const SearchResultRow: React.FC<SearchProps> = ({label, page}: SearchProps) => {
    const withSignature = page.name.replace(page.searchKey, label)

    return (
        <div className="template-wrapper">
            <span dangerouslySetInnerHTML={
                {__html: withSignature.replace(/\*\*(.*?)\*\*/g, '<span class="phraseHighlight">$1</span>') }
            }/>
            <span className="template-description">{page.description}</span>
        </div>
    )
}

const highlightMatchedPhrases = (records: OptionWithSearchResult[]): OptionWithHighlightComponent[] => {
    // @ts-ignore
    return records.map(record => {
        return {
            ...record,
            template: <SearchResultRow label={record.highlight} page={record}/>
        }
    })
}

class DokkaFuzzyFilterComponent extends Select {
    getListItems(rawFilterString: string, _: Option[]) {
        const matchedRecords = this.props.data
            .map((record: Option) => {
                return {
                    ...fuzzyHighlight(rawFilterString.trim(), record.searchKey),
                    ...record
                }
            })
            .filter((record: OptionWithSearchResult) => record.matched)

        return highlightMatchedPhrases(orderRecords(matchedRecords, rawFilterString))
    }
}

const WithFuzzySearchFilterComponent: React.FC<Props> = ({data}: Props) => {
    const [selected, onSelected] = useState<Option>(data[0]);
    const onChangeSelected = useCallback(
        (option: Option) => {
            window.location.replace(`${(window as IWindow).pathToRoot}${option.location}?query=${option.name}`)
            onSelected(option);
        },
        [data]
    );

    return (
        <div className="search-container">
            <div className="search">
                <DokkaFuzzyFilterComponent
                    selectedLabel="Search"
                    label="Please type page name"
                    filter={true}
                    type={Select.Type.CUSTOM}
                    clear
                    renderOptimization
                    selected={selected}
                    data={data}
                    popupClassName={"popup-wrapper"}
                    onSelect={onChangeSelected}
                    customAnchor={({wrapperProps, buttonProps, popup}) =>
                        <DokkaSearchAnchor wrapperProps={wrapperProps} buttonProps={buttonProps} popup={popup}/>
                    }
                />
        </div>
      </div>
    )
}

const DokkaSearchAnchor = ({wrapperProps, buttonProps, popup}) => {
    return (
        <span {...wrapperProps}>
            <button type="button" {...buttonProps}>
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20">
                    <path d="M19.64 18.36l-6.24-6.24a7.52 7.52 0 1 0-1.28 1.28l6.24 6.24zM7.5 13.4a5.9 5.9 0 1 1 5.9-5.9 5.91 5.91 0 0 1-5.9 5.9z"/>
                </svg>
            </button>
            {popup}
        </span>
    )
}

export const WithFuzzySearchFilter = () => {
    let data: Option[] = [];
    const pages = (window as IWindow).pages;
    if (pages) {
        data = pages.map((page, i) => ({
            ...page,
            label: page.searchKey,
            key: i + 1,
            type: page.kind,
            rgItemType: List.ListProps.Type.CUSTOM
        }));
    }

    return <WithFuzzySearchFilterComponent data={data}/>;
};
