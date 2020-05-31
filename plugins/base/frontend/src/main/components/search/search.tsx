import React, {useCallback, useState} from 'react';
import {Select} from '@jetbrains/ring-ui';
import '@jetbrains/ring-ui/components/input-size/input-size.scss';
import './search.scss';
import {IWindow, Option, Props} from "./types";

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
                    type={Select.Type.CUSTOM}
                    clear
                    selected={selected}
                    data={data}
                    popupClassName={"popup-wrapper"}
                    onSelect={onChangeSelected}
                    customAnchor={({wrapperProps, buttonProps, popup}) => (
                        <span {...wrapperProps}>
                            <button type="button" {...buttonProps}>
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20">
                                    <path d="M19.64 18.36l-6.24-6.24a7.52 7.52 0 1 0-1.28 1.28l6.24 6.24zM7.5 13.4a5.9 5.9 0 1 1 5.9-5.9 5.91 5.91 0 0 1-5.9 5.9z"/>
                                </svg>
                            </button>
                            {popup}
                        </span>
                    )}
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
