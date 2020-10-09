import React, { useState, useEffect } from "react";
import './pageSummary.scss'
import _ from "lodash";
import Scrollspy from 'react-scrollspy'

type PageSummaryProps = {
    entries: PageSummaryEntry[],
}

type PageSummaryEntry = {
    location: string,
    label: string,
    sourceSets: SourceSetFilterKey[]
}

type SourceSetFilterKey = string

export const PageSummary: React.FC<PageSummaryProps> = ({ entries }: PageSummaryProps) => {
    const [hidden, setHidden] = useState<Boolean>(true);
    const [displayableEntries, setDisplayableEntries] = useState<PageSummaryEntry[]>(entries)

    const handleMouseHover = () => {
        setHidden(!hidden)
    }

    useEffect(() => {
        const handeEvent = (event: CustomEvent<SourceSetFilterKey[]>) => {
            const displayable = entries.filter((entry) => entry.sourceSets.some((sourceset) => event.detail.includes(sourceset)))
            setDisplayableEntries(displayable)
        }

        window.addEventListener('sourceset-filter-change', handeEvent)
        return () => window.removeEventListener('sourceset-filter-change', handeEvent)
    }, [entries])

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
                    {displayableEntries.map((item) => <li><a href={'#' + item.location}>{item.label}</a></li>)}
                </Scrollspy>}
            </div>
        </div>
    )
}