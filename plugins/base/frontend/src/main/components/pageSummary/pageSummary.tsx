import React, { useState, useEffect } from "react";
import './pageSummary.scss'
import _ from "lodash";
import Scrollspy from 'react-scrollspy'

type PageSummaryProps = {
    entries: PageSummaryEntry[],
    containerId: string, //Id of a container that has scroll enabled 
    offsetComponentId: string, //Id of a top navbar component
}

type PageSummaryEntry = {
    location: string,
    label: string,
    sourceSets: SourceSetFilterKey[]
}

type SourceSetFilterKey = string

const getElementHeightFromDom = (elementId: string): number => document.getElementById(elementId).offsetHeight

export const PageSummary: React.FC<PageSummaryProps> = ({ entries, containerId, offsetComponentId }: PageSummaryProps) => {
    const [hidden, setHidden] = useState<Boolean>(true);
    const [displayableEntries, setDisplayableEntries] = useState<PageSummaryEntry[]>(entries)
    const topOffset = getElementHeightFromDom(offsetComponentId)

    useEffect(() => {
        const handeEvent = (event: CustomEvent<SourceSetFilterKey[]>) => {
            const displayable = entries.filter((entry) => entry.sourceSets.some((sourceset) => event.detail.includes(sourceset)))
            setDisplayableEntries(displayable)
        }

        window.addEventListener('sourceset-filter-change', handeEvent)
        return () => window.removeEventListener('sourceset-filter-change', handeEvent)
    }, [entries])

    const handleMouseHover = () => {
        setHidden(!hidden)
    }

    const handleClick = (entry: PageSummaryEntry) => {
        document.getElementById(containerId).scrollTo({
            top: document.getElementById(entry.location).offsetTop - topOffset,
            behavior: 'smooth'
        })
    }

    let classnames = "page-summary"
    if (hidden) classnames += " hidden"

    return (
        <div
            className={classnames}
            onMouseEnter={handleMouseHover}
            onMouseLeave={handleMouseHover}
        >
            <div className={"content-wrapper"}>
                <h4>On this page</h4>
                {!hidden && <Scrollspy items={displayableEntries.map((e) => e.location)} currentClassName="selected">
                    {displayableEntries.map((item) => <li><a onClick={() => handleClick(item)}>{item.label}</a></li>)}
                </Scrollspy>}
            </div>
        </div>
    )
}