import React, {useCallback, useState} from 'react';
import {Select} from '@jetbrains/ring-ui';
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import {IWindow, Option, Props, State} from "./types";

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
                <Select
                    selectedLabel="Search"
                    label="Please type page name"
                    filter={{fuzzy: true}}
                    clear
                    selected={selected}
                    data={data}
                    onSelect={onChangeSelected}
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
            label: page.name,
            key: i + 1,
            type: page.kind
        }));
    }

    return <WithFuzzySearchFilterComponent data={data}/>;
};
