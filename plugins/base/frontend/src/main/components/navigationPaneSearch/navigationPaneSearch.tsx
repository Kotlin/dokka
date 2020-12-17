import React, { useCallback, useState, useEffect } from 'react';
import {Select, List } from '@jetbrains/ring-ui';
import { DokkaFuzzyFilterComponent } from '../search/dokkaFuzzyFilter';
import { IWindow, Option } from '../search/types';
import './navigationPaneSearch.scss';
import ClearIcon from 'react-svg-loader!./clear.svg';
import { relativizeUrlForRequest } from '../utils/requests';

export const NavigationPaneSearch = () => {
    const [navigationList, setNavigationList] = useState<Option[]>([]);
    const [selected, onSelected] = useState<Option | null>(null);
    const [filterValue, setFilterValue] = useState<string>('')
    
    const onChangeSelected = useCallback(
        (element: Option) => {
            window.location.replace(`${(window as IWindow).pathToRoot}${element.location}`)
            onSelected(element);
        },
        [selected]
    );

    const onFilter = (filterValue: string) => {
        setFilterValue(filterValue)
    }

    const onClearClick = () => {
        setFilterValue('')
    }

    const shouldShowPopup = (filterState: string): boolean => {
        return filterState.trim().length !== 0
    }

    useEffect(() => {
        fetch(relativizeUrlForRequest('scripts/navigation-pane.json'))
            .then(response => response.json())
            .then((result) => {
                setNavigationList(result.map((record: Option, idx: number) => {
                    return {
                        ...record,
                        key: idx,
                        rgItemType: List.ListProps.Type.CUSTOM
                    }
                }))
            },
            (error) => {
                console.error('failed to fetch navigationPane data', error)
                setNavigationList([])
            })
    }, [])
  

    return <div className={"paneSearchInputWrapper"}>
            <DokkaFuzzyFilterComponent
                    id="navigation-pane-search"
                    className="navigation-pane-search"
                    inputPlaceholder="Title filter"
                    clear={true}
                    type={Select.Type.INPUT_WITHOUT_CONTROLS}
                    filter={{fuzzy:true, value: filterValue}}
                    selected={selected}
                    data={navigationList}
                    popupClassName={"navigation-pane-popup"}
                    onSelect={onChangeSelected}
                    onFilter={onFilter}
                    shouldShowPopup={shouldShowPopup}
                    renderOptimization={false}
                />
                <span className={"paneSearchInputClearIcon"} onClick={onClearClick}><ClearIcon /></span>
        </div>
}