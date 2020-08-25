import React, { useCallback, useState, useEffect } from 'react';
import {Select, List } from '@jetbrains/ring-ui';
import { DokkaFuzzyFilterComponent } from '../search/dokkaFuzzyFilter';
import { IWindow, Option } from '../search/types';
import './navigationPaneSearch.scss';
import ClearIcon from 'react-svg-loader!./clear.svg';

type NavigationPaneSearchProps = {
    modules?: string[]
}

export const NavigationPaneSearch = ({modules}: NavigationPaneSearchProps) => {
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

    const fetchData = (url: string): Promise<Option[]> => {
        return fetch(url)
            .then(response => response.json())
            .then((result) => {
                    return result.map((record: Option, idx: number) => {
                        return {
                            ...record,
                            key: idx,
                            rgItemType: List.ListProps.Type.CUSTOM
                        }
                    })
                },
                (error) => {
                    console.error('failed to fetch navigationPane data', error)
                    setNavigationList([])
                })
    }

    useEffect(() => {
        const urlFrom = (prefix: string) => prefix.endsWith('/') ? `${prefix}scripts/navigation-pane.json` : `${prefix}/scripts/navigation-pane.json`
        if(modules != null){
            const requests = modules.map((moduleName: string) => {
                const url = `scripts/navigation-pane-${moduleName}.json`
                return fetchData(url).then((value: Option[]) =>
                    value.map((element: Option) => {
                        return {
                            ...element,
                            location: moduleName + '/' + element.location
                        }
                    })
                )
            })
            Promise.all(requests).then((values: Option[][]) => {
                const flattended = values.flat(1).map((value, idx) => {
                    return {
                        ...value,
                        key: idx
                    }
                })
                setNavigationList(flattended)
            })
        } else {
            const pathToRoot = (window as IWindow).pathToRoot
            const url = urlFrom(pathToRoot)
            fetchData(url).then((value: Option[]) => setNavigationList(value))
        }
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
                    renderOptimization={false}
                />
                <span className={"paneSearchInputClearIcon"} onClick={onClearClick}><ClearIcon /></span>
        </div>
}