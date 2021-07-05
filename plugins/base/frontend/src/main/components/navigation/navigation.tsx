import React, { useState, useCallback, useEffect } from "react";
import { NavigationRecord } from "./types";
import _ from "lodash";

type NavigationRecordProps = {
    record: NavigationRecord;
    currentPageId: string;
    tabsIncludedInNavigation: string[];
    parentCallback: (expanded: boolean) => void
}

type NavigationRowAttributes = {
    "data-active": string;
}

const shouldBeExpandedBecauseMatchesDirectly = (tabFromUrl: string | null, currentPageId: string, record: NavigationRecord, tabsIncludedInNavigation: string[]) => {
    if (tabFromUrl && (currentPageId + '/' + tabFromUrl.toLowerCase()) == record.id) {
        return true
    }
    return (!tabFromUrl || !tabsIncludedInNavigation.includes(tabFromUrl)) && currentPageId == record.id
}

const NavigationRecord: React.FC<NavigationRecordProps> = ({ record, currentPageId, tabsIncludedInNavigation, parentCallback }: NavigationRecordProps) => {
    const [currentTab, setCurrentTab] = useState<string | null>(() => {
        return new URLSearchParams(window.location.search).get('active-tab')
    })
    const [expanded, setExpanded] = useState<boolean>(() => {
        return shouldBeExpandedBecauseMatchesDirectly(currentTab, currentPageId, record, tabsIncludedInNavigation)
    });
    const clickHandler = useCallback(() => setExpanded(!expanded), [expanded]);

    useEffect(() => {
        if (expanded) parentCallback(expanded)
    });

    useEffect(() => {
        window.addEventListener('tab-changed', (evt) => {
            //This event is required to inform react about tab changes, since push state is not available in standard event listeners api
            setCurrentTab(evt.detail)
        })
    })

    const children = record.children.map(e => <NavigationRecord record={e}
                                                                currentPageId={currentPageId}
                                                                parentCallback={setExpanded}
                                                                tabsIncludedInNavigation={tabsIncludedInNavigation}/>)
    let activeAttributes: NavigationRowAttributes | null = null
    if(shouldBeExpandedBecauseMatchesDirectly(currentTab, currentPageId, record, tabsIncludedInNavigation)) {
        activeAttributes = { "data-active": "" }
    }

    return <div className={expanded ? "sideMenuPart" : "sideMenuPart hidden"} id={record.id} {...activeAttributes}>
        <div className="overview">
            <a href={pathToRoot + record.location}>{record.name}</a>
            {record.children.length > 0 &&
            <span className="navButton pull-right" onClick={clickHandler}>
                    <span className="navButtonContent"/>
                </span>
            }
        </div>
        {children}
    </div>
}

export type NavigationProps = {
    records: NavigationRecord[];
    tabsIncludedInNavigation: string[];
    currentPageId: string;
}

export const Navigation: React.FC<NavigationProps> = ({ records, tabsIncludedInNavigation, currentPageId, currentTab }: NavigationProps) => {
    const dummyCallback = (_: boolean) => { }
    return <div>
        {currentTab}
        {records.map(record => <NavigationRecord record={record}
                                                 currentPageId={currentPageId}
                                                 parentCallback={dummyCallback}
                                                 tabsIncludedInNavigation={tabsIncludedInNavigation}/>)}
    </div>
}