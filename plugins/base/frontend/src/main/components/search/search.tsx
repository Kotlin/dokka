import React, { useCallback, useEffect, useState } from 'react';
import { Select, List } from '@jetbrains/ring-ui';
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import './search.scss';
import { IWindow, Option, Props } from "./types";
import { DokkaSearchAnchor } from "./dokkaSearchAnchor";
import { DokkaFuzzyFilterComponent } from "./dokkaFuzzyFilter";
import { relativizeUrlForRequest } from '../utils/requests';

const WithFuzzySearchFilterComponent: React.FC<Props> = ({ data }: Props) => {
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
                    disableScrollToActive
                    selected={selected}
                    data={data}
                    popupClassName={"popup-wrapper"}
                    onSelect={onChangeSelected}
                    customAnchor={({ wrapperProps, buttonProps, popup }) =>
                        <DokkaSearchAnchor wrapperProps={wrapperProps} buttonProps={buttonProps} popup={popup} />
                    }
                />
            </div>
        </div>
    )
}

export const WithFuzzySearchFilter = () => {
    const [navigationList, setNavigationList] = useState<Option[]>([]);

    useEffect(() => {
        fetch(relativizeUrlForRequest('scripts/pages.json'))
            .then(response => response.json())
            .then((result) => {
                setNavigationList(result.map((record: Option, idx: number) => {
                    return {
                        ...record,
                        label: record.name,
                        key: idx,
                        type: record.kind,
                        rgItemType: List.ListProps.Type.CUSTOM
                    }
                }))
            },
            (error) => {
                console.error('failed to fetch pages data', error)
                setNavigationList([])
            })
    }, [])

    return <WithFuzzySearchFilterComponent data={navigationList} />;
};
