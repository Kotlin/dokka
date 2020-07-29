import React, {useCallback, useState} from 'react';
import {Select, List} from '@jetbrains/ring-ui';
import fuzzyHighlight from '@jetbrains/ring-ui/components/global/fuzzy-highlight.js'
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import './search.scss';
import {IWindow, Option, Props, Page} from "./types";

enum SearchRank {
    SearchKeyMatch = 1,
    NameMatch = 0
}
type OptionWithSearchResult = Option & {
    matched: boolean,
    highlight: string,
    rank: SearchRank
}

type OptionWithHighlightComponent = Option & {
    name: React.FC<SearchProps>
}

type SearchProps = {
    searchResult: OptionWithSearchResult,
}

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

const SearchResultRow: React.FC<SearchProps> = ({searchResult}: SearchProps) => {
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

const highlightMatchedPhrases = (records: OptionWithSearchResult[]): OptionWithHighlightComponent[] => {
    // @ts-ignore
    return records.map(record => {
        return {
            ...record,
            template: <SearchResultRow searchResult={record}/>
        }
    })
}

class DokkaFuzzyFilterComponent extends Select {
    getListItems(rawFilterString: string, _: Option[]) {
        const matchedRecords = this.props.data
            .map((record: Option) => {
                const bySearchKey = fuzzyHighlight(rawFilterString.trim(), record.searchKey, true)
                if(bySearchKey.matched){
                    return {
                        ...bySearchKey,
                        ...record,
                        rank: SearchRank.SearchKeyMatch
                    }
                }
                return {
                    ...fuzzyHighlight(rawFilterString.trim(), record.name),
                    ...record,
                    rank: SearchRank.NameMatch
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
