import React, { useCallback, useState, useEffect } from 'react';
import {Select, List } from '@jetbrains/ring-ui';
import { DokkaFuzzyFilterComponent } from '../search/dokkaFuzzyFilter';
import { IWindow, Option } from '../search/types';
import './navigationPaneSearch.scss';
import ClearIcon from 'react-svg-loader!./clear.svg';

export const NavigationPaneSearch = () => {
    const defaultWidth = 300

    const [navigationList, setNavigationList] = useState<Option[]>([]);
    const [selected, onSelected] = useState<Option | null>(null);
    const [minWidth, setMinWidth] = useState<number>(defaultWidth);
    const [filterValue, setFilterValue] = useState<string>('')
    
    const onChangeSelected = useCallback(
        (element: Option) => {
            window.location.replace(`${(window as IWindow).pathToRoot}${element.location}`)
            onSelected(element);
        },
        [selected]
    );

    const onFilter = (filterValue: string, filteredRecords?: Option[]) => {
        if(filteredRecords){
            const requiredWidth = Math.max(...filteredRecords.map(e => e.label.length*9), defaultWidth)
            setMinWidth(requiredWidth)
        }
        setFilterValue(filterValue)
    }

    const onClearClick = () => {
        setFilterValue('')
    }

    useEffect(() => {
        const pathToRoot = (window as IWindow).pathToRoot
        const url = pathToRoot.endsWith('/') ? `${pathToRoot}scripts/navigation-pane.json` : `${pathToRoot}/scripts/navigation-pane.json`
        fetch(url)
            .then(response => response.json())
            .then((result) => {
                setNavigationList(result.map((record: Option) => {
                    return {
                        ...record,
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
                    minWidth={minWidth}
                />
                <span className={"paneSearchInputClearIcon"} onClick={onClearClick}><ClearIcon /></span>
        </div>
}