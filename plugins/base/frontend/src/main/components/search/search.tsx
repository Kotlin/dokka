import React, {useCallback, useState} from 'react';
import {Select, List} from '@jetbrains/ring-ui';
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import './search.scss';
import {IWindow, Option, Props} from "./types";
import {DokkaSearchAnchor} from "./dokkaSearchAnchor";
import {DokkaFuzzyFilterComponent} from "./dokkaFuzzyFilter";

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
                    id="pages-search"
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
